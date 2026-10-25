package com.example.sample.greeting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.greeting.domain.GetGreetingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One immutable UiState, per compose-development - not several separate
// mutableStateOf fields.
data class GreetingUiState(
    val name: String = "",
    val greetingMessage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class GreetingViewModel(private val getGreeting: GetGreetingUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(GreetingUiState())
    val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onSubmit() {
        val name = _uiState.value.name
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { getGreeting(name) }
                .onSuccess { greeting ->
                    _uiState.update { it.copy(isLoading = false, greetingMessage = "${greeting.message}, ${greeting.recipientName}!") }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.message ?: "Something went wrong") }
                }
        }
    }
}
