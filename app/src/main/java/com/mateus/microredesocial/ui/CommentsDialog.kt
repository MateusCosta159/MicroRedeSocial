package com.mateus.microredesocial.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mateus.microredesocial.R
import com.mateus.microredesocial.adapter.CommentAdapter
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.CommentDao
import com.mateus.microredesocial.dao.UserDao
import com.mateus.microredesocial.databinding.DialogCommentsBinding
import com.mateus.microredesocial.model.Comment
import com.mateus.microredesocial.utils.Base64Converter

class CommentsDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCommentsBinding? = null
    private val binding get() = _binding!!

    var postId: String = ""
    var onCommentCountChanged: ((delta: Int) -> Unit)? = null

    private val auth = UserAuth()
    private val commentDao = CommentDao()
    private val userDao = UserDao()
    private val converter = Base64Converter()
    private lateinit var adapter: CommentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCommentsBinding.inflate(inflater, container, false); return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentUid = auth.getCurrentUid() ?: ""
        adapter = CommentAdapter(currentUid = currentUid, onDelete = { comment -> confirmDeleteComment(comment) })

        binding.recyclerComments.apply { this.adapter = this@CommentsDialog.adapter; layoutManager = LinearLayoutManager(requireContext()) }

        loadComments(); loadCurrentUserAvatar(currentUid)
        binding.btnCloseComments.setOnClickListener { dismiss() }
        binding.btnSendComment.setOnClickListener { sendComment() }
        binding.edtComment.setOnEditorActionListener { _, _, _ -> sendComment(); true }
    }

    override fun onStart() { super.onStart(); dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private fun loadComments() {
        if (postId.isEmpty()) return; showLoading(true)
        commentDao.getComments(postId) { comments ->
            if (_binding == null) return@getComments
            showLoading(false); adapter.setComments(comments); updateHeader(comments.size)
            binding.txtNoComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerComments.visibility = if (comments.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun sendComment() {
        val text = binding.edtComment.text.toString().trim()
        if (text.isEmpty()) return
        val uid = auth.getCurrentUid() ?: return
        binding.btnSendComment.isEnabled = false
        val comment = Comment(postId = postId, userId = uid, text = text, timestamp = System.currentTimeMillis())
        commentDao.addComment(postId, comment,
            onSuccess = {
                if (_binding == null) return@addComment
                binding.btnSendComment.isEnabled = true; binding.edtComment.setText("")
                adapter.addComment(comment); binding.recyclerComments.scrollToPosition(adapter.itemCount - 1)
                binding.txtNoComments.visibility = View.GONE; binding.recyclerComments.visibility = View.VISIBLE
                updateHeader(adapter.itemCount); onCommentCountChanged?.invoke(+1)
            },
            onError = { msg -> if (_binding == null) return@addComment; binding.btnSendComment.isEnabled = true; Toast.makeText(context, "Erro: $msg", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun confirmDeleteComment(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_comment)).setMessage(getString(R.string.delete_comment_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteComment(comment) }
            .setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun deleteComment(comment: Comment) {
        commentDao.deleteComment(postId, comment.id,
            onSuccess = {
                if (_binding == null) return@deleteComment
                adapter.removeComment(comment); updateHeader(adapter.itemCount); onCommentCountChanged?.invoke(-1)
                if (adapter.itemCount == 0) { binding.txtNoComments.visibility = View.VISIBLE; binding.recyclerComments.visibility = View.GONE }
            },
            onError = { msg -> if (_binding == null) return@deleteComment; Toast.makeText(context, "Erro: $msg", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun loadCurrentUserAvatar(uid: String) {
        userDao.getUser(uid) { user ->
            if (_binding == null || user?.photo.isNullOrEmpty()) return@getUser
            binding.imgCurrentUserAvatar.setImageBitmap(converter.stringToBitmap(user!!.photo!!))
        }
    }

    private fun updateHeader(count: Int) {
        binding.txtCommentCount.text = if (count == 0) getString(R.string.comments) else getString(R.string.comments_count, count)
    }

    private fun showLoading(loading: Boolean) {
        binding.progressComments.visibility = if (loading) View.VISIBLE else View.GONE
    }
}