package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class RestReason(val label: String) {
    VACATION("Vacaciones"),
    ILLNESS("Enfermedad"),
    INJURY("Lesión"),
}

data class RestPeriod(
    val id: Long,
    val reason: RestReason,
    val start: LocalDate,
    val end: LocalDate?,
    val lastConfirmed: LocalDate,
    val endedEarly: LocalDate?,
) {
    /** Último día cubierto, o null si es indefinido. */
    val effectiveEnd: LocalDate? get() = endedEarly ?: end

    fun covers(day: LocalDate): Boolean = !day.isBefore(start) && (effectiveEnd == null || !day.isAfter(effectiveEnd))
}

sealed interface RestValidation {
    data object Ok : RestValidation
    data class Rejected(val reason: String) : RestValidation
}
