package io.github.adelledgar.ariseup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.RestPeriod
import io.github.adelledgar.ariseup.domain.model.RestReason
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
@Entity(tableName = "body_entry", indices = [Index("date")])
data class BodyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val weightKg: Double?,
    val waistCm: Double? = null,
    val bodyFatPct: Double? = null,
)

@Serializable
@Entity(tableName = "fitness_test", indices = [Index("date")])
data class FitnessTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val pushUps: Int?,
    val sitUps: Int?,
    val squats: Int?,
    val cooperMeters: Int?,
    val sitAndReachCm: Double?,
    val weightKg: Double?,
    val waistCm: Double?,
    val bodyFatPct: Double?,
    val isInitial: Boolean,
) {
    fun toDomain() = FitnessTest(date, pushUps, sitUps, squats, cooperMeters, sitAndReachCm, weightKg, waistCm, bodyFatPct)

    companion object {
        fun from(t: FitnessTest, isInitial: Boolean, id: Long = 0) = FitnessTestEntity(
            id, t.date, t.pushUps, t.sitUps, t.squats, t.cooperMeters, t.sitAndReachCm, t.weightKg, t.waistCm, t.bodyFatPct, isInitial,
        )
    }
}

@Serializable
@Entity(tableName = "rest_period")
data class RestPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reason: RestReason,
    @Serializable(LocalDateSerializer::class) val start: LocalDate,
    @Serializable(LocalDateSerializer::class) val end: LocalDate?,
    @Serializable(LocalDateSerializer::class) val lastConfirmed: LocalDate,
    @Serializable(LocalDateSerializer::class) val endedEarly: LocalDate? = null,
    @Serializable(LocalDateTimeSerializer::class) val confirmPromptAt: LocalDateTime? = null,
) {
    fun toDomain() = RestPeriod(id, reason, start, end, lastConfirmed, endedEarly)
}

@Serializable
@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    @Serializable(InstantSerializer::class) val unlockedAt: Instant,
)

/** Registro de eventos del Sistema; [payload] es el [io.github.adelledgar.ariseup.domain.model.SystemEvent] en JSON. */
@Serializable
@Entity(tableName = "event", indices = [Index("at")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(InstantSerializer::class) val at: Instant,
    @Serializable(LocalDateSerializer::class) val day: LocalDate?,
    val type: String,
    val payload: String,
)

/** Registros de Health Connect ya importados, para no duplicarlos. */
@Serializable
@Entity(tableName = "health_import")
data class HealthImportEntity(
    @PrimaryKey val recordId: String,
    val missionId: Long,
)
