package io.github.adelledgar.ariseup.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.PlayerEntity
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.domain.DailyQuestScaling
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.StatBlock
import io.github.adelledgar.ariseup.system.MissionPlanner
import java.time.LocalDate
import java.time.Period

sealed interface OnboardingResult {
    data class Done(val hasDaily: Boolean, val message: String?) : OnboardingResult
    data class Invalid(val errors: Map<String, String>) : OnboardingResult
}

/** Errores por campo (height, weight, birthDate, name); vacío si todo es válido. */
fun validateProfile(heightCm: Double?, weightKg: Double?, birthDate: LocalDate?, today: LocalDate, name: String = "Jugador"): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (name.isBlank()) errors["name"] = "Indica tu nombre de Jugador"
    if (heightCm == null) errors["height"] = "Indica tu altura" else if (heightCm !in 120.0..230.0) errors["height"] = "Altura entre 120 y 230 cm"
    if (weightKg == null) errors["weight"] = "Indica tu peso" else if (weightKg !in 30.0..300.0) errors["weight"] = "Peso entre 30 y 300 kg"
    if (birthDate == null) errors["birthDate"] = "Indica tu fecha de nacimiento"
    else if (Period.between(birthDate, today).years < 14) errors["birthDate"] = "Debes tener al menos 14 años"
    return errors
}

class OnboardingViewModel(
    private val db: AriseUpDatabase,
    private val body: BodyRepository,
    private val planner: MissionPlanner,
    private val gameClock: GameClock,
) : ViewModel() {
    suspend fun finish(profile: Profile, test: FitnessTest): OnboardingResult {
        val today = gameClock.today()
        val errors = validateProfile(profile.heightCm, test.weightKg, profile.birthDate, today, profile.name)
        if (errors.isNotEmpty()) return OnboardingResult.Invalid(errors)
        val baseline = if (test.exerciseComplete) DailyQuestScaling.baseline(test) else null
        db.withTransaction {
            db.playerDao().upsert(
                PlayerEntity(
                    name = profile.name.trim(), sex = profile.sex, birthDate = profile.birthDate, heightCm = profile.heightCm,
                    activity = profile.activity, goal = profile.goal, level = 1, xp = 0, freePoints = 0,
                    stats = StatBlock.initial(), streak = 0, titleId = null, baseline = baseline, createdOn = today,
                ),
            )
            body.add(BodyEntryEntity(date = today, weightKg = test.weightKg, waistCm = test.waistCm, bodyFatPct = test.bodyFatPct))
            body.saveTest(test.copy(date = today), isInitial = true)
        }
        planner.ensureDay(today)
        return OnboardingResult.Done(
            hasDaily = baseline != null,
            message = if (baseline == null) "[Completa el test para recibir tu misión diaria]" else null,
        )
    }
}
