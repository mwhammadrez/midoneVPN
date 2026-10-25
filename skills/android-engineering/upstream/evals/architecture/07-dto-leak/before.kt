package com.example.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

// data layer: mirrors the wire format exactly
@Serializable
data class UserDto(
    val id: String,
    val display_name: String?,
    val avatar_url: String?,
    val is_verified_account: Int, // 0/1, API quirk
)

// BUG: UiState holds the DTO directly instead of a domain/UI model.
data class ProfileUiState(val user: UserDto? = null)

class ProfileViewModel(private val api: ProfileApi) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    suspend fun load(userId: String) {
        val dto = api.fetchUser(userId) // returns UserDto directly
        _uiState.value = ProfileUiState(user = dto)
    }
}

@Composable
fun ProfileScreen(state: ProfileUiState) {
    // Compose now depends on wire-format nullability and API-specific
    // shape (display_name can be null, is_verified_account is an Int).
    Text(state.user?.display_name ?: "Unknown")
    if (state.user?.is_verified_account == 1) {
        Text("Verified")
    }
}
