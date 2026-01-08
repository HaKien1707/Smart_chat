package com.example.smart_chat.utils.firebase

import com.example.smart_chat.models.userModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAuthentication {
    @JvmStatic
    fun currentUserID(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    @JvmStatic
    fun currentUserDetails(): DocumentReference {
        return FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserID()!!)
    }

    @JvmStatic
    fun allUsersCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("users")
    }

    @JvmStatic
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }

    @JvmStatic
    val isLoggedIn: Boolean
        get() = currentUserID() != null

    @JvmStatic
    fun getBlockedUsers(onComplete: (List<userModel>) -> Unit) {
        val currentUserID = currentUserID() ?: return

        currentUserDetails().get().addOnSuccessListener {
            val user = it.toObject(userModel::class.java)
            val blockedIDs = user?.blockedUsers ?: emptyList<String>()

            if (blockedIDs.isEmpty()) {
                onComplete(emptyList())
                return@addOnSuccessListener
            }

            allUsersCollection().whereIn("userID", blockedIDs).get()
                .addOnSuccessListener { documents ->
                    val blockedUsers = documents.toObjects(userModel::class.java)
                    onComplete(blockedUsers)
                }
                .addOnFailureListener {
                    onComplete(emptyList())
                }
        }
    }

    @JvmStatic
    fun unblockUser(userID: String, onComplete: () -> Unit) {
        currentUserDetails().update("blockedUsers", FieldValue.arrayRemove(userID))
            .addOnSuccessListener { onComplete() }
    }
}