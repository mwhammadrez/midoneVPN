package com.example.counter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun Counter() {
    // BUG: plain local var, not held in Compose state. Mutating it does
    // not trigger recomposition, and it resets to 0 on every
    // recomposition anyway since it's re-initialized each call.
    var count = 0

    Column {
        Text("Count: $count")
        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}
