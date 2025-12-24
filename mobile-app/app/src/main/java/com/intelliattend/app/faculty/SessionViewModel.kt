package com.intelliattend.app.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class SessionViewModel @Inject constructor() : ViewModel() {

    private val _qrContent = MutableStateFlow<String>("")
    val qrContent = _qrContent.asStateFlow()

    private val _timerValue = MutableStateFlow(30)
    val timerValue = _timerValue.asStateFlow()

    private var sessionId: String? = null

    fun startSession(id: String) {
        sessionId = id
        generateDynamicQR()
    }

    private fun generateDynamicQR() {
        viewModelScope.launch {
            while (true) {
                val timestamp = System.currentTimeMillis()
                val nonce = UUID.randomUUID().toString()
                // In a real app, this would be a signed JWT or similar
                _qrContent.value = "session=$sessionId&ts=$timestamp&nonce=$nonce"
                
                for (i in 30 downTo 1) {
                    _timerValue.value = i
                    delay(1000)
                }
            }
        }
    }
}
