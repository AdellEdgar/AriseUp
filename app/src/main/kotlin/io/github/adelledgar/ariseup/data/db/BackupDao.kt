package io.github.adelledgar.ariseup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Borrado e inserción masiva para importar copias de seguridad. */
@Dao
interface BackupDao {
    @Query("DELETE FROM player") suspend fun clearPlayer()
    @Query("DELETE FROM penalty") suspend fun clearPenalty()
    @Query("DELETE FROM day") suspend fun clearDays()
    @Query("DELETE FROM mission") suspend fun clearMissions()
    @Query("DELETE FROM exercise") suspend fun clearExercises()
    @Query("DELETE FROM routine") suspend fun clearRoutines()
    @Query("DELETE FROM routine_exercise") suspend fun clearRoutineExercises()
    @Query("DELETE FROM set_log") suspend fun clearSets()
    @Query("DELETE FROM food") suspend fun clearFoods()
    @Query("DELETE FROM recipe") suspend fun clearRecipes()
    @Query("DELETE FROM recipe_item") suspend fun clearRecipeItems()
    @Query("DELETE FROM food_log") suspend fun clearFoodLogs()
    @Query("DELETE FROM diet_rule") suspend fun clearRules()
    @Query("DELETE FROM diet_rule_log") suspend fun clearRuleLogs()
    @Query("DELETE FROM body_entry") suspend fun clearBody()
    @Query("DELETE FROM fitness_test") suspend fun clearTests()
    @Query("DELETE FROM rest_period") suspend fun clearRests()
    @Query("DELETE FROM achievement") suspend fun clearAchievements()
    @Query("DELETE FROM event") suspend fun clearEvents()
    @Query("DELETE FROM health_import") suspend fun clearHealth()

    @Insert suspend fun players(l: List<PlayerEntity>)
    @Insert suspend fun penalties(l: List<PenaltyEntity>)
    @Insert suspend fun days(l: List<DayEntity>)
    @Insert suspend fun missions(l: List<MissionEntity>)
    @Insert suspend fun exercises(l: List<ExerciseEntity>)
    @Insert suspend fun routines(l: List<RoutineEntity>)
    @Insert suspend fun routineExercises(l: List<RoutineExerciseEntity>)
    @Insert suspend fun sets(l: List<SetLogEntity>)
    @Insert suspend fun foods(l: List<FoodEntity>)
    @Insert suspend fun recipes(l: List<RecipeEntity>)
    @Insert suspend fun recipeItems(l: List<RecipeItemEntity>)
    @Insert suspend fun foodLogs(l: List<FoodLogEntity>)
    @Insert suspend fun rules(l: List<DietRuleEntity>)
    @Insert suspend fun ruleLogs(l: List<DietRuleLogEntity>)
    @Insert suspend fun body(l: List<BodyEntryEntity>)
    @Insert suspend fun tests(l: List<FitnessTestEntity>)
    @Insert suspend fun rests(l: List<RestPeriodEntity>)
    @Insert suspend fun achievements(l: List<AchievementEntity>)
    @Insert suspend fun events(l: List<EventEntity>)
    @Insert suspend fun health(l: List<HealthImportEntity>)
}
