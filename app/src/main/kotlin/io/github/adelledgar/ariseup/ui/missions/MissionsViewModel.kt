package io.github.adelledgar.ariseup.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.MissionEntity
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.MissionRepository
import io.github.adelledgar.ariseup.data.repo.ReevaluationService
import io.github.adelledgar.ariseup.data.repo.RoutineRepository
import io.github.adelledgar.ariseup.data.repo.RoutineWithExercises
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class MissionsUi(
    val today: LocalDate,
    val penalty: MissionEntity?,
    val missions: List<MissionEntity>,
    val routines: Map<Long, RoutineWithExercises>,
    val now: Instant,
)

/** Mensaje del Sistema que la pantalla muestra en una ventana. */
data class SystemMessage(val title: String, val lines: List<String>, val levelUp: Boolean = false, val error: Boolean = false)

class MissionsViewModel(
    private val db: AriseUpDatabase,
    private val missions: MissionRepository,
    private val routines: RoutineRepository,
    private val body: BodyRepository,
    private val reevaluation: ReevaluationService,
    private val gameClock: GameClock,
) : ViewModel() {
    private val day = MutableStateFlow<LocalDate?>(null)
    private val _messages = MutableSharedFlow<SystemMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<SystemMessage> = _messages

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<MissionsUi?> = flow { emit(gameClock.today()) }
        .flatMapLatest { today ->
            day.value = today
            combine(missions.observe(today), db.penaltyDao().observe()) { list, _ ->
                val penalty = db.missionDao().openOfKind(MissionKind.PENALTY)
                val routineMap = list.filter { it.kind == MissionKind.ROUTINE && it.refId != null }
                    .mapNotNull { m -> routines.withExercises(m.refId!!)?.let { m.refId to it } }.toMap()
                MissionsUi(today, penalty, list.filter { it.kind != MissionKind.PENALTY }, routineMap, gameClock.now())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun sets(missionId: Long): Flow<List<io.github.adelledgar.ariseup.data.db.SetLogEntity>> = routines.observeSets(missionId)

    suspend fun addDailySync(missionId: Long, exercise: DailyExercise, amount: Double, minutes: Double = 0.0): List<SystemEvent> =
        missions.addDailyProgress(missionId, exercise, amount, minutes)

    suspend fun weighInSync(missionId: Long?, weightKg: Double, waistCm: Double?, bodyFatPct: Double?): List<SystemEvent> {
        body.add(BodyEntryEntity(date = gameClock.today(), weightKg = weightKg, waistCm = waistCm, bodyFatPct = bodyFatPct))
        return missionId?.let { missions.complete(it) } ?: emptyList()
    }

    suspend fun reevaluateSync(missionId: Long?, test: FitnessTest): List<SystemEvent> = reevaluation.apply(missionId, test)

    fun addDaily(missionId: Long, exercise: DailyExercise, amount: Double, minutes: Double = 0.0) =
        run { addDailySync(missionId, exercise, amount, minutes) }

    fun logSet(missionId: Long, reId: Long, setIndex: Int, reps: Int?, weightKg: Double?, seconds: Int?, meters: Double?) =
        run { missions.logSet(missionId, reId, setIndex, reps, weightKg, seconds, meters) }

    fun weighIn(missionId: Long?, weightKg: Double, waistCm: Double?, bodyFatPct: Double?) =
        run { weighInSync(missionId, weightKg, waistCm, bodyFatPct) }

    fun reevaluate(missionId: Long?, test: FitnessTest) = run { reevaluateSync(missionId, test) }

    private fun run(block: suspend () -> List<SystemEvent>) {
        viewModelScope.launch {
            try {
                messagesFor(block()).forEach { _messages.emit(it) }
            } catch (e: IllegalStateException) {
                _messages.emit(SystemMessage("Aviso", listOf(e.message ?: "[Error]"), error = true))
            }
        }
    }

    companion object {
        /** Traduce los eventos a ventanas: compleción con XP y, aparte, subida de nivel o rango. */
        fun messagesFor(events: List<SystemEvent>): List<SystemMessage> {
            if (events.isEmpty()) return emptyList()
            val out = mutableListOf<SystemMessage>()
            val xp = events.filterIsInstance<SystemEvent.XpGained>()
            val stats = events.filterIsInstance<SystemEvent.StatsChanged>().flatMap { it.delta.entries }
            if (xp.isNotEmpty() || events.any { it is SystemEvent.PenaltyCleared }) {
                val lines = buildList {
                    xp.forEach { add("+${it.amount} XP") }
                    stats.groupBy { it.key }.forEach { (stat, v) ->
                        val sum = v.sumOf { it.value }
                        add("${stat.label} ${if (sum >= 0) "+" else ""}${"%.2f".format(java.util.Locale("es"), sum).trimEnd('0').trimEnd(',')}")
                    }
                    if (events.any { it is SystemEvent.PenaltyCleared }) add("Has salido de la Zona de Penalización.")
                }
                out += SystemMessage("Misión completada", lines)
            } else if (stats.isNotEmpty()) {
                out += SystemMessage("Reevaluación completada", stats.map { (s, v) -> "${s.label} ${if (v >= 0) "+" else ""}${v.toInt()}" })
            }
            events.filterIsInstance<SystemEvent.LevelUp>().lastOrNull()?.let { up ->
                val rank = events.filterIsInstance<SystemEvent.RankChanged>().lastOrNull()
                out += SystemMessage(
                    "Ha subido de nivel",
                    listOfNotNull("Nivel ${up.newLevel}", rank?.let { "Nuevo rango: ${it.to}" }, "Tienes puntos de atributo para repartir."),
                    levelUp = true,
                )
            }
            events.filterIsInstance<SystemEvent.AchievementUnlocked>().forEach {
                out += SystemMessage("Misión oculta completada", listOf(it.id))
            }
            return out
        }
    }
}
