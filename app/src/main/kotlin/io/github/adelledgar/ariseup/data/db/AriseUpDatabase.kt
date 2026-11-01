package io.github.adelledgar.ariseup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PlayerEntity::class, PenaltyEntity::class, DayEntity::class, MissionEntity::class,
        ExerciseEntity::class, RoutineEntity::class, RoutineExerciseEntity::class, SetLogEntity::class,
        FoodEntity::class, RecipeEntity::class, RecipeItemEntity::class, FoodLogEntity::class,
        DietRuleEntity::class, DietRuleLogEntity::class, BodyEntryEntity::class, FitnessTestEntity::class,
        RestPeriodEntity::class, AchievementEntity::class, EventEntity::class, HealthImportEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AriseUpDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun penaltyDao(): PenaltyDao
    abstract fun dayDao(): DayDao
    abstract fun missionDao(): MissionDao
    abstract fun routineDao(): RoutineDao
    abstract fun foodDao(): FoodDao
    abstract fun bodyDao(): BodyDao
    abstract fun restDao(): RestDao
    abstract fun eventDao(): EventDao
    abstract fun achievementDao(): AchievementDao
    abstract fun healthImportDao(): HealthImportDao

    companion object {
        const val NAME = "ariseup.db"
    }
}
