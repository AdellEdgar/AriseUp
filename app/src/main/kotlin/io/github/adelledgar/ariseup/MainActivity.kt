package io.github.adelledgar.ariseup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as AriseUpApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("[Sistema iniciado]") }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val events = container.coordinator.catchUp()
            container.notifier.postCloseEvents(events)
        }
    }
}
