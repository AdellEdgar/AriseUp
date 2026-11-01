package io.github.adelledgar.ariseup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.adelledgar.ariseup.ui.nav.AriseUpRoot
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as AriseUpApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AriseUpRoot(container) }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val events = container.coordinator.catchUp()
            container.notifier.postCloseEvents(events)
        }
    }
}
