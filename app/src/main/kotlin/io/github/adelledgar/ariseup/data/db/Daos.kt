package io.github.adelledgar.ariseup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import io.github.adelledgar.ariseup.domain.model.MissionKind
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1") suspend fun get(): PlayerEntity?
    @Query("SELECT * FROM player WHERE id = 1") fun observe(): Flow<PlayerEntity?>
    @Upsert suspend fun upsert(p: PlayerEntity)
    @Query("SELECT COUNT(*) FROM player") suspend fun count(): Int
    @Query("SELECT * FROM player") suspend fun all(): List<PlayerEntity>
    @Query("DELETE FROM player") suspend fun clear()
}

@Dao
interface PenaltyDao {
    @Query("SELECT * FROM penalty WHERE id = 1") suspend fun get(): PenaltyEntity?
    @Query("SELECT * FROM penalty WHERE id = 1") fun observe(): Flow<PenaltyEntity?>
    @Upsert suspend fun upsert(p: PenaltyEntity)
    @Query("DELETE FROM penalty") suspend fun clear()
    @Query("SELECT * FROM penalty") suspend fun all(): List<PenaltyEntity>
}

@Dao
interface DayDao {
    @Query("SELECT * FROM day WHERE date = :date") suspend fun get(date: LocalDate): DayEntity?
    @Query("SELECT * FROM day WHERE date = :date") fun observe(date: LocalDate): Flow<DayEntity?>
    @Upsert suspend fun upsert(d: DayEntity)
    @Query("SELECT * FROM day WHERE date < :before AND status = :status ORDER BY date")
    suspend fun withStatusBefore(before: LocalDate, status: DayStatus): List<DayEntity>
    @Query("SELECT * FROM day WHERE status = :status ORDER BY date") suspend fun withStatus(status: DayStatus): List<DayEntity>
    @Query("SELECT * FROM day ORDER BY date") fun observeAll(): Flow<List<DayEntity>>
    @Query("SELECT MAX(date) FROM day") suspend fun lastDate(): LocalDate?
    @Query("SELECT * FROM day") suspend fun all(): List<DayEntity>
    @Query("DELETE FROM day") suspend fun clear()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM mission WHERE id = :id") suspend fun get(id: Long): MissionEntity?
    @Query("SELECT * FROM mission WHERE date = :date ORDER BY id") suspend fun forDate(date: LocalDate): List<MissionEntity>
    @Query("SELECT * FROM mission WHERE date = :date ORDER BY id") fun observeForDate(date: LocalDate): Flow<List<MissionEntity>>
    @Query("SELECT * FROM mission WHERE date = :date AND kind = :kind") suspend fun forDateAndKind(date: LocalDate, kind: MissionKind): List<MissionEntity>
    @Query("SELECT * FROM mission WHERE kind = :kind AND completedAt IS NULL ORDER BY date DESC LIMIT 1")
    suspend fun openOfKind(kind: MissionKind): MissionEntity?
    @Query("SELECT COUNT(*) FROM mission WHERE kind = 'URGENT' AND date BETWEEN :from AND :to")
    suspend fun urgentCount(from: LocalDate, to: LocalDate): Int
    @Query("SELECT * FROM mission WHERE kind = :kind ORDER BY date") suspend fun allOfKind(kind: MissionKind): List<MissionEntity>
    @Insert suspend fun insert(m: MissionEntity): Long
    @Upsert suspend fun upsert(m: MissionEntity)
    @Query("UPDATE mission SET progress = :progress WHERE id = :id") suspend fun setProgress(id: Long, progress: String)
    @Query("UPDATE mission SET completedAt = :at WHERE id = :id AND completedAt IS NULL") suspend fun markCompleted(id: Long, at: Instant): Int
    @Query("DELETE FROM mission WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM mission") suspend fun all(): List<MissionEntity>
    @Query("DELETE FROM mission") suspend fun clear()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine WHERE archived = 0 ORDER BY name") fun observeActive(): Flow<List<RoutineEntity>>
    @Query("SELECT * FROM routine WHERE archived = 0") suspend fun active(): List<RoutineEntity>
    @Query("SELECT * FROM routine WHERE id = :id") suspend fun get(id: Long): RoutineEntity?
    @Insert suspend fun insert(r: RoutineEntity): Long
    @Upsert suspend fun upsert(r: RoutineEntity)
    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY position") suspend fun exercisesOf(routineId: Long): List<RoutineExerciseEntity>
    @Insert suspend fun insertExercise(e: RoutineExerciseEntity): Long
    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId") suspend fun clearExercises(routineId: Long)
    @Query("SELECT * FROM exercise ORDER BY name") fun observeExercises(): Flow<List<ExerciseEntity>>
    @Query("SELECT * FROM exercise ORDER BY name") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM exercise WHERE id = :id") suspend fun exercise(id: Long): ExerciseEntity?
    @Insert suspend fun insertExerciseDef(e: ExerciseEntity): Long
    @Insert suspend fun insertExerciseDefs(e: List<ExerciseEntity>)
    @Query("SELECT COUNT(*) FROM exercise") suspend fun exerciseCount(): Int
    @Query("SELECT * FROM set_log WHERE missionId = :missionId ORDER BY routineExerciseId, setIndex") suspend fun setsOf(missionId: Long): List<SetLogEntity>
    @Query("SELECT * FROM set_log WHERE missionId = :missionId ORDER BY routineExerciseId, setIndex") fun observeSets(missionId: Long): Flow<List<SetLogEntity>>
    @Upsert suspend fun upsertSet(s: SetLogEntity): Long
    @Query("SELECT * FROM set_log WHERE missionId = :missionId AND routineExerciseId = :reId AND setIndex = :setIndex")
    suspend fun findSet(missionId: Long, reId: Long, setIndex: Int): SetLogEntity?
    @Query("SELECT * FROM routine") suspend fun allRoutines(): List<RoutineEntity>
    @Query("SELECT * FROM routine_exercise") suspend fun allRoutineExercises(): List<RoutineExerciseEntity>
    @Query("SELECT * FROM set_log") suspend fun allSets(): List<SetLogEntity>
}

