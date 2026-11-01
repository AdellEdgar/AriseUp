package io.github.adelledgar.ariseup.ui.nutrition

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.nav.Routes

fun NutritionRoutes(builder: NavGraphBuilder, nav: NavHostController, container: AppContainer) {
    builder.composable(Routes.NUTRITION) { Text("[Nutrición]") }
}
