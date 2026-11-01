package io.github.adelledgar.ariseup.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.domain.model.TargetOverrides
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

enum class WeighInFrequency(val label: String) { DAILY("Cada día"), EVERY_2_DAYS("Cada 2 días"), OFF("Desactivado") }

enum class NutritionMode(val label: String) { MANDATORY("Obligatoria"), OPTIONAL("Opcional"), OFF("Desactivada") }

@Serializable
data class Settings(
    val resetMinutes: Int = 0,
    val weighInFrequency: WeighInFrequency = WeighInFrequency.DAILY,
    val weighInMinutes: Int = 8 * 60,
    val nutritionMode: NutritionMode = NutritionMode.MANDATORY,
    val restTimerSeconds: Int = 90,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val targetOverrides: TargetOverrides = TargetOverrides.NONE,
) {
    val resetTime: LocalTime get() = LocalTime.of(resetMinutes / 60, resetMinutes % 60)
    val weighInTime: LocalTime get() = LocalTime.of(weighInMinutes / 60, weighInMinutes % 60)
}

/** Preferencias del usuario (DataStore). */
class SettingsStore(context: Context, fileName: String = "settings") {
    private val store: DataStore<Preferences> = stores.getOrPut(fileName) {
        PreferenceDataStoreFactory.create { context.applicationContext.preferencesDataStoreFile(fileName) }
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            resetMinutes = p[RESET] ?: 0,
            weighInFrequency = p[WEIGH_FREQ]?.let { runCatching { WeighInFrequency.valueOf(it) }.getOrNull() } ?: WeighInFrequency.DAILY,
            weighInMinutes = p[WEIGH_TIME] ?: (8 * 60),
            nutritionMode = p[NUTRITION]?.let { runCatching { NutritionMode.valueOf(it) }.getOrNull() } ?: NutritionMode.MANDATORY,
            restTimerSeconds = p[REST_TIMER] ?: 90,
            sound = p[SOUND] ?: true,
            vibration = p[VIBRATION] ?: true,
            targetOverrides = p[OVERRIDES]?.let { runCatching { AppJson.decodeFromString(TargetOverrides.serializer(), it) }.getOrNull() }
                ?: TargetOverrides.NONE,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { p ->
            val old = Settings(
                resetMinutes = p[RESET] ?: 0,
                weighInFrequency = p[WEIGH_FREQ]?.let { runCatching { WeighInFrequency.valueOf(it) }.getOrNull() } ?: WeighInFrequency.DAILY,
                weighInMinutes = p[WEIGH_TIME] ?: (8 * 60),
                nutritionMode = p[NUTRITION]?.let { runCatching { NutritionMode.valueOf(it) }.getOrNull() } ?: NutritionMode.MANDATORY,
                restTimerSeconds = p[REST_TIMER] ?: 90,
                sound = p[SOUND] ?: true,
                vibration = p[VIBRATION] ?: true,
                targetOverrides = p[OVERRIDES]?.let { runCatching { AppJson.decodeFromString(TargetOverrides.serializer(), it) }.getOrNull() }
                    ?: TargetOverrides.NONE,
            )
            val s = transform(old)
            p[RESET] = s.resetMinutes
            p[WEIGH_FREQ] = s.weighInFrequency.name
            p[WEIGH_TIME] = s.weighInMinutes
            p[NUTRITION] = s.nutritionMode.name
            p[REST_TIMER] = s.restTimerSeconds
            p[SOUND] = s.sound
            p[VIBRATION] = s.vibration
            p[OVERRIDES] = AppJson.encodeToString(TargetOverrides.serializer(), s.targetOverrides)
        }
    }

    private companion object {
        val stores = ConcurrentHashMap<String, DataStore<Preferences>>()
        val RESET = intPreferencesKey("reset_minutes")
        val WEIGH_FREQ = stringPreferencesKey("weigh_in_frequency")
        val WEIGH_TIME = intPreferencesKey("weigh_in_minutes")
        val NUTRITION = stringPreferencesKey("nutrition_mode")
        val REST_TIMER = intPreferencesKey("rest_timer_seconds")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val OVERRIDES = stringPreferencesKey("target_overrides")
    }
}
