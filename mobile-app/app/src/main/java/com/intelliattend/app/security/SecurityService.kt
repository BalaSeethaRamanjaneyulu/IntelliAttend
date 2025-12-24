package com.intelliattend.app.security

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val integrityManager = IntegrityManagerFactory.create(context)

    suspend fun getIntegrityToken(nonce: String): String? {
        return try {
            val request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(GOOGLE_CLOUD_PROJECT_NUMBER) // Replace with actual project number
                .setNonce(nonce)
                .build()

            val response = integrityManager.requestIntegrityToken(request).await()
            response.token()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val GOOGLE_CLOUD_PROJECT_NUMBER = 1234567890L // Placeholder
    }
}
