package com.example.sample.greeting.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GreetingRoute(viewModel: GreetingViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GreetingScreen(
        state = uiState,
        onNameChange = viewModel::onNameChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

// Stateless, hoisted - previewable and testable without a ViewModel.
@Composable
fun GreetingScreen(
    state: GreetingUiState,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        TextField(value = state.name, onValueChange = onNameChange, label = { Text("Your name") })
        Button(onClick = onSubmit, enabled = state.name.isNotBlank() && !state.isLoading) {
            Text("Say hello")
        }
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text("Error: ${state.error}")
            state.greetingMessage != null -> Text(state.greetingMessage)
        }
    }
}
