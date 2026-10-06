package com.weatherengine.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherengine.app.core.Validators
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.local.SettingsStore
import com.weatherengine.app.data.repository.WeatherEngineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
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

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    fun toggleAuthMode() = setAuthMode(!_uiState.value.isSignUp)

    fun setAuthMode(isSignUp: Boolean) = _uiState.update {
        if (it.isSignUp == isSignUp) it else it.copy(isSignUp = isSignUp, emailError = null, passwordError = null, generalError = null)
    }

    fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        val emailOk = Validators.isValidEmail(s.email)
        val passOk = Validators.isValidPassword(s.password)
        if (!emailOk || !passOk) {
            _uiState.update {
                it.copy(
                    emailError = if (!emailOk) "Enter a valid email address" else null,
                    passwordError = if (!passOk) "Password must be at least 8 characters" else null
                )
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch(ioDispatcher) {
            val res = if (s.isSignUp) repository.signup(s.email.trim(), s.password) else repository.login(s.email.trim(), s.password)
            when (res) {
                is NetworkResult.Success -> {
                    settingsStore.saveAuth(res.data.token, res.data.email)
                    runCatching {
                        val p = repository.getPersonas()
                        if (p is NetworkResult.Success && p.data.isNotEmpty()) {
                            settingsStore.saveSelectedPersonas(p.data.map { it.code }.toSet())
                        }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToNextScreen.tryEmit(Unit)
                }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, generalError = res.message) }
            }
        }
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onSkip: () -> Unit = onAuthSuccess
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToNextScreen.collect { onAuthSuccess() }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("WeatherEngine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.isSignUp) "Create an account to sync personas" else "Sign in to your weather dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            TabRow(selectedTabIndex = if (state.isSignUp) 1 else 0, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = !state.isSignUp, onClick = { viewModel.setAuthMode(false) }, text = { Text("Log In", fontWeight = FontWeight.SemiBold) })
                Tab(selected = state.isSignUp, onClick = { viewModel.setAuthMode(true) }, text = { Text("Sign Up", fontWeight = FontWeight.SemiBold) })
            }
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Email address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.passwordError != null,
                supportingText = { state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle password")
                    }
                }
            )

            state.generalError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isSignUp) "Sign Up" else "Log In")
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSkip,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Continue as Guest")
            }
        }
    }
}
