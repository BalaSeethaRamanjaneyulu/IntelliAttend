package com.intelliattend.app.session

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing token update from ActiveSessions
 */
data class TokenUpdate(
    val currentToken: String,
    val previousToken: String?,
    val currentTimestamp: Long,
    val previousTimestamp: Long?,
    val currentExpiry: Long,
    val previousExpiry: Long?,
    val serverTime: Any?,  // Firestore SERVER_TIMESTAMP
    val sequence: Int,
    val sessionId: String,
    val status: String
)

/**
 * Repository for managing Firestore ActiveSessions real-time listener
 * 
 * This enables instant (<200ms) QR token synchronization from Firestore
 * instead of polling or WebSocket
 */
@Singleton
class ActiveSessionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val COLLECTION_NAME = "ActiveSessions"
        private const val TAG = "ActiveSessionRepo"
    }
    
    /**
     * Listen to ActiveSession token updates in real-time
     * 
     * Returns a Flow that emits TokenUpdate whenever the Firestore document changes
     * Automatically handles connection loss and recovery
     * 
     * @param sessionId Session to listen to
     * @return Flow of TokenUpdate events
     */
    fun listenToActiveSession(sessionId: String): Flow<Result<TokenUpdate>> = callbackFlow {
        android.util.Log.d(TAG, "Starting listener for session: $sessionId")
        
        val listenerRegistration: ListenerRegistration = firestore
            .collection(COLLECTION_NAME)
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                
                if (error != null) {
                    android.util.Log.e(TAG, "Listener error: ${error.message}", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    android.util.Log.w(TAG, "ActiveSession not found: $sessionId")
                    trySend(Result.failure(Exception("ActiveSession not found")))
                    return@addSnapshotListener
                }
                
                try {
                    val data = snapshot.data ?: run {
                        android.util.Log.w(TAG, "Empty ActiveSession data")
                        trySend(Result.failure(Exception("Empty session data")))
                        return@addSnapshotListener
                    }
                    
                    val tokenUpdate = TokenUpdate(
                        currentToken = data["currentToken"] as? String 
                            ?: error("Missing currentToken"),
                        previousToken = data["previousToken"] as? String,
                        currentTimestamp = (data["currentTimestamp"] as? Long) 
                            ?: error("Missing currentTimestamp"),
                        previousTimestamp = data["previousTimestamp"] as? Long,
                        currentExpiry = (data["currentExpiry"] as? Long) 
                            ?: error("Missing currentExpiry"),
                        previousExpiry = data["previousExpiry"] as? Long,
                        serverTime = data["serverTime"],
                        sequence = (data["sequence"] as? Long)?.toInt() ?: 0,
                        sessionId = data["sessionId"] as? String ?: sessionId,
                        status = data["status"] as? String ?: "unknown"
                    )
                    
                    android.util.Log.d(TAG, "Token updated - Seq: ${tokenUpdate.sequence}")
                    trySend(Result.success(tokenUpdate))
                    
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Parse error: ${e.message}", e)
                    trySend(Result.failure(e))
                }
            }
        
        // Cleanup listener when Flow is cancelled
        awaitClose {
            android.util.Log.d(TAG, "Removing listener for session: $sessionId")
            listenerRegistration.remove()
        }
    }
    
    /**
     * Get current ActiveSession data (one-time fetch, no listener)
     * 
     * @param sessionId Session to fetch
     * @return TokenUpdate or null if not found
     */
    suspend fun getActiveSession(sessionId: String): TokenUpdate? {
        return try {
            val snapshot = firestore
                .collection(COLLECTION_NAME)
                .document(sessionId)
                .get()
                .await()
            
            if (!snapshot.exists()) {
                android.util.Log.w(TAG, "ActiveSession not found: $sessionId")
                return null
            }
            
            val data = snapshot.data ?: return null
            
            TokenUpdate(
                currentToken = data["currentToken"] as? String ?: return null,
                previousToken = data["previousToken"] as? String,
                currentTimestamp = (data["currentTimestamp"] as? Long) ?: return null,
                previousTimestamp = data["previousTimestamp"] as? Long,
                currentExpiry = (data["currentExpiry"] as? Long) ?: return null,
                previousExpiry = data["previousExpiry"] as? Long,
                serverTime = data["serverTime"],
                sequence = (data["sequence"] as? Long)?.toInt() ?: 0,
                sessionId = data["sessionId"] as? String ?: sessionId,
                status = data["status"] as? String ?: "unknown"
            )
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching ActiveSession: ${e.message}", e)
            null
        }
    }
}
