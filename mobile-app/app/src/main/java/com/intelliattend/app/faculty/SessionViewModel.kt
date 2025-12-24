package com.intelliattend.app.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: FacultyRepository
) : ViewModel() {

    private val _otpValue = MutableStateFlow<String>("")
    val otpValue = _otpValue.asStateFlow()

    private val _timerValue = MutableStateFlow(60)
    val timerValue = _timerValue.asStateFlow()

    private val _attendeeCount = MutableStateFlow(0)
    val attendeeCount = _attendeeCount.asStateFlow()

    private var sessionId: String? = null

    fun startSession(id: String) {
        sessionId = id
        generateOtp()
        listenForAttendees(id)
    }

    private fun listenForAttendees(id: String) {
        repository.getLiveAttendeeCount(id)
            .onEach { count -> _attendeeCount.value = count }
            .launchIn(viewModelScope)
    }

    private fun generateOtp() {
        viewModelScope.launch {
            while (true) {
                // Generate a random 6-digit OTP
                val otp = (100000..999999).random().toString()
                _otpValue.value = otp
                
                for (i in 60 downTo 1) {
                    _timerValue.value = i
                    delay(1000)
                }
            }
        }
    }
}