@Dao
interface FoodDao {
    @Insert suspend fun insert(f: FoodEntity): Long
    @Insert suspend fun insertAll(f: List<FoodEntity>)
    @Upsert suspend fun upsert(f: FoodEntity): Long
    @Query("SELECT * FROM food WHERE id = :id") suspend fun get(id: Long): FoodEntity?
    @Query("SELECT * FROM food WHERE barcode = :code") suspend fun byBarcode(code: String): FoodEntity?
    @Query(
        "SELECT * FROM food WHERE searchName LIKE '%' || :q || '%' " +
            "ORDER BY favorite DESC, (lastUsedAt IS NULL), lastUsedAt DESC, (source = 'GENERIC') DESC, name LIMIT :limit",
    )
    suspend fun search(q: String, limit: Int): List<FoodEntity>
    @Query("SELECT COUNT(*) FROM food WHERE source = :source") suspend fun countBySource(source: FoodSource): Int
    @Query("UPDATE food SET lastUsedAt = :at WHERE id = :id") suspend fun touch(id: Long, at: Instant)
    @Query("UPDATE food SET favorite = :fav WHERE id = :id") suspend fun setFavorite(id: Long, fav: Boolean)
    @Query("SELECT * FROM food WHERE source != 'GENERIC'") suspend fun allNonGeneric(): List<FoodEntity>
    @Query("SELECT * FROM food") suspend fun all(): List<FoodEntity>

    @Insert suspend fun insertRecipe(r: RecipeEntity): Long
    @Upsert suspend fun upsertRecipe(r: RecipeEntity)
    @Query("SELECT * FROM recipe WHERE id = :id") suspend fun recipe(id: Long): RecipeEntity?
    @Query("SELECT * FROM recipe ORDER BY name") fun observeRecipes(): Flow<List<RecipeEntity>>
    @Query("SELECT * FROM recipe WHERE searchName LIKE '%' || :q || '%' ORDER BY name") suspend fun searchRecipes(q: String): List<RecipeEntity>
    @Insert suspend fun insertItems(items: List<RecipeItemEntity>)
    @Query("DELETE FROM recipe_item WHERE recipeId = :recipeId") suspend fun clearItems(recipeId: Long)
    @Query("SELECT * FROM recipe_item WHERE recipeId = :recipeId") suspend fun items(recipeId: Long): List<RecipeItemEntity>
    @Query("SELECT COUNT(*) FROM recipe") suspend fun recipeCount(): Int
    @Query("SELECT * FROM recipe") suspend fun allRecipes(): List<RecipeEntity>
    @Query("SELECT * FROM recipe_item") suspend fun allItems(): List<RecipeItemEntity>

