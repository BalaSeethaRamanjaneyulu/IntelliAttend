package com.intelliattend.app.session

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for QR tokens from ActiveSessions
 * 
 * Stores latest token data for instant validation when student scans QR
 * No server call needed - comparison happens locally
 */
@Singleton
class TokenCacheService @Inject constructor() {
    
    companion object {
        private const val TAG = "TokenCacheService"
        private const val TOKEN_VALIDITY_SECONDS = 5
        private const val GRACE_PERIOD_SECONDS = 2
    }
    
    private val _cachedTokenUpdate = MutableStateFlow<TokenUpdate?>(null)
    val cachedTokenUpdate: StateFlow<TokenUpdate?> = _cachedTokenUpdate.asStateFlow()
    
    private var serverTimeOffset: Long = 0  // Difference between server and local time
    
    /**
     * Update cached token data when Firestore listener fires
     * 
     * @param tokenUpdate New token data from ActiveSessions
     */
    fun updateCache(tokenUpdate: TokenUpdate) {
        _cachedTokenUpdate.value = tokenUpdate
        
        // Calculate server time offset for clock sync
        // ServerTime from Firestore vs local time
        val serverTime = tokenUpdate.currentTimestamp
        val localTime = System.currentTimeMillis() / 1000
        serverTimeOffset = serverTime - localTime
        
        Log.d(TAG, "Token cached - Seq: ${tokenUpdate.sequence}, Offset: ${serverTimeOffset}s")
    }
    
    /**
     * Validate scanned QR token against cached tokens
     * 
     * Checks:
     * 1. Token format and signature (via TokenValidator)
     * 2. Timestamp within validity window (5s + 2s grace)
     * 3. Matches current OR previous token (for rotation overlap)
     * 
     * @param scannedToken QR code string from camera
     * @return Pair of (isValid, errorMessage)
     */
    fun validateScannedToken(scannedToken: String): Pair<Boolean, String?> {
        val cached = _cachedTokenUpdate.value
        
        if (cached == null) {
            Log.w(TAG, "No cached token available")
            return Pair(false, "Token cache not initialized. Please wait...")
        }
        
        if (cached.status != "active") {
            return Pair(false, "Session is not active")
        }
        
        // Quick match: Check if scanned token equals current or previous
        val matchesCurrent = scannedToken == cached.currentToken
        val matchesPrevious = cached.previousToken != null && scannedToken == cached.previousToken
        
        if (matchesCurrent || matchesPrevious) {
            Log.d(TAG, "✅ Token matched: ${if (matchesCurrent) "current" else "previous"}")
            
            // Additional timestamp validation
            val tokenTimestamp = if (matchesCurrent) cached.currentTimestamp else cached.previousTimestamp ?: 0
            val isTimestampValid = validateTimestamp(tokenTimestamp)
            
            return if (isTimestampValid.first) {
                Pair(true, null)
            } else {
                Pair(false, isTimestampValid.second)
            }
        }
        
        Log.w(TAG, "❌ Token mismatch")
        return Pair(false, "QR code does not match current session")
    }
    
    /**
     * Validate timestamp is within allowed window
     * 
     * @param tokenTimestamp Unix timestamp from token
     * @return Pair of (isValid, errorMessage)
     */
    private fun validateTimestamp(tokenTimestamp: Long): Pair<Boolean, String?> {
        // Adjust local time using server offset
        val adjustedCurrentTime = (System.currentTimeMillis() / 1000) + serverTimeOffset
        val ageSeconds = adjustedCurrentTime - tokenTimestamp
        
        val maxAge = TOKEN_VALIDITY_SECONDS + GRACE_PERIOD_SECONDS
        
        return when {
            ageSeconds < 0 -> {
                Log.w(TAG, "Token from future: ${-ageSeconds}s (clock skew)")
                Pair(false, "Clock synchronization error. Please check device time.")
            }
            ageSeconds > maxAge -> {
                Log.w(TAG, "Token expired: ${ageSeconds}s old (max ${maxAge}s)")
                Pair(false, "QR code expired. Please scan the latest code.")
            }
            else -> {
                Log.d(TAG, "Timestamp valid: ${ageSeconds}s old")
                Pair(true, null)
            }
        }
    }
    
    /**
     * Get current cached token (if available)
     * 
     * @return Current token string or null
     */
    fun getCurrentToken(): String? {
        return _cachedTokenUpdate.value?.currentToken
    }
    
    /**
     * Clear cache (e.g., when leaving scanner screen)
     */
    fun clearCache() {
        _cachedTokenUpdate.value = null
        serverTimeOffset = 0
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * Get session ID from cached data
     * 
     * @return Session ID or null
     */
    fun getSessionId(): String? {
        return _cachedTokenUpdate.value?.sessionId
    }
}
