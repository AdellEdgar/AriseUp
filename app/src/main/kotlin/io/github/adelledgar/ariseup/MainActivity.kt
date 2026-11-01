package io.github.adelledgar.ariseup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.adelledgar.ariseup.data.gameDays
import io.github.adelledgar.ariseup.ui.nav.AriseUpRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as AriseUpApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AriseUpRoot(container) }
        // En primer plano: al abrir y cada vez que cambia el día de juego, importar carreras y cerrar días pendientes.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                gameDays(container.gameClock).collect { syncAndClose() }
            }
        }
    }

    private suspend fun syncAndClose() {
        try {
            runCatching { container.healthImporter.importOpenDays() }
            container.notifier.postCloseEvents(container.coordinator.catchUp())
        } catch (e: Exception) {
            // Un fallo del cierre no debe impedir abrir la app; se reintenta en el siguiente ciclo.
            Log.e("AriseUp", "Error al cerrar días pendientes", e)
        }
    }
}
