package com.example.sample.greeting.domain

// Pure domain model - no Android import anywhere in this module.
data class Greeting(val message: String, val recipientName: String)

interface GreetingRepository {
    suspend fun getGreeting(name: String): Greeting
}

class GetGreetingUseCase(private val repository: GreetingRepository) {
    suspend operator fun invoke(name: String): Greeting {
        require(name.isNotBlank()) { "name must not be blank" }
        return repository.getGreeting(name.trim())
    }
}
