package com.intelliattend.app.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelliattend.app.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val studentId = authRepository.getCurrentUser()?.uid
        if (studentId == null) {
            _uiState.value = DashboardUiState.Error("User not logged in")
            return
        }

        repository.getAttendanceHistory(studentId)
            .onEach { records ->
                _uiState.value = DashboardUiState.Success(records)
            }
            .catch { e ->
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to load history")
            }
            .launchIn(viewModelScope)
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val history: List<AttendanceRecord>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
