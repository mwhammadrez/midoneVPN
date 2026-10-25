package com.example.ticker

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TickerFragment(private val ticks: Flow<Int>) : Fragment() {

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // BUG: collects for as long as the Fragment's view lifecycle
        // scope exists, but never stops/restarts around STOPPED - keeps
        // collecting (and whatever `ticks` does upstream keeps running:
        // a timer, a location subscription, a socket) even while this
        // screen is backgrounded.
        lifecycleScope.launch {
            ticks.collect { value ->
                render(value)
            }
        }
    }

    private fun render(value: Int) {
        // update UI
    }
}
