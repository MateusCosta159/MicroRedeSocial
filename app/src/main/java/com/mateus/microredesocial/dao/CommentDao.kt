package com.mateus.microredesocial.dao

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mateus.microredesocial.model.Comment

class CommentDao {

    private val db = FirebaseFirestore.getInstance()

    fun addComment(postId: String, comment: Comment, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val postRef = db.collection("posts").document(postId)
        val commentsRef = postRef.collection("comments")
        val docRef = commentsRef.document()
        val commentFinal = comment.copy(id = docRef.id, postId = postId)

        db.runTransaction { tx ->
            tx.set(docRef, commentFinal)
            tx.update(postRef, "commentCount", FieldValue.increment(1))
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao comentar") }
    }

    fun deleteComment(postId: String, commentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        db.runTransaction { tx ->
            tx.delete(commentRef)
            tx.update(postRef, "commentCount", FieldValue.increment(-1))
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir comentário") }
    }

    fun getComments(postId: String, onResult: (List<Comment>) -> Unit) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { result ->
                onResult(result.mapNotNull { it.toObject(Comment::class.java) })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}