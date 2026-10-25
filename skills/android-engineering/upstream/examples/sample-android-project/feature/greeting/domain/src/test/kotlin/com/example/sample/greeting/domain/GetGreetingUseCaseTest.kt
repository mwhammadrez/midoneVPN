package com.example.sample.greeting.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private class FakeGreetingRepository(private val greeting: Greeting) : GreetingRepository {
    override suspend fun getGreeting(name: String): Greeting = greeting.copy(recipientName = name)
}

class GetGreetingUseCaseTest {

    @Test
    fun `trims whitespace before calling the repository`() = runTest {
        val repository = FakeGreetingRepository(Greeting("Hello", ""))
        val useCase = GetGreetingUseCase(repository)

        val result = useCase("  Ada  ")

        assertEquals("Ada", result.recipientName)
    }

    @Test
    fun `rejects a blank name without calling the repository`() = runTest {
        val repository = FakeGreetingRepository(Greeting("Hello", ""))
        val useCase = GetGreetingUseCase(repository)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase("   ") }
        }
    }
}
