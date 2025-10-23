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
import com.nibodhdaware.intentionality.supabase.SupabaseClientManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val supabase = SupabaseClientManager.client
    private val context: Context = IntentionalityApp.instance

    init {
        checkIfLoggedIn()
    }

    private fun checkIfLoggedIn() {
        viewModelScope.launch {
            try {
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userId = session.user?.id
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
            // Get Web Client ID from Google Cloud Console
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("889785018407-fc3bmn63rntqrnnk25t3778nc670jm3r.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            handleSignIn(result)
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Google Sign-In failed"
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

                // Sign in to Supabase using the ID token
                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = io.github.jan.supabase.gotrue.providers.Google
                }

                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    // Log Supabase user metadata
                    Log.d("LoginViewModel", "Supabase User ID: ${session.user?.id}")
                    Log.d("LoginViewModel", "Supabase Email: ${session.user?.email}")
                    Log.d("LoginViewModel", "Supabase Metadata: ${session.user?.userMetadata}")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userId = session.user?.id
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create session"
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

    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                _uiState.value = LoginUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Sign out failed"
                )
            }
        }
    }
}
