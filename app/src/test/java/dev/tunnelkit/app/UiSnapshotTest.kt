package dev.tunnelkit.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.tunnelkit.app.ui.HomeScreen
import dev.tunnelkit.app.ui.Methods
import dev.tunnelkit.app.ui.SampleServers
import org.junit.Rule
import org.junit.Test

class UiSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.NoActionBar")

    @Test
    fun home_disconnected() {
        paparazzi.snapshot {
            HomeScreen(connected = false, method = Methods.first(), server = SampleServers[1])
        }
    }

    @Test
    fun home_empty() {
        paparazzi.snapshot {
            HomeScreen(connected = false, method = Methods.first(), server = null, servers = emptyList())
        }
    }

    @Test
    fun home_connected() {
        paparazzi.snapshot {
            HomeScreen(connected = true, method = Methods.first(), server = SampleServers[1])
        }
    }
}
