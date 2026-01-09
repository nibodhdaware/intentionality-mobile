package com.nibodhdaware.intentionality.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nibodhdaware.intentionality.IntentionalityApp
import com.nibodhdaware.intentionality.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val userId: String? = null,
    val isSignUpMode: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val context: Context = IntentionalityApp.instance

    init {
        checkIfLoggedIn()
        Log.d("LoginViewModel", "LoginViewModel initialized")
        Log.d("LoginViewModel", "Firebase Auth instance: ${FirebaseManager.getCurrentUser()}")
    }

    private fun checkIfLoggedIn() {
        viewModelScope.launch {
            try {
                val user = FirebaseManager.getCurrentUser()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userId = user.uid
                    )
                }
            } catch (e: Exception) {
                // Not logged in
            }
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        return try {
            Log.d("LoginViewModel", "Starting Google Sign-In...")
            
            // Get Web Client ID from Firebase google-services.json
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("938266027514-vgh2o1odih2hbqpae8h2qe0kbe1ugk94.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            Log.d("LoginViewModel", "Credential received, processing...")
            handleSignIn(result)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Google Sign-In failed", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = when {
                    e.message?.contains("cancel", ignoreCase = true) == true -> 
                        "Sign-in was cancelled. Please try again."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your connection."
                    else -> e.message ?: "Google Sign-In failed"
                }
            )
            Result.failure(e)
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                // Log Google profile data
                Log.d("LoginViewModel", "Google ID: ${googleIdTokenCredential.id}")
                Log.d("LoginViewModel", "Display Name: ${googleIdTokenCredential.displayName}")
                Log.d("LoginViewModel", "Profile Picture URI: ${googleIdTokenCredential.profilePictureUri}")
                Log.d("LoginViewModel", "Given Name: ${googleIdTokenCredential.givenName}")
                Log.d("LoginViewModel", "Family Name: ${googleIdTokenCredential.familyName}")

                // Sign in to Firebase using the ID token
                val authResult = FirebaseManager.signInWithGoogle(idToken)
                
                authResult.onSuccess { user ->
                    Log.d("LoginViewModel", "Firebase User ID: ${user.uid}")
                    Log.d("LoginViewModel", "Firebase Email: ${user.email}")
                    Log.d("LoginViewModel", "Display Name: ${user.displayName}")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userId = user.uid
                    )
                }.onFailure { error ->
                    Log.e("LoginViewModel", "Firebase sign-in error", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Firebase authentication failed"
                    )
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Sign-in error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Authentication failed"
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid credential type"
            )
        }
    }

    fun skipLogin() {
        // For development purposes, create a mock session
        _uiState.value = _uiState.value.copy(
            isLoggedIn = true,
            userId = "dev-user-${System.currentTimeMillis()}"
        )
    }
    
    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null
        )
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        return try {
            val result = FirebaseManager.signInWithEmail(email, password)
            
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userId = user.uid
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when {
                        error.message?.contains("password", ignoreCase = true) == true ->
                            "Invalid email or password"
                        error.message?.contains("user", ignoreCase = true) == true ->
                            "No account found with this email"
                        else -> error.message ?: "Sign in failed"
                    }
                )
            }
            
            result.map { }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Sign in failed"
            )
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        return try {
            val result = FirebaseManager.signUpWithEmail(email, password)
            
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userId = user.uid
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when {
                        error.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                            "This email is already registered"
                        error.message?.contains("weak-password", ignoreCase = true) == true ->
                            "Password should be at least 6 characters"
                        error.message?.contains("invalid-email", ignoreCase = true) == true ->
                            "Please enter a valid email"
                        else -> error.message ?: "Sign up failed"
                    }
                )
            }
            
            result.map { }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Sign up failed"
            )
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        return try {
            val result = FirebaseManager.resetPassword(email)
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Password reset failed"
                )
            }
            
            result
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Password reset failed"
            )
            Result.failure(e)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                FirebaseManager.signOut()
                _uiState.value = LoginUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Sign out failed"
                )
            }
        }
    }
}
