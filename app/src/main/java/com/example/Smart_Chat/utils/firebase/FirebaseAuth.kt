package com.example.Smart_Chat.utils.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAuth {
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
}