package com.example.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Order(val id: String, val totalCents: Long, val status: String)

// domain
interface OrdersRepository {
    suspend fun getOrders(userId: String): List<Order>
}

// data
class OrderDao {
    suspend fun getOrders(userId: String): List<OrderEntity> = TODO()
}
data class OrderEntity(val id: String, val totalCents: Long, val status: String)
interface OrdersApi {
    suspend fun fetchOrders(userId: String): List<OrderEntity>
}

private fun OrderEntity.toDomain() = Order(id, totalCents, status)

class OrdersRepositoryImpl(
    private val dao: OrderDao,
    private val api: OrdersApi,
) : OrdersRepository {
    override suspend fun getOrders(userId: String): List<Order> {
        val remote = try {
            api.fetchOrders(userId)
        } catch (e: Exception) {
            dao.getOrders(userId)
        }
        return remote.map { it.toDomain() }
    }
}

// presentation - depends only on the repository interface
data class OrdersUiState(val orders: List<Order> = emptyList(), val isLoading: Boolean = false)

class OrdersViewModel(
    private val repository: OrdersRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = OrdersUiState(orders = repository.getOrders(userId), isLoading = false)
        }
    }
}
