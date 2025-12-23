package com.intelliattend.faculty.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.faculty.data.model.LiveStatusResponse
import com.intelliattend.faculty.data.repository.SessionRepository
import com.intelliattend.faculty.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _liveStatusState = MutableStateFlow<Resource<LiveStatusResponse>?>(null)
    val liveStatusState: StateFlow<Resource<LiveStatusResponse>?> = _liveStatusState.asStateFlow()
    
    private var pollingJob: Job? = null
    
    /**
     * Start polling for live status updates every 5 seconds
     */
    fun startPolling(sessionId: String) {
        pollingJob?.cancel()
        
        pollingJob = viewModelScope.launch {
            while (true) {
                sessionRepository.getLiveStatus(sessionId).collect { resource ->
                    _liveStatusState.value = resource
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }
    
    /**
     * Stop polling
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    /**
     * Manual refresh
     */
    fun refresh(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.getLiveStatus(sessionId).collect { resource ->
                _liveStatusState.value = resource
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
