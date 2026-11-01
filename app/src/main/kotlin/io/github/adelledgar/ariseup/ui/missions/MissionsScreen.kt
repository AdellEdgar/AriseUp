package io.github.adelledgar.ariseup.ui.missions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.adelledgar.ariseup.di.AppContainer

@Composable fun MissionsRoute(container: AppContainer, onManageRoutines: () -> Unit) { Text("[Misiones]") }

@Composable fun RoutineListRoute(container: AppContainer, onEdit: (Long) -> Unit, onBack: () -> Unit) { Text("[Rutinas]") }

@Composable fun RoutineEditorRoute(container: AppContainer, id: Long, onDone: () -> Unit) { Text("[Rutina]") }
