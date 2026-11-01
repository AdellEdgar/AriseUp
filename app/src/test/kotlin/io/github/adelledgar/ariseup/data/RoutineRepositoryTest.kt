package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.ExerciseType
import io.github.adelledgar.ariseup.data.db.RoutineEntity
import io.github.adelledgar.ariseup.data.db.RoutineExerciseEntity
import io.github.adelledgar.ariseup.data.seed.BuiltInExercises
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.RoutineCategory
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryTest {
    private val env = TestEnv()
    private val monday = LocalDate.of(2026, 11, 2)

    private suspend fun routine(mask: Int, name: String = "Pierna"): Long {
        env.routines.seedExercisesIfEmpty()
        val squat = env.routines.exercises().first { it.type == ExerciseType.REPS_WEIGHT }
        return env.routines.save(
            RoutineEntity(name = name, daysOfWeekMask = mask, category = RoutineCategory.STRENGTH, mandatory = true),
            listOf(RoutineExerciseEntity(routineId = 0, exerciseId = squat.id, sets = 2, targetPerSet = 8.0, position = 0)),
        )
    }

    @Test
    fun builtInsSeededOnce() = runTest {
        env.routines.seedExercisesIfEmpty()
        env.routines.seedExercisesIfEmpty()
        assertEquals(BuiltInExercises.all.size, env.routines.exercises().size)
        assertTrue(BuiltInExercises.all.size >= 35)
    }

    @Test
    fun activeForUsesWeekMask() = runTest {
        routine(mask = 0b0000001, name = "Lunes")
        routine(mask = 0b0000010, name = "Martes")
        assertEquals(listOf("Lunes"), env.routines.activeFor(monday).map { it.routine.name })
    }

    @Test
    fun archivedNotActive() = runTest {
        val id = routine(mask = 0b1111111)
        env.routines.archive(id)
        assertTrue(env.routines.activeFor(monday).isEmpty())
    }

    @Test
    fun logSetCompletesRoutineWhenAllSetsMeetTarget() = runTest {
        env.seedPlayer()
        val id = routine(mask = 0b0000001)
        val r = env.routines.activeFor(monday).single()
        val mission = env.missions.createSimple(env.today, MissionKind.ROUTINE, "Pierna", mandatory = true, refId = id)
        val re = r.exercises.single().link
        assertTrue(env.missions.logSet(mission, re.id, 0, reps = 8, weightKg = 60.0).isEmpty())
        assertTrue(env.missions.logSet(mission, re.id, 1, reps = 7, weightKg = 60.0).isEmpty())
        assertNull(env.db.missionDao().get(mission)!!.completedAt)
        val events = env.missions.logSet(mission, re.id, 1, reps = 8, weightKg = 60.0)
        assertTrue(events.contains(SystemEvent.StatsChanged(mapOf(Stat.FUE to 0.3), "Pierna")))
        assertEquals(2, env.db.routineDao().setsOf(mission).size)
    }

    @Test
    fun loggingOnClosedDayFails() = runTest {
        env.seedPlayer()
        val id = routine(mask = 0b1111111)
        val yesterday = env.today.minusDays(1)
        val mission = env.missions.createSimple(yesterday, MissionKind.ROUTINE, "Pierna", mandatory = true, refId = id)
        env.db.dayDao().upsert(DayEntity(yesterday, DayStatus.CLOSED, restActive = false, targets = null))
        val re = env.routines.withExercises(id)!!.exercises.single().link
        val e = assertFailsWith<IllegalStateException> { env.missions.logSet(mission, re.id, 0, reps = 8) }
        assertEquals("[Este día ya está cerrado]", e.message)
    }
}
