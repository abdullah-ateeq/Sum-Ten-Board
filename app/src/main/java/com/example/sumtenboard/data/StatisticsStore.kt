package com.example.sumtenboard.data

import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary

data class GameStatistics(
    val gamesPlayed: Int = 0,
    val boardsCleared: Int = 0,
    val incompleteGames: Int = 0,
    val bestScore: Int = 0,
    val bestClear: Int = 0,
    /** Best accuracy among cleared boards, or -1 when no board has been cleared yet. */
    val bestAccuracy: Int = NO_ACCURACY,
    val totalPairsRemoved: Int = 0,
    val totalFailedAttempts: Int = 0,
    val totalReshuffles: Int = 0,
    val perfectClears: Int = 0
) {
    fun record(summary: GameSummary): GameStatistics {
        val cleared = summary.result == GameResult.BOARD_CLEARED
        val accuracy = summary.accuracyPercent
        return copy(
            gamesPlayed = gamesPlayed + 1,
            boardsCleared = boardsCleared + if (cleared) 1 else 0,
            incompleteGames = incompleteGames + if (cleared) 0 else 1,
            bestScore = maxOf(bestScore, summary.score),
            bestClear = maxOf(bestClear, summary.tilesCleared),
            bestAccuracy = if (cleared && accuracy != null) maxOf(bestAccuracy, accuracy) else bestAccuracy,
            totalPairsRemoved = totalPairsRemoved + summary.pairsRemoved,
            totalFailedAttempts = totalFailedAttempts + summary.failedAttempts,
            totalReshuffles = totalReshuffles + summary.reshufflesUsed,
            perfectClears = perfectClears + if (summary.isPerfectClear) 1 else 0
        )
    }

    companion object {
        const val NO_ACCURACY = -1
    }
}

class StatisticsStore(private val store: KeyValueStore) {

    fun load() = GameStatistics(
        gamesPlayed = store.getInt(KEY_GAMES, 0),
        boardsCleared = store.getInt(KEY_CLEARED, 0),
        incompleteGames = store.getInt(KEY_INCOMPLETE, 0),
        bestScore = store.getInt(KEY_BEST_SCORE, 0),
        bestClear = store.getInt(KEY_BEST_CLEAR, 0),
        bestAccuracy = store.getInt(KEY_BEST_ACCURACY, GameStatistics.NO_ACCURACY),
        totalPairsRemoved = store.getInt(KEY_TOTAL_PAIRS, 0),
        totalFailedAttempts = store.getInt(KEY_TOTAL_FAILED, 0),
        totalReshuffles = store.getInt(KEY_TOTAL_RESHUFFLES, 0),
        perfectClears = store.getInt(KEY_PERFECT, 0)
    )

    fun save(stats: GameStatistics) {
        store.edit {
            putInt(KEY_GAMES, stats.gamesPlayed)
            putInt(KEY_CLEARED, stats.boardsCleared)
            putInt(KEY_INCOMPLETE, stats.incompleteGames)
            putInt(KEY_BEST_SCORE, stats.bestScore)
            putInt(KEY_BEST_CLEAR, stats.bestClear)
            putInt(KEY_BEST_ACCURACY, stats.bestAccuracy)
            putInt(KEY_TOTAL_PAIRS, stats.totalPairsRemoved)
            putInt(KEY_TOTAL_FAILED, stats.totalFailedAttempts)
            putInt(KEY_TOTAL_RESHUFFLES, stats.totalReshuffles)
            putInt(KEY_PERFECT, stats.perfectClears)
        }
    }

    fun record(summary: GameSummary): GameStatistics = load().record(summary).also(::save)

    fun reset(): GameStatistics {
        store.remove(ALL_KEYS)
        return GameStatistics()
    }

    companion object {
        const val PREFS_NAME = "sum_ten_statistics"
        private const val KEY_GAMES = "games_played"
        private const val KEY_CLEARED = "boards_cleared"
        private const val KEY_INCOMPLETE = "incomplete_games"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_BEST_CLEAR = "best_clear"
        private const val KEY_BEST_ACCURACY = "best_accuracy"
        private const val KEY_TOTAL_PAIRS = "total_pairs"
        private const val KEY_TOTAL_FAILED = "total_failed"
        private const val KEY_TOTAL_RESHUFFLES = "total_reshuffles"
        private const val KEY_PERFECT = "perfect_clears"
        private val ALL_KEYS = listOf(
            KEY_GAMES, KEY_CLEARED, KEY_INCOMPLETE, KEY_BEST_SCORE, KEY_BEST_CLEAR, KEY_BEST_ACCURACY,
            KEY_TOTAL_PAIRS, KEY_TOTAL_FAILED, KEY_TOTAL_RESHUFFLES, KEY_PERFECT
        )
    }
}
