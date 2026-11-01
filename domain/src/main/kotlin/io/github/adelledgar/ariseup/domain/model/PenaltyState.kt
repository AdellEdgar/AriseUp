package io.github.adelledgar.ariseup.domain.model

import java.time.LocalDate

/** Zona de Penalización activa. La degradación empieza en el cierre de [firstDegradeOnCloseOf]. */
data class PenaltyState(
    val startedOn: LocalDate,
    val firstDegradeOnCloseOf: LocalDate,
    val targets: DailyTargets,
    val degradeCount: Int,
)
