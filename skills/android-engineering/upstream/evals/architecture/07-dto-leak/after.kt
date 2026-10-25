package com.example.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

// data layer only - never referenced above the repository.
@Serializable
private data class UserDto(
    val id: String,
    val display_name: String?,
    val avatar_url: String?,
    val is_verified_account: Int,
)

// domain layer
data class User(val id: String, val displayName: String, val isVerified: Boolean)

private fun UserDto.toDomain(): User = User(
    id = id,
    displayName = display_name ?: "Unknown",
    isVerified = is_verified_account == 1,
)

interface ProfileRepository {
    suspend fun getUser(userId: String): User
}

class ProfileRepositoryImpl(private val api: ProfileApi) : ProfileRepository {
    override suspend fun getUser(userId: String): User = api.fetchUser(userId).toDomain()
}

// presentation layer
data class ProfileUiState(val user: User? = null)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    suspend fun load(userId: String) {
        _uiState.value = ProfileUiState(user = repository.getUser(userId))
    }
}

@Composable
fun ProfileScreen(state: ProfileUiState) {
    Text(state.user?.displayName ?: "Unknown")
    if (state.user?.isVerified == true) {
        Text("Verified")
    }
}
