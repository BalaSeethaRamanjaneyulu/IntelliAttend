package com.intelliattend.app.student

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAttendanceHistory(studentId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val listener = firestore.collection("attendance")
            .whereEqualTo("student_id", studentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }
}

data class AttendanceRecord(
    val session_id: String = "",
    val course_name: String = "",
    val timestamp: Long = 0,
    val status: String = "Present"
)
