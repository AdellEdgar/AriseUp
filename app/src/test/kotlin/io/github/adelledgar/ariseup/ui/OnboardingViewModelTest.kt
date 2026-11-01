package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.system.MissionPlanner
import io.github.adelledgar.ariseup.ui.onboarding.OnboardingResult
import io.github.adelledgar.ariseup.ui.onboarding.OnboardingViewModel
import io.github.adelledgar.ariseup.ui.onboarding.validateProfile
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OnboardingViewModelTest {
    private val env = TestEnv()
    private val planner = MissionPlanner(env.db, env.players, env.missions, env.foods, env.settings, env.gameClock)
    private val vm = OnboardingViewModel(env.db, env.body, planner, env.gameClock)
    private val profile = Profile("Jin", Sex.MALE, LocalDate.of(1996, 5, 1), 178.0, ActivityLevel.MODERATE, Goal.LOSE_FAT)

    private fun test(complete: Boolean) = FitnessTest(
        env.today, 20, 30, if (complete) 40 else null, 2000, 5.0, 82.0, null, null,
    )

    @Test
    fun completeTestCreatesDailyWithBaseline() = runTest {
        val r = vm.finish(profile, test(complete = true))
        assertIs<OnboardingResult.Done>(r)
        assertTrue(r.hasDaily)
        val daily = env.db.missionDao().forDateAndKind(env.today, MissionKind.DAILY).single()
        assertTrue(daily.target!!.contains("\"pushUps\":10"))
        assertEquals(DailyTargets(10, 15, 20, 1.0), env.players.current()!!.baseline)
        assertEquals(82.0, env.body.latest()!!.weightKg)
        assertEquals(1, env.players.current()!!.progress.level)
    }

    @Test
    fun incompleteTestHasNoDaily() = runTest {
        val r = vm.finish(profile, test(complete = false))
        assertIs<OnboardingResult.Done>(r)
        assertEquals(false, r.hasDaily)
        assertEquals("[Completa el test para recibir tu misión diaria]", r.message)
        assertTrue(env.db.missionDao().forDateAndKind(env.today, MissionKind.DAILY).isEmpty())
    }

    @Test
    fun invalidHeightRejected() = runTest {
        val r = vm.finish(profile.copy(heightCm = 300.0), test(complete = true))
        assertIs<OnboardingResult.Invalid>(r)
        assertEquals("Altura entre 120 y 230 cm", r.errors["height"])
        assertEquals(null, env.players.current())
    }

    @Test
    fun validation() {
        val today = LocalDate.of(2026, 11, 2)
        assertTrue(validateProfile(178.0, 80.0, LocalDate.of(1996, 1, 1), today).isEmpty())
        assertEquals("Peso entre 30 y 300 kg", validateProfile(178.0, 20.0, LocalDate.of(1996, 1, 1), today)["weight"])
        assertEquals("Debes tener al menos 14 años", validateProfile(178.0, 80.0, today.minusYears(10), today)["birthDate"])
        assertEquals("Indica tu peso", validateProfile(178.0, null, LocalDate.of(1996, 1, 1), today)["weight"])
    }
}
