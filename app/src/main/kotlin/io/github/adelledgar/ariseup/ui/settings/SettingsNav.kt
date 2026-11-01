package io.github.adelledgar.ariseup.ui.settings

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.nav.Routes

fun SettingsRoutes(builder: NavGraphBuilder, nav: NavHostController, container: AppContainer) {
    builder.composable(Routes.SETTINGS) { Text("[Ajustes]") }
}
