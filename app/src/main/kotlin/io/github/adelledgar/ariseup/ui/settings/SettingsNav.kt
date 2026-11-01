package io.github.adelledgar.ariseup.ui.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.nav.Routes

private const val REST = "settings/rest"

fun SettingsRoutes(builder: NavGraphBuilder, nav: NavHostController, container: AppContainer) = with(builder) {
    composable(Routes.SETTINGS) { SettingsScreen(container, onRest = { nav.navigate(REST) }, onBack = { nav.popBackStack() }) }
    composable(REST) { RestModeScreen(container, onBack = { nav.popBackStack() }) }
}
