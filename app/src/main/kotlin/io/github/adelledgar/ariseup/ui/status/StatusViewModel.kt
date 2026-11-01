package io.github.adelledgar.ariseup.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.domain.BodyShape
import io.github.adelledgar.ariseup.domain.BodyShapeModel
import io.github.adelledgar.ariseup.domain.Leveling
import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.StatBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class StatusUi(
    val name: String,
    val title: String?,
    val titles: List<String>,
    val level: Int,
    val rank: Rank,
    val xp: Long,
    val xpToNext: Long,
    val stats: StatBlock,
    val freePoints: Int,
    val streak: Int,
    val penalty: Boolean,
    val penaltyDeadline: Instant?,
    val rest: Boolean,
    val shape: BodyShape,
    val ghost: BodyShape?,
    val sex: Sex,
    val weightKg: Double?,
    val bmi: Double?,
    val bodyFatPct: Double?,
    val bodyFatEstimated: Boolean,
    val weightDelta: Double?,
)

class StatusViewModel(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val body: BodyRepository,
    private val gameClock: GameClock,
) : ViewModel() {
    /** Flujo frío del estado (los tests lo leen directamente). */
    val uiFlow: Flow<StatusUi?> = combine(
        players.entity(),
        db.penaltyDao().observe(),
        body.observeAll(),
        db.restDao().observeAll(),
        db.achievementDao().observeAll(),
    ) { p, penalty, entries, rests, achievements ->
        if (p == null) return@combine null
        val today = gameClock.today()
        val age = p.profile().age(today)
        val weighed = entries.filter { it.weightKg != null }
        val latest = weighed.lastOrNull()
        val first = weighed.firstOrNull()
        val fatEntry = entries.lastOrNull { it.bodyFatPct != null }?.bodyFatPct
        val weight = latest?.weightKg ?: 70.0
        val fue = p.stats[Stat.FUE]
        val shape = BodyShapeModel.shape(p.sex, age, p.heightCm, weight, fatEntry, fue)
        val ghost = if (first != null && latest != null && first.id != latest.id) {
            BodyShapeModel.shape(p.sex, age, p.heightCm, first.weightKg!!, first.bodyFatPct, fue)
        } else {
            null
        }
        val bmi = latest?.weightKg?.let { BodyShapeModel.bmi(it, p.heightCm) }
        val titles = achievements.mapNotNull { a -> Achievement.entries.firstOrNull { it.name == a.id }?.title }
        StatusUi(
            name = p.name,
            title = p.titleId,
            titles = titles,
            level = p.level,
            rank = Leveling.rankOf(p.level),
            xp = p.xp,
            xpToNext = Leveling.xpToNext(p.level),
            stats = p.stats,
            freePoints = p.freePoints,
            streak = p.streak,
            penalty = penalty != null,
            penaltyDeadline = penalty?.let { gameClock.startOf(maxOf(today, it.firstDegradeOnCloseOf).plusDays(1)) },
            rest = rests.any { it.toDomain().covers(today) },
            shape = shape,
            ghost = ghost,
            sex = p.sex,
            weightKg = latest?.weightKg,
            bmi = bmi,
            bodyFatPct = fatEntry ?: bmi?.let { BodyShapeModel.estimatedBodyFat(it, age, p.sex) },
            bodyFatEstimated = fatEntry == null,
            weightDelta = if (latest != null && first != null) latest.weightKg!! - first.weightKg!! else null,
        )
    }

    val ui: StateFlow<StatusUi?> = uiFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun spend(stat: Stat) {
        viewModelScope.launch { runCatching { players.spendFreePoint(stat) } }
    }

    fun chooseTitle(title: String?) {
        viewModelScope.launch {
            players.currentEntity()?.let { players.saveEntity(it.copy(titleId = title)) }
        }
    }

}
