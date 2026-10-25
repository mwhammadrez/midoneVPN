package com.example.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// UiState now holds pre-resolved, pre-formatted values - no cents,
// no NumberFormat, no validation logic in the composables below.
data class CheckoutUiState(
    val lines: List<CartLineUi> = emptyList(),
    val promoCode: String = "",
    val promoError: String? = null,
    val shippingOptions: List<String> = emptyList(),
    val selectedShipping: String? = null,
    val subtotalFormatted: String = "",
    val discountFormatted: String = "",
    val totalFormatted: String = "",
    val isSubmitting: Boolean = false,
    val canSubmit: Boolean = false,
)

data class CartLineUi(val name: String, val quantity: Int, val lineTotalFormatted: String)

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onPromoCodeChange: (String) -> Unit,
    onShippingSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        CartLineList(state.lines)
        PromoCodeField(state.promoCode, state.promoError, onPromoCodeChange)
        ShippingSelector(state.shippingOptions, state.selectedShipping, onShippingSelected)
        OrderSummaryCard(state.subtotalFormatted, state.discountFormatted, state.totalFormatted)
        if (state.isSubmitting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onSubmit, enabled = state.canSubmit) { Text("Place order") }
        }
    }
}

@Composable
fun CartLineList(lines: List<CartLineUi>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Your cart")
        for (line in lines) {
            Row { Text(line.name); Text("x${line.quantity}"); Text(line.lineTotalFormatted) }
        }
    }
}

@Composable
fun PromoCodeField(value: String, error: String?, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Promo code")
        TextField(value = value, onValueChange = onValueChange)
        error?.let { Text(it) }
    }
}

@Composable
fun ShippingSelector(
    options: List<String>,
    selected: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("Shipping")
        for (option in options) {
            Row {
                Text(option)
                Button(onClick = { onSelected(option) }) {
                    Text(if (option == selected) "Selected" else "Select")
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(subtotal: String, discount: String, total: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Text("Subtotal: $subtotal")
        Text("Discount: $discount")
        Text("Total: $total")
    }
}

// Promo validation, discount math, and currency formatting now live in
// the ViewModel (see android-architecture / compose-development), e.g.:
//
// private fun buildUiState(lines: List<CartLine>, promoCode: String, ...): CheckoutUiState {
//     val promoValid = promoCode.isEmpty() || (promoCode.length == 6 && promoCode.all { it.isLetterOrDigit() })
//     val subtotalCents = lines.sumOf { it.unitPriceCents * it.quantity }
//     val discountCents = if (promoValid && promoCode.isNotEmpty()) subtotalCents / 10 else 0L
//     ...formatter.format(...) once, here, not in the composable...
// }
