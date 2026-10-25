package com.example.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sample.greeting.data.GreetingRepositoryImpl
import com.example.sample.greeting.domain.GetGreetingUseCase
import com.example.sample.greeting.presentation.GreetingRoute
import com.example.sample.greeting.presentation.GreetingViewModel

// Minimal manual wiring - a real project uses its chosen DI framework
// (see skills/android-architecture's dependency-injection section)
// instead of constructing dependencies by hand in an Activity.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val useCase = GetGreetingUseCase(GreetingRepositoryImpl())
        setContent {
            val viewModel: GreetingViewModel = viewModel { GreetingViewModel(useCase) }
            GreetingRoute(viewModel = viewModel)
        }
    }
}
