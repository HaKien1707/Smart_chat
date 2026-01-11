package com.example.smart_chat.utils.firebase

import com.example.smart_chat.models.community.CommunityModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseCommunity {
    @JvmStatic
    fun allCommunitiesCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("communities")
    }

    @JvmStatic
    fun getCommunityReference(communityID: String): DocumentReference {
        return allCommunitiesCollection().document(communityID)
    }

    @JvmStatic
    fun getCommunityMessagesReference(communityID: String): CollectionReference {
        return getCommunityReference(communityID).collection("messages")
    }

    @JvmStatic
    fun createCommunity(
        communityName: String,
        communityDescription: String,
        communityImage: String?,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val communityID = allCommunitiesCollection().document().id

        val community = CommunityModel(
            communityID,
            communityName,
            communityDescription,
            communityImage,
            currentUserID,
            Timestamp.now()
        )

        // Backfill new fields for search/permissions.
        community.ownerID = currentUserID
        community.adminIDs = mutableListOf()
        community.communityType = "public"

        getCommunityReference(communityID).set(community)
            .addOnSuccessListener { onSuccess(communityID) }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun banUserFromCommunity(
        communityID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getCommunityReference(communityID)
            .update("bannedUserIDs", FieldValue.arrayUnion(userID))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun unbanUserFromCommunity(
        communityID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getCommunityReference(communityID)
            .update("bannedUserIDs", FieldValue.arrayRemove(userID))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun isBannedFromCommunity(
        communityID: String,
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        getCommunityReference(communityID).get()
            .addOnSuccessListener { document ->
                val community = document.toObject(CommunityModel::class.java)
                val isBanned = community?.bannedUserIDs?.contains(userID) == true
                onResult(isBanned)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}