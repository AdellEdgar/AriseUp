package io.github.adelledgar.ariseup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.PenaltyState
import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.domain.model.StatBlock
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Fila única con el perfil y el progreso del Jugador. Existe solo tras el primer arranque. */
@Serializable
@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val sex: Sex,
    @Serializable(LocalDateSerializer::class) val birthDate: LocalDate,
    val heightCm: Double,
    val activity: ActivityLevel,
    val goal: Goal,
    val level: Int,
    val xp: Long,
    val freePoints: Int,
    val stats: StatBlock,
    val streak: Int,
    val titleId: String?,
    val baseline: DailyTargets?,
    @Serializable(LocalDateSerializer::class) val createdOn: LocalDate,
) {
    fun progress() = PlayerProgress(level, xp, freePoints, stats, streak)

    fun profile() = Profile(name, sex, birthDate, heightCm, activity, goal)

    fun withProgress(p: PlayerProgress) = copy(level = p.level, xp = p.xp, freePoints = p.freePoints, stats = p.stats, streak = p.streak)
}

@Serializable
@Entity(tableName = "penalty")
data class PenaltyEntity(
    @PrimaryKey val id: Int = 1,
    @Serializable(LocalDateSerializer::class) val startedOn: LocalDate,
    @Serializable(LocalDateSerializer::class) val firstDegradeOnCloseOf: LocalDate,
    val targets: DailyTargets,
    val degradeCount: Int,
) {
    fun toDomain() = PenaltyState(startedOn, firstDegradeOnCloseOf, targets, degradeCount)

    companion object {
        fun from(s: PenaltyState) = PenaltyEntity(1, s.startedOn, s.firstDegradeOnCloseOf, s.targets, s.degradeCount)
    }
}

enum class DayStatus { OPEN, EXERCISE_CLOSED, CLOSED }

@Serializable
@Entity(tableName = "day")
data class DayEntity(
    @PrimaryKey @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val status: DayStatus,
    val restActive: Boolean,
    val targets: DailyTargets?,
)
