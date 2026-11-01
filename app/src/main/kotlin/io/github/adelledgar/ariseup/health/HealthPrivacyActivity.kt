package io.github.adelledgar.ariseup.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.theme.AriseUpTheme
import io.github.adelledgar.ariseup.ui.theme.SystemColors

/** Explicación del uso de datos de Health Connect (obligatoria para pedir permisos). */
class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AriseUpTheme(penalty = false) {
                SystemWindow("Privacidad de Health Connect", Modifier.padding(16.dp), onClose = { finish() }) {
                    Text(
                        "AriseUp solo lee tus sesiones de carrera y su distancia para sumarlas a la misión diaria. " +
                            "No escribe nada en Health Connect y los datos nunca salen de tu móvil.",
                        color = SystemColors.text,
                    )
                }
            }
        }
    }
}
