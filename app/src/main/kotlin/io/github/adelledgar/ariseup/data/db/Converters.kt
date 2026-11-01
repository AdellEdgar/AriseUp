package io.github.adelledgar.ariseup.data.db

import androidx.room.TypeConverter
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.StatBlock
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** JSON compartido para columnas serializadas, eventos y copias de seguridad. */
val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Converters {
    @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun localDateTimeToString(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun stringToLocalDateTime(v: String?): LocalDateTime? = v?.let(LocalDateTime::parse)

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun statsToString(v: StatBlock?): String? = v?.let { AppJson.encodeToString(StatBlock.serializer(), it) }
    @TypeConverter fun stringToStats(v: String?): StatBlock? = v?.let { AppJson.decodeFromString(StatBlock.serializer(), it) }

    @TypeConverter fun targetsToString(v: DailyTargets?): String? = v?.let { AppJson.encodeToString(DailyTargets.serializer(), it) }
    @TypeConverter fun stringToTargets(v: String?): DailyTargets? = v?.let { AppJson.decodeFromString(DailyTargets.serializer(), it) }
}
