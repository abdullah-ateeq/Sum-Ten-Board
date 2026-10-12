package com.example.sumtenboard.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sumtenboard.data.AppSettings
import com.example.sumtenboard.data.GameStatistics
import com.example.sumtenboard.data.SettingsStore
import com.example.sumtenboard.data.StatisticsStore
import com.example.sumtenboard.game.logic.ReshuffleOutcome
import com.example.sumtenboard.game.logic.SelectionOutcome
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary
import com.example.sumtenboard.game.model.SumTenGameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot feedback the UI turns into sound, haptics or a snackbar. */
enum class GameEvent {
    SELECTED,
    VALID_PAIR,
    INVALID_PAIR,
    REMOVED,
    RESHUFFLED,
    BOARD_CLEARED,
    NO_MOVES,
    NO_POSSIBLE_PAIRS,
    SAME_CELL
}

/** How long removal / mismatch feedback stays on screen before the board settles. */
data class ResolveTiming(
    val validMillis: Long = 560,
    val invalidMillis: Long = 480,
    val reducedMotionMillis: Long = 160
)

class SumTenViewModel(
    private val settingsStore: SettingsStore,
    private val statisticsStore: StatisticsStore,
    private val engine: SumTenGameEngine = SumTenGameEngine(),
    private val timing: ResolveTiming = ResolveTiming()
) : ViewModel() {

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _statistics = MutableStateFlow(statisticsStore.load())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()

    private val _gameState = MutableStateFlow(engine.newGame())
    val gameState: StateFlow<SumTenGameState> = _gameState.asStateFlow()

    private val _lastSummary = MutableStateFlow<GameSummary?>(null)
    val lastSummary: StateFlow<GameSummary?> = _lastSummary.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var resolveJob: Job? = null
    private var gameRecorded = false

    fun startNewGame() {
        resolveJob?.cancel()
        resolveJob = null
        gameRecorded = false
        _gameState.value = engine.newGame()
    }

    /** Replaces the current game with a prepared one (used by tests and previews). */
    fun startGame(state: SumTenGameState) {
        resolveJob?.cancel()
        resolveJob = null
        gameRecorded = false
        _gameState.value = state
    }

    fun selectCell(cellId: Int) {
        val current = _gameState.value
        when (val outcome = engine.selectCell(current, cellId, _settings.value.showValidMatches)) {
            SelectionOutcome.Ignored -> Unit
            SelectionOutcome.SameCell -> _events.tryEmit(GameEvent.SAME_CELL)
            is SelectionOutcome.Selected -> {
                _gameState.value = outcome.state
                _events.tryEmit(GameEvent.SELECTED)
            }
            is SelectionOutcome.Resolved -> {
                _gameState.value = outcome.state
                _events.tryEmit(if (outcome.valid) GameEvent.VALID_PAIR else GameEvent.INVALID_PAIR)
                scheduleResolution(outcome.valid)
            }
        }
    }

    private fun scheduleResolution(valid: Boolean) {
        val wait = when {
            !_settings.value.animations -> timing.reducedMotionMillis
            valid -> timing.validMillis
            else -> timing.invalidMillis
        }
        resolveJob = viewModelScope.launch {
            delay(wait)
            val before = _gameState.value
            val after = engine.finishResolution(before, _settings.value.autoDetectNoMoves)
            _gameState.value = after
            if (valid) _events.tryEmit(GameEvent.REMOVED)
            when (after.phase) {
                GamePhase.COMPLETED -> onGameFinished(after)
                GamePhase.NO_MOVES -> _events.tryEmit(GameEvent.NO_MOVES)
                else -> Unit
            }
        }
    }

    fun reshuffle() {
        when (val outcome = engine.reshuffle(_gameState.value)) {
            ReshuffleOutcome.NotAvailable -> Unit
            is ReshuffleOutcome.Shuffled -> {
                _gameState.value = outcome.state
                _events.tryEmit(GameEvent.RESHUFFLED)
            }
            is ReshuffleOutcome.NoPossiblePairs -> {
                _gameState.value = outcome.state
                _events.tryEmit(GameEvent.NO_POSSIBLE_PAIRS)
            }
        }
    }

    fun endGame() {
        if (_gameState.value.phase == GamePhase.RESOLVING) return
        val ended = engine.endGame(_gameState.value)
        _gameState.value = ended
        onGameFinished(ended)
    }

    /** Counts the current run in statistics (if it had started) and deals a fresh board. */
    fun recordAndStartNewGame() {
        val current = _gameState.value
        if (current.phase != GamePhase.RESOLVING && current.moves > 0) {
            onGameFinished(engine.endGame(current))
        }
        startNewGame()
    }

    private fun onGameFinished(state: SumTenGameState) {
        if (gameRecorded) return
        val summary = engine.summary(state) ?: return
        gameRecorded = true
        _lastSummary.value = summary
        _statistics.value = statisticsStore.record(summary)
        if (summary.result == GameResult.BOARD_CLEARED) _events.tryEmit(GameEvent.BOARD_CLEARED)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        settingsStore.save(updated)
        // Reflect the hint setting immediately if a tile is currently selected.
        _gameState.update { engine.withHighlights(it, updated.showValidMatches) }
    }

    fun resetSettings() {
        _settings.value = settingsStore.reset()
        _gameState.update { engine.withHighlights(it, _settings.value.showValidMatches) }
    }

    fun resetStatistics() {
        _statistics.value = statisticsStore.reset()
    }

    class Factory(
        private val settingsStore: SettingsStore,
        private val statisticsStore: StatisticsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SumTenViewModel(settingsStore, statisticsStore) as T
    }
}
