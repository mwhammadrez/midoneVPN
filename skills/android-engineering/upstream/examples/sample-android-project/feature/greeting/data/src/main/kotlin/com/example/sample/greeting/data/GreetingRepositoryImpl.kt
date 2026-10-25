package com.example.sample.greeting.data

import com.example.sample.greeting.domain.Greeting
import com.example.sample.greeting.domain.GreetingRepository

// Stands in for a real remote/local data source. In a real project this
// would call a Retrofit/Ktor API and map a DTO to the domain model here
// (see skills/android-architecture) - kept in-memory to stay minimal.
class GreetingRepositoryImpl : GreetingRepository {
    override suspend fun getGreeting(name: String): Greeting =
        Greeting(message = "Hello", recipientName = name)
}
