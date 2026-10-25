package com.example.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// data-layer types
class OrderDao {
    suspend fun getOrders(userId: String): List<OrderEntity> = TODO()
}
data class OrderEntity(val id: String, val totalCents: Long, val status: String)

interface OrdersApi {
    suspend fun fetchOrders(userId: String): List<OrderEntity>
}

data class OrdersUiState(val orders: List<OrderEntity> = emptyList(), val isLoading: Boolean = false)

// BUG: ViewModel injects the DAO and API directly, bypassing any
// repository interface, and reaches into both data sources itself.
class OrdersViewModel(
    private val dao: OrderDao,
    private val api: OrdersApi,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val local = dao.getOrders(userId)
            val remote = try {
                api.fetchOrders(userId)
            } catch (e: Exception) {
                local
            }
            _uiState.value = OrdersUiState(orders = remote, isLoading = false)
        }
    }
}
