package com.mateus.microredesocial.dao

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mateus.microredesocial.model.Post

class PostDao {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("posts")

    companion object {
        const val PAGE_SIZE = 5L
    }

    fun addPost(post: Post, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val docRef = collection.document()
        val postWithId = post.copy(id = docRef.id)
        docRef.set(postWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao criar relato") }
    }

    fun updatePost(post: Post, onSuccess: () -> Unit, onError: (String) -> Unit) {
        collection.document(post.id).set(post)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar relato") }
    }

    fun deletePost(postId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        collection.document(postId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir relato") }
    }

    fun getFirstPage(onResult: (List<Post>, DocumentSnapshot?) -> Unit) {
        collection.orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGE_SIZE).get()
            .addOnSuccessListener { result ->
                val posts = result.mapNotNull { it.toObject(Post::class.java) }
                onResult(posts, result.documents.lastOrNull())
            }
            .addOnFailureListener { e ->
                Log.e("PostDao", "Erro: ${e.message}")
                onResult(emptyList(), null)
            }
    }

    fun getNextPage(lastDoc: DocumentSnapshot, onResult: (List<Post>, DocumentSnapshot?) -> Unit) {
        collection.orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastDoc).limit(PAGE_SIZE).get()
            .addOnSuccessListener { result ->
                val posts = result.mapNotNull { it.toObject(Post::class.java) }
                onResult(posts, result.documents.lastOrNull())
            }
            .addOnFailureListener { e ->
                Log.e("PostDao", "Erro: ${e.message}")
                onResult(emptyList(), null)
            }
    }
}