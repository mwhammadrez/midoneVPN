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
import java.text.NumberFormat
import java.util.Locale

data class CartLine(val name: String, val quantity: Int, val unitPriceCents: Long)

// BUG: one composable renders the entire checkout screen - cart list,
// promo code entry with validation, shipping selection, price summary
// with formatting logic, and the submit button - all in one function
// with business logic (currency formatting, promo validation, total
// calculation) inlined into the UI tree.
@Composable
fun CheckoutScreen(
    lines: List<CartLine>,
    promoCode: String,
    onPromoCodeChange: (String) -> Unit,
    shippingOptions: List<String>,
    selectedShipping: String,
    onShippingSelected: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your cart")
        for (line in lines) {
            Row {
                Text(line.name)
                Text("x${line.quantity}")
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                Text(formatter.format(line.unitPriceCents * line.quantity / 100.0))
            }
        }

        Text("Promo code")
        TextField(value = promoCode, onValueChange = onPromoCodeChange)
        val promoValid = promoCode.isEmpty() || (promoCode.length == 6 && promoCode.all { it.isLetterOrDigit() })
        if (!promoValid) {
            Text("Invalid promo code")
        }

        Text("Shipping")
        for (option in shippingOptions) {
            Row {
                Text(option)
                Button(onClick = { onShippingSelected(option) }) {
                    Text(if (option == selectedShipping) "Selected" else "Select")
                }
            }
        }

        val subtotalCents = lines.sumOf { it.unitPriceCents * it.quantity }
        val discountCents = if (promoValid && promoCode.isNotEmpty()) subtotalCents / 10 else 0L
        val totalCents = subtotalCents - discountCents
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        Card {
            Text("Subtotal: ${formatter.format(subtotalCents / 100.0)}")
            Text("Discount: ${formatter.format(discountCents / 100.0)}")
            Text("Total: ${formatter.format(totalCents / 100.0)}")
        }

        if (isSubmitting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onSubmit, enabled = promoValid) {
                Text("Place order")
            }
        }
    }
}
