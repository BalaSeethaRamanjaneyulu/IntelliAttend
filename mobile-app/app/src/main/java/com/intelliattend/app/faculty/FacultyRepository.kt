package com.intelliattend.app.faculty

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacultyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getLiveAttendeeCount(sessionId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("attendance")
            .whereEqualTo("session_id", sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val count = snapshot?.size() ?: 0
                trySend(count)
            }
        awaitClose { listener.remove() }
    }
}
