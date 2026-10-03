package com.weatherengine.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.api.FailureKind
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val showMockModeOption: Boolean = false,
    val isMockMode: Boolean = false
)

class AuthViewModel(
    private val repository: WeatherEngineRepository,
    private val settingsStore: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigateToNextScreen = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToNextScreen: SharedFlow<Unit> = _navigateToNextScreen.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsStore.isMockModeFlow.collect { mock ->
                _uiState.update { it.copy(isMockMode = mock) }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun toggleAuthMode() {
        setAuthMode(!_uiState.value.isSignUp)
    }

    fun setAuthMode(isSignUp: Boolean) {
        _uiState.update {
            if (it.isSignUp == isSignUp) return@update it
            it.copy(
                isSignUp = isSignUp,
                emailError = null,
                passwordError = null,
                generalError = null,
                showMockModeOption = false
            )
        }
    }

    fun skipForNow() {
        _navigateToNextScreen.tryEmit(Unit)
    }

    fun enableMockMode() {
        viewModelScope.launch {
            settingsStore.setMockMode(true)
            _uiState.update { it.copy(showMockModeOption = false, generalError = null) }
        }
    }

    fun submit() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val emailValid = Validators.isValidEmail(currentState.email)
        val passwordValid = Validators.isValidPassword(currentState.password)

        if (!emailValid || !passwordValid) {
            _uiState.update {
                it.copy(
                    emailError = if (!emailValid) "Enter a valid email address" else null,
                    passwordError = if (!passwordValid) "Password must be at least 8 characters" else null
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null, showMockModeOption = false) }

        viewModelScope.launch(ioDispatcher) {
            val result = if (currentState.isSignUp) {
                repository.signup(currentState.email.trim(), currentState.password)
            } else {
                repository.login(currentState.email.trim(), currentState.password)
            }

            when (result) {
                is NetworkResult.Success -> {
                    settingsStore.saveAuth(result.data.token, result.data.email)
                    try {
                        val personaRes = repository.getPersonas()
                        if (personaRes is NetworkResult.Success && personaRes.data.isNotEmpty()) {
                            settingsStore.saveSelectedPersonas(
                                personaRes.data.map { it.code }.toSet()
                            )
                        }
                    } catch (_: Throwable) {
                        // Safe fallback if persona sync fails or is unstubbed in unit tests
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToNextScreen.tryEmit(Unit)
                }
                is NetworkResult.Failure -> {
                    val showMock = result.kind == FailureKind.Network || result.kind == FailureKind.Timeout
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message,
                            showMockModeOption = showMock
                        )
                    }
                }
            }
        }
    }
}
