package com.mateus.microredesocial.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mateus.microredesocial.dao.UserDao
import com.mateus.microredesocial.databinding.ItemPostBinding
import com.mateus.microredesocial.model.Post
import com.mateus.microredesocial.model.User
import com.mateus.microredesocial.utils.Base64Converter
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val currentUid: String,
    private val onEdit: (Post) -> Unit,
    private val onDelete: (Post) -> Unit,
    private val onComment: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val allPosts: MutableList<Post> = mutableListOf()
    private val posts: MutableList<Post> = mutableListOf()
    private val converter = Base64Converter()
    private val userDao = UserDao()
    private val userCache: MutableMap<String, User> = mutableMapOf()

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        with(holder.binding) {
            txtContent.text = post.description
            txtTimestamp.text = formatTime(post.timestamp)

            if (!post.image.isNullOrEmpty()) {
                imgPost.setImageBitmap(converter.stringToBitmap(post.image))
                imgPost.visibility = View.VISIBLE
            } else {
                imgPost.visibility = View.GONE
            }

            if (!post.locationName.isNullOrEmpty()) {
                chipLocation.text = post.locationName
                chipLocation.visibility = View.VISIBLE
            } else {
                chipLocation.visibility = View.GONE
            }

            val isOwner = post.userId == currentUid
            btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
            btnEdit.setOnClickListener { onEdit(post) }
            btnDelete.setOnClickListener { onDelete(post) }
            btnComment.setOnClickListener { onComment(post) }

            if (post.commentCount > 0) {
                txtCommentCount.visibility = View.VISIBLE
                txtCommentCount.text = post.commentCount.toString()
                txtCommentSeparator.visibility = View.VISIBLE
            } else {
                txtCommentCount.visibility = View.GONE
                txtCommentSeparator.visibility = View.GONE
            }

            val cached = userCache[post.userId]
            if (cached != null) {
                applyUser(holder.binding, cached)
            } else {
                txtName.text = "Carregando..."
                txtUsername.text = ""
                imgAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                userDao.getUser(post.userId) { user ->
                    user ?: return@getUser
                    userCache[post.userId] = user
                    val idx = posts.indexOfFirst { it.id == post.id }
                    if (idx >= 0) notifyItemChanged(idx)
                }
            }
        }
    }

    private fun applyUser(binding: ItemPostBinding, user: User) {
        binding.txtName.text = user.name
        binding.txtUsername.text = "@${user.username}"
        if (!user.photo.isNullOrEmpty()) {
            binding.imgAvatar.setImageBitmap(converter.stringToBitmap(user.photo))
        } else {
            binding.imgAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        allPosts.clear(); allPosts.addAll(newPosts)
        posts.clear(); posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun appendPosts(newPosts: List<Post>) {
        allPosts.addAll(newPosts)
        val start = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(start, newPosts.size)
    }

    fun filterPosts(query: String) {
        posts.clear()
        if (query.isBlank()) {
            posts.addAll(allPosts)
        } else {
            val q = query.trim().normalize()
            posts.addAll(allPosts.filter { post ->
                val matchesContent  = post.description.normalize().contains(q)
                val matchesCity     = post.locationName?.normalize()?.contains(q) == true
                val cachedUser      = userCache[post.userId]
                val matchesName     = cachedUser?.name?.normalize()?.contains(q) == true
                val matchesUsername = cachedUser?.username?.normalize()?.contains(q) == true
                matchesContent || matchesCity || matchesName || matchesUsername
            })
        }
        notifyDataSetChanged()
    }

    fun removePost(post: Post) {
        allPosts.removeAll { it.id == post.id }
        val index = posts.indexOfFirst { it.id == post.id }
        if (index >= 0) { posts.removeAt(index); notifyItemRemoved(index) }
    }

    fun updatePost(post: Post) {
        val allIdx = allPosts.indexOfFirst { it.id == post.id }
        if (allIdx >= 0) allPosts[allIdx] = post
        val index = posts.indexOfFirst { it.id == post.id }
        if (index >= 0) { posts[index] = post; notifyItemChanged(index) }
    }

    fun updateCommentCount(postId: String, delta: Int) {
        val allIdx = allPosts.indexOfFirst { it.id == postId }
        if (allIdx >= 0) allPosts[allIdx] = allPosts[allIdx].copy(
            commentCount = (allPosts[allIdx].commentCount + delta).coerceAtLeast(0)
        )
        val idx = posts.indexOfFirst { it.id == postId }
        if (idx >= 0) {
            posts[idx] = posts[idx].copy(commentCount = (posts[idx].commentCount + delta).coerceAtLeast(0))
            notifyItemChanged(idx)
        }
    }

    private fun String.normalize(): String {
        val nfd = Normalizer.normalize(this, Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase()
    }

    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000L         -> "agora"
            diff < 3_600_000L      -> "${diff / 60_000}min"
            diff < 86_400_000L     -> "${diff / 3_600_000}h"
            diff < 7 * 86_400_000L -> "${diff / 86_400_000}d"
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}