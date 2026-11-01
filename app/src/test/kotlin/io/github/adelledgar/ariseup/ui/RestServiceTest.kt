package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.RestPeriodEntity
import io.github.adelledgar.ariseup.data.repo.RestService
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.RestValidation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RestServiceTest {
    private val env = TestEnv()
    private val service = RestService(env.db, env.events, env.gameClock)

    @Test
    fun vacationCounterShown() = runTest {
        env.db.restDao().insert(RestPeriodEntity(reason = RestReason.VACATION, start = env.today.minusDays(40), end = env.today.minusDays(29), lastConfirmed = env.today))
        assertEquals("12/30", service.vacationUsage())
    }

    @Test
    fun startingTodayMakesTodaysMissionsOptional() = runTest {
        env.seedPlayer()
        env.db.dayDao().upsert(DayEntity(env.today, DayStatus.OPEN, restActive = false, targets = null))
        val id = env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 1.0))
        assertEquals(RestValidation.Ok, service.start(RestReason.ILLNESS, env.today, null))
        assertFalse(env.db.missionDao().get(id)!!.mandatory)
        assertTrue(env.db.dayDao().get(env.today)!!.restActive)
    }

    @Test
    fun rejectionMessagePassedThrough() = runTest {
        env.seedPlayer()
        assertEquals(
            RestValidation.Rejected("[Las vacaciones necesitan fecha de fin]"),
            service.start(RestReason.VACATION, env.today, null),
        )
    }

    @Test
    fun confirmAndEndEarly() = runTest {
        env.seedPlayer()
        service.start(RestReason.INJURY, env.today, null)
        val p = env.db.restDao().all().single()
        service.confirm(p.id)
        assertEquals(env.today, env.db.restDao().all().single().lastConfirmed)
        service.endNow(p.id)
        assertNotNull(env.db.restDao().all().single().endedEarly)
    }
}