    @Insert suspend fun insertLog(l: FoodLogEntity): Long
    @Query("DELETE FROM food_log WHERE id = :id") suspend fun deleteLog(id: Long)
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY id") suspend fun logsFor(date: LocalDate): List<FoodLogEntity>
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY id") fun observeLogs(date: LocalDate): Flow<List<FoodLogEntity>>
    @Query("SELECT * FROM food_log") suspend fun allLogs(): List<FoodLogEntity>

    @Query("SELECT * FROM diet_rule WHERE archived = 0 ORDER BY id") suspend fun activeRules(): List<DietRuleEntity>
    @Query("SELECT * FROM diet_rule WHERE archived = 0 ORDER BY id") fun observeRules(): Flow<List<DietRuleEntity>>
    @Query("SELECT * FROM diet_rule WHERE id = :id") suspend fun rule(id: Long): DietRuleEntity?
    @Upsert suspend fun upsertRule(r: DietRuleEntity): Long
    @Upsert suspend fun upsertRuleLog(l: DietRuleLogEntity)
    @Query("SELECT * FROM diet_rule_log WHERE date = :date") suspend fun ruleLogs(date: LocalDate): List<DietRuleLogEntity>
    @Query("SELECT * FROM diet_rule_log WHERE date = :date") fun observeRuleLogs(date: LocalDate): Flow<List<DietRuleLogEntity>>
    @Query("SELECT * FROM diet_rule") suspend fun allRules(): List<DietRuleEntity>
    @Query("SELECT * FROM diet_rule_log") suspend fun allRuleLogs(): List<DietRuleLogEntity>
}

@Dao
interface BodyDao {
    @Insert suspend fun insert(e: BodyEntryEntity): Long
    @Query("SELECT * FROM body_entry WHERE weightKg IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1") suspend fun latest(): BodyEntryEntity?
    @Query("SELECT * FROM body_entry WHERE weightKg IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1") fun observeLatest(): Flow<BodyEntryEntity?>
    @Query("SELECT * FROM body_entry WHERE weightKg IS NOT NULL ORDER BY date, id LIMIT 1") suspend fun first(): BodyEntryEntity?
    @Query("SELECT * FROM body_entry WHERE date = :date LIMIT 1") suspend fun on(date: LocalDate): BodyEntryEntity?
    @Query("SELECT * FROM body_entry ORDER BY date, id") fun observeAll(): Flow<List<BodyEntryEntity>>
    @Query("SELECT * FROM body_entry") suspend fun all(): List<BodyEntryEntity>

    @Insert suspend fun insertTest(t: FitnessTestEntity): Long
    @Upsert suspend fun upsertTest(t: FitnessTestEntity)
    @Query("SELECT * FROM fitness_test ORDER BY date DESC, id DESC LIMIT 1") suspend fun latestTest(): FitnessTestEntity?
    @Query("SELECT * FROM fitness_test ORDER BY date, id") fun observeTests(): Flow<List<FitnessTestEntity>>
    @Query("SELECT * FROM fitness_test") suspend fun allTests(): List<FitnessTestEntity>
}

@Dao
interface RestDao {
    @Query("SELECT * FROM rest_period ORDER BY start") suspend fun all(): List<RestPeriodEntity>
    @Query("SELECT * FROM rest_period ORDER BY start") fun observeAll(): Flow<List<RestPeriodEntity>>
    @Insert suspend fun insert(r: RestPeriodEntity): Long
    @Upsert suspend fun upsert(r: RestPeriodEntity)
}

@Dao
interface EventDao {
    @Insert suspend fun insertAll(e: List<EventEntity>)
    @Query("SELECT * FROM event ORDER BY at DESC, id DESC LIMIT :limit") fun observeRecent(limit: Int): Flow<List<EventEntity>>
    @Query("SELECT * FROM event ORDER BY at, id") suspend fun all(): List<EventEntity>
    @Query("SELECT * FROM event WHERE type = :type ORDER BY at, id") suspend fun ofType(type: String): List<EventEntity>
    @Query("SELECT COUNT(*) FROM event") suspend fun count(): Int
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievement") suspend fun all(): List<AchievementEntity>
    @Query("SELECT * FROM achievement") fun observeAll(): Flow<List<AchievementEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(a: AchievementEntity)
}

@Dao
interface HealthImportDao {
    @Query("SELECT EXISTS(SELECT 1 FROM health_import WHERE recordId = :id)") suspend fun exists(id: String): Boolean
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(h: HealthImportEntity)
    @Query("SELECT * FROM health_import") suspend fun all(): List<HealthImportEntity>
}
