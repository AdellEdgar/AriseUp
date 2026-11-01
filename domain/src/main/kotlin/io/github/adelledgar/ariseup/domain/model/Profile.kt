package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period

@Serializable
enum class Sex(val label: String) { MALE("Hombre"), FEMALE("Mujer") }

@Serializable
enum class Goal(val label: String) {
    LOSE_FAT("Perder grasa"),
    MAINTAIN("Mantener"),
    GAIN_MUSCLE("Ganar músculo"),
}

@Serializable
enum class ActivityLevel(val factor: Double, val label: String) {
    SEDENTARY(1.2, "Sedentario"),
    LIGHT(1.375, "Ligero"),
    MODERATE(1.55, "Moderado"),
    HIGH(1.725, "Alto"),
    VERY_HIGH(1.9, "Muy alto"),
}

data class Profile(
    val name: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val activity: ActivityLevel,
    val goal: Goal,
) {
    fun age(today: LocalDate): Int = Period.between(birthDate, today).years
}
