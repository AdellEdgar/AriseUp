package io.github.adelledgar.ariseup.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.missions.MissionsRoute
import io.github.adelledgar.ariseup.ui.missions.RoutineEditorRoute
import io.github.adelledgar.ariseup.ui.missions.RoutineListRoute
import io.github.adelledgar.ariseup.ui.nutrition.NutritionRoutes
import io.github.adelledgar.ariseup.ui.onboarding.OnboardingRoute
import io.github.adelledgar.ariseup.ui.progress.ProgressRoute
import io.github.adelledgar.ariseup.ui.settings.SettingsRoutes
import io.github.adelledgar.ariseup.ui.status.StatusRoute
import io.github.adelledgar.ariseup.ui.theme.AriseUpTheme
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors

object Routes {
    const val STATUS = "status"
    const val MISSIONS = "missions"
    const val NUTRITION = "nutrition"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val ROUTINES = "routines"
    const val ROUTINE_EDIT = "routine/{id}"
    fun routineEdit(id: Long) = "routine/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.STATUS, "Estado", Icons.Default.Person),
    Tab(Routes.MISSIONS, "Misiones", Icons.Default.FitnessCenter),
    Tab(Routes.NUTRITION, "Nutrición", Icons.Default.Restaurant),
    Tab(Routes.PROGRESS, "Progreso", Icons.Default.AutoGraph),
)

/** Raíz de la interfaz: primer arranque o las cuatro pestañas. */
@Composable
fun AriseUpRoot(container: AppContainer) {
    val state by container.players.state().collectAsStateWithLifecycle(initialValue = null, minActiveState = androidx.lifecycle.Lifecycle.State.CREATED)
    val player by container.players.entity().collectAsStateWithLifecycle(initialValue = PLACEHOLDER)
    AriseUpTheme(penalty = state?.penalty != null) {
        Box(Modifier.fillMaxSize().background(SystemColors.background)) {
            when {
                player === PLACEHOLDER -> Unit
                player == null -> OnboardingRoute(container)
                else -> MainScaffold(container)
            }
        }
    }
}

private val PLACEHOLDER = io.github.adelledgar.ariseup.data.db.PlayerEntity(
    id = -1, name = "", sex = io.github.adelledgar.ariseup.domain.model.Sex.MALE, birthDate = java.time.LocalDate.MIN,
    heightCm = 0.0, activity = io.github.adelledgar.ariseup.domain.model.ActivityLevel.SEDENTARY,
    goal = io.github.adelledgar.ariseup.domain.model.Goal.MAINTAIN, level = 0, xp = 0, freePoints = 0,
    stats = io.github.adelledgar.ariseup.domain.model.StatBlock.initial(), streak = 0, titleId = null, baseline = null,
    createdOn = java.time.LocalDate.MIN,
)

@Composable
private fun MainScaffold(container: AppContainer) {
    val nav = rememberNavController()
    Scaffold(
        containerColor = SystemColors.background,
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        NavHost(nav, startDestination = Routes.STATUS, modifier = Modifier.padding(padding)) {
            composable(Routes.STATUS) { StatusRoute(container, onOpenSettings = { nav.navigate(Routes.SETTINGS) }) }
            composable(Routes.MISSIONS) { MissionsRoute(container, onManageRoutines = { nav.navigate(Routes.ROUTINES) }) }
            composable(Routes.ROUTINES) {
                RoutineListRoute(container, onEdit = { nav.navigate(Routes.routineEdit(it)) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.ROUTINE_EDIT) { entry ->
                RoutineEditorRoute(container, entry.arguments?.getString("id")?.toLongOrNull() ?: 0L, onDone = { nav.popBackStack() })
            }
            NutritionRoutes(this, nav, container)
            composable(Routes.PROGRESS) { ProgressRoute(container) }
            SettingsRoutes(this, nav, container)
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val accent = LocalSystemPalette.current.accent
    NavigationBar(containerColor = Color(0xFF070B14)) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f),
                    unselectedIconColor = SystemColors.muted,
                    unselectedTextColor = SystemColors.muted,
                ),
            )
        }
    }
}
