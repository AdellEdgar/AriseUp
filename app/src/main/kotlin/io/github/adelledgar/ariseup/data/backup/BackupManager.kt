package io.github.adelledgar.ariseup.data.backup

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.db.AchievementEntity
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DietRuleEntity
import io.github.adelledgar.ariseup.data.db.DietRuleLogEntity
import io.github.adelledgar.ariseup.data.db.EventEntity
import io.github.adelledgar.ariseup.data.db.ExerciseEntity
import io.github.adelledgar.ariseup.data.db.FitnessTestEntity
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodLogEntity
import io.github.adelledgar.ariseup.data.db.HealthImportEntity
import io.github.adelledgar.ariseup.data.db.MissionEntity
import io.github.adelledgar.ariseup.data.db.PenaltyEntity
import io.github.adelledgar.ariseup.data.db.PlayerEntity
import io.github.adelledgar.ariseup.data.db.RecipeEntity
import io.github.adelledgar.ariseup.data.db.RecipeItemEntity
import io.github.adelledgar.ariseup.data.db.RestPeriodEntity
import io.github.adelledgar.ariseup.data.db.RoutineEntity
import io.github.adelledgar.ariseup.data.db.RoutineExerciseEntity
import io.github.adelledgar.ariseup.data.db.SetLogEntity
import io.github.adelledgar.ariseup.data.prefs.Settings
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class BackupTables(
    val player: List<PlayerEntity> = emptyList(),
    val penalty: List<PenaltyEntity> = emptyList(),
    val days: List<DayEntity> = emptyList(),
    val missions: List<MissionEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineExercises: List<RoutineExerciseEntity> = emptyList(),
    val sets: List<SetLogEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeItems: List<RecipeItemEntity> = emptyList(),
    val foodLogs: List<FoodLogEntity> = emptyList(),
    val rules: List<DietRuleEntity> = emptyList(),
    val ruleLogs: List<DietRuleLogEntity> = emptyList(),
    val body: List<BodyEntryEntity> = emptyList(),
    val tests: List<FitnessTestEntity> = emptyList(),
    val rests: List<RestPeriodEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val health: List<HealthImportEntity> = emptyList(),
)

@Serializable
data class Backup(val schemaVersion: Int, val exportedAt: Long, val settings: Settings, val tables: BackupTables)

sealed interface ImportResult {
    data object Ok : ImportResult
    data object InvalidFile : ImportResult
    data class NewerSchema(val version: Int) : ImportResult
}

/** Copia de seguridad completa en JSON (spec §5.3). */
class BackupManager(private val db: AriseUpDatabase, private val settings: SettingsStore) {
    private val json = Json(AppJson) { prettyPrint = false }

    suspend fun export(out: OutputStream) {
        val f = db.foodDao()
        val r = db.routineDao()
        val tables = BackupTables(
            player = db.playerDao().all(), penalty = db.penaltyDao().all(), days = db.dayDao().all(),
            missions = db.missionDao().all(), exercises = r.exercises(), routines = r.allRoutines(),
            routineExercises = r.allRoutineExercises(), sets = r.allSets(),
            foods = f.all(), recipes = f.allRecipes(), recipeItems = f.allItems(), foodLogs = f.allLogs(),
            rules = f.allRules(), ruleLogs = f.allRuleLogs(), body = db.bodyDao().all(), tests = db.bodyDao().allTests(),
            rests = db.restDao().all(), achievements = db.achievementDao().all(), events = db.eventDao().all(),
            health = db.healthImportDao().all(),
        )
        val backup = Backup(SCHEMA_VERSION, System.currentTimeMillis(), settings.current(), tables)
        out.bufferedWriter(Charsets.UTF_8).use { it.write(json.encodeToString(Backup.serializer(), backup)) }
    }

    /** Valida todo el archivo antes de tocar nada; después sustituye los datos en una sola transacción. */
    suspend fun import(input: InputStream): ImportResult {
        val text = runCatching { input.bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull() ?: return ImportResult.InvalidFile
        val version = runCatching { json.parseToJsonElement(text).jsonObject["schemaVersion"]!!.jsonPrimitive.int }.getOrNull()
            ?: return ImportResult.InvalidFile
        if (version > SCHEMA_VERSION) return ImportResult.NewerSchema(version)
        val backup = runCatching { json.decodeFromString(Backup.serializer(), text) }.getOrNull() ?: return ImportResult.InvalidFile
        val t = backup.tables
        val b = db.backupDao()
        val restored = runCatching {
            db.withTransaction {
                b.clearHealth(); b.clearEvents(); b.clearAchievements(); b.clearRests(); b.clearTests(); b.clearBody()
                b.clearRuleLogs(); b.clearRules(); b.clearFoodLogs(); b.clearRecipeItems(); b.clearRecipes()
                b.clearFoods()
                b.clearSets(); b.clearRoutineExercises(); b.clearRoutines(); b.clearExercises(); b.clearMissions()
                b.clearDays(); b.clearPenalty(); b.clearPlayer()
                b.players(t.player); b.penalties(t.penalty); b.days(t.days); b.missions(t.missions); b.exercises(t.exercises)
                b.routines(t.routines); b.routineExercises(t.routineExercises); b.sets(t.sets); b.foods(t.foods)
                b.recipes(t.recipes); b.recipeItems(t.recipeItems); b.foodLogs(t.foodLogs); b.rules(t.rules)
                b.ruleLogs(t.ruleLogs); b.body(t.body); b.tests(t.tests); b.rests(t.rests); b.achievements(t.achievements)
                b.events(t.events); b.health(t.health)
            }
        }
        if (restored.isFailure) return ImportResult.InvalidFile
        settings.update { backup.settings }
        return ImportResult.Ok
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}
