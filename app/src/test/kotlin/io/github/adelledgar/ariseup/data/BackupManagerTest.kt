package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.backup.BackupManager
import io.github.adelledgar.ariseup.data.backup.ImportResult
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.prefs.WeighInFrequency
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private val env = TestEnv()
    private val backup = BackupManager(env.db, env.settings, env.gameClock)

    private suspend fun seed() {
        env.seedPlayer(level = 7, xp = 120)
        env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 1.0))
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 80.0))
        val f = env.foods.insert(FoodEntity.create("Pollo", null, "123", FoodSource.CUSTOM, kcal = 165.0))
        env.foods.logFood(env.today, Meal.LUNCH, f, 150.0)
        env.events.append(listOf(SystemEvent.LevelUp(7)), env.today)
        env.settings.update { it.copy(weighInFrequency = WeighInFrequency.EVERY_2_DAYS) }
    }

    @Test
    fun roundTrip() = runTest {
        seed()
        val out = ByteArrayOutputStream()
        backup.export(out)
        val other = TestEnv()
        val r = BackupManager(other.db, other.settings, other.gameClock).import(out.toByteArray().inputStream())
        assertEquals(ImportResult.Ok, r)
        assertEquals(7, other.db.playerDao().get()!!.level)
        assertEquals(1, other.db.missionDao().all().size)
        assertEquals(1, other.db.foodDao().allLogs().size)
        assertEquals("123", other.db.foodDao().all().single().barcode)
        assertEquals(1, other.db.eventDao().count())
        assertEquals(WeighInFrequency.EVERY_2_DAYS, other.settings.current().weighInFrequency)
    }

    @Test
    fun importReplacesExistingData() = runTest {
        seed()
        val out = ByteArrayOutputStream()
        backup.export(out)
        env.foods.insert(FoodEntity.create("Extra", null, null, FoodSource.CUSTOM))
        assertEquals(ImportResult.Ok, backup.import(out.toByteArray().inputStream()))
        assertEquals(listOf("Pollo"), env.db.foodDao().all().map { it.name })
    }

    @Test
    fun corruptFileLeavesDataIntact() = runTest {
        seed()
        assertEquals(ImportResult.InvalidFile, backup.import("{not json".byteInputStream()))
        assertEquals(ImportResult.InvalidFile, backup.import("""{"hola":1}""".byteInputStream()))
        assertEquals(1, env.db.playerDao().count())
    }

    @Test
    fun newerSchemaRejected() = runTest {
        seed()
        val out = ByteArrayOutputStream()
        backup.export(out)
        val future = out.toString(Charsets.UTF_8.name()).replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":99")
        assertEquals(ImportResult.NewerSchema(99), backup.import(future.byteInputStream()))
        assertEquals(7, env.db.playerDao().get()!!.level)
    }
}
