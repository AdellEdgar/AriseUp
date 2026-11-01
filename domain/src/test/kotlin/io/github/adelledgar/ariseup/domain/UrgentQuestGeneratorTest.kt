package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrgentQuestGeneratorTest {
    private val d = LocalDate.of(2026, 11, 2)
    private val t = DailyTargets(40, 40, 40, 4.0)

    @Test
    fun urgentNeverInRestOrPenalty() {
        repeat(1000) {
            assertNull(UrgentQuestGenerator.maybeGenerate(d, Random(it), 0, inRest = true, inPenalty = false, targets = t))
            assertNull(UrgentQuestGenerator.maybeGenerate(d, Random(it), 0, inRest = false, inPenalty = true, targets = t))
        }
    }

    @Test
    fun urgentWeeklyCap() {
        repeat(1000) { assertNull(UrgentQuestGenerator.maybeGenerate(d, Random(it), 2, false, false, t)) }
    }

    @Test
    fun urgentRateAbout20pct() {
        val n = (0 until 10_000).count { UrgentQuestGenerator.maybeGenerate(d, Random(it), 0, false, false, t) != null }
        assertTrue(n in 1800..2200, "n = $n")
    }

    @Test
    fun urgentBounds() {
        val specs = (0 until 2000).mapNotNull { UrgentQuestGenerator.maybeGenerate(d, Random(it), 0, false, false, t) }
        assertTrue(specs.isNotEmpty())
        for (s in specs) {
            assertTrue(s.appearAt >= LocalTime.of(10, 0) && s.appearAt <= LocalTime.of(19, 0))
            assertTrue(s.durationHours in 3..6)
            if (s.exercise == DailyExercise.RUN) {
                assertTrue(s.amount in 1.0..2.5, "km = ${s.amount}")
                assertEquals(0.0, s.amount * 2 % 1.0, 1e-9)
            } else {
                assertTrue(s.amount in ceil(40 * 0.3)..ceil(40 * 0.6), "reps = ${s.amount}")
            }
        }
        assertEquals(DailyExercise.entries.toSet(), specs.map { it.exercise }.toSet())
    }

    @Test
    fun deterministicWithSeed() {
        assertEquals(
            UrgentQuestGenerator.maybeGenerate(d, Random(42), 0, false, false, t),
            UrgentQuestGenerator.maybeGenerate(d, Random(42), 0, false, false, t),
        )
    }

    @Test
    fun failurePenalty() {
        assertEquals(10, UrgentQuestGenerator.urgentFailurePenalty(1))
        assertEquals(316, UrgentQuestGenerator.urgentFailurePenalty(10))
    }
}
