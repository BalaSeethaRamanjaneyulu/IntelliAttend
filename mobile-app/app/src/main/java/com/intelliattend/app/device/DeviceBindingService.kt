package com.intelliattend.app.device

import android.content.Context
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceBindingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getFirebaseInstallationId(): String? {
        return try {
            FirebaseInstallations.getInstance().id.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
