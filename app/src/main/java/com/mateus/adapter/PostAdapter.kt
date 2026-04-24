package com.mateus.microredesocial.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mateus.microredesocial.databinding.ItemPostBinding
import com.mateus.microredesocial.model.Post
import com.mateus.microredesocial.utils.Base64Converter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val posts: MutableList<Post>,
    private val onPostClick: (Post) -> Unit = {}
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        with(holder.binding) {
            txtDescricao.text = post.descricao
            txtAutorNome.text = post.autorNome
            txtCidade.text = if (post.cidade.isNotEmpty()) "📍 ${post.cidade}" else ""

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            txtTimestamp.text = sdf.format(Date(post.timestamp))

            val bitmap = Base64Converter.stringToBitmap(post.imageString)
            if (bitmap != null) {
                imgPost.setImageBitmap(bitmap)
            }

            val autorBitmap = Base64Converter.stringToBitmap(post.autorFoto)
            if (autorBitmap != null) {
                imgAutorFoto.setImageBitmap(autorBitmap)
            }

            root.setOnClickListener { onPostClick(post) }
        }
    }

    override fun getItemCount(): Int = posts.size

    fun addPosts(newPosts: List<Post>) {
        val startPos = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(startPos, newPosts.size)
    }

    fun setPosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
