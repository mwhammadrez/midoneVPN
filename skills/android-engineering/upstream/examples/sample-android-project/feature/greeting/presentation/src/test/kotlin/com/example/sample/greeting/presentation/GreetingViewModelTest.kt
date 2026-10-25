package com.example.sample.greeting.presentation

import com.example.sample.greeting.domain.GetGreetingUseCase
import com.example.sample.greeting.domain.Greeting
import com.example.sample.greeting.domain.GreetingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeGreetingRepository : GreetingRepository {
    override suspend fun getGreeting(name: String): Greeting = Greeting("Hello", name)
}

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `submit produces a greeting message for the entered name`() = runTest {
        val viewModel = GreetingViewModel(GetGreetingUseCase(FakeGreetingRepository()))

        viewModel.onNameChange("Ada")
        viewModel.onSubmit()

        assertEquals("Hello, Ada!", viewModel.uiState.value.greetingMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `submitting a blank name surfaces an error instead of a greeting`() = runTest {
        val viewModel = GreetingViewModel(GetGreetingUseCase(FakeGreetingRepository()))

        viewModel.onSubmit() // name defaults to ""

        assertEquals(null, viewModel.uiState.value.greetingMessage)
        assert(viewModel.uiState.value.error != null)
    }
}
