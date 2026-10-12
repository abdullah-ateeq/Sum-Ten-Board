package com.example.sumtenboard.data

import com.example.sumtenboard.InMemoryStore
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistenceTest {

    @Test
    fun `settings default values`() {
        val settings = SettingsStore(InMemoryStore()).load()
        assertEquals(AppSettings(), settings)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(true, settings.showValidMatches)
        assertEquals(false, settings.showCoordinates)
        assertEquals(true, settings.autoDetectNoMoves)
    }

    @Test
    fun `settings round trip through storage`() {
        val backing = InMemoryStore()
        val changed = AppSettings(
            themeMode = ThemeMode.DARK, dynamicColor = false, sound = false, haptics = false,
            animations = false, confirmRestart = false, showValidMatches = false,
            showCoordinates = true, autoDetectNoMoves = false
        )
        SettingsStore(backing).save(changed)
        assertEquals(changed, SettingsStore(backing).load())
    }

    @Test
    fun `reset settings restores defaults`() {
        val backing = InMemoryStore()
        SettingsStore(backing).save(AppSettings(themeMode = ThemeMode.LIGHT, sound = false))
        assertEquals(AppSettings(), SettingsStore(backing).reset())
        assertEquals(AppSettings(), SettingsStore(backing).load())
    }

    private fun summary(
        result: GameResult, tiles: Int, pairs: Int, score: Int, moves: Int, failed: Int, reshuffles: Int
    ) = GameSummary(result, tiles, pairs, score, moves, failed, reshuffles)

    @Test
    fun `statistics accumulate across games`() {
        val backing = InMemoryStore()
        val store = StatisticsStore(backing)
        store.record(summary(GameResult.BOARD_CLEARED, 36, 18, 234, 18, 0, 0))
        store.record(summary(GameResult.NO_MOVES, 28, 14, 168, 17, 3, 2))
        store.record(summary(GameResult.PLAYER_ENDED, 20, 10, 108, 12, 2, 0))

        val stats = StatisticsStore(backing).load()
        assertEquals(3, stats.gamesPlayed)
        assertEquals(1, stats.boardsCleared)
        assertEquals(2, stats.incompleteGames)
        assertEquals(234, stats.bestScore)
        assertEquals(36, stats.bestClear)
        assertEquals(100, stats.bestAccuracy)
        assertEquals(42, stats.totalPairsRemoved)
        assertEquals(5, stats.totalFailedAttempts)
        assertEquals(2, stats.totalReshuffles)
        assertEquals(1, stats.perfectClears)
    }

    @Test
    fun `best accuracy only counts completed boards`() {
        val stats = GameStatistics()
            .record(summary(GameResult.PLAYER_ENDED, 4, 2, 26, 2, 0, 0))
        assertEquals(GameStatistics.NO_ACCURACY, stats.bestAccuracy)
        val cleared = stats.record(summary(GameResult.BOARD_CLEARED, 36, 18, 200, 21, 3, 1))
        assertEquals(86, cleared.bestAccuracy)
        assertEquals(0, cleared.perfectClears)
    }

    @Test
    fun `reset statistics clears everything`() {
        val backing = InMemoryStore()
        StatisticsStore(backing).record(summary(GameResult.BOARD_CLEARED, 36, 18, 234, 18, 0, 0))
        StatisticsStore(backing).reset()
        assertEquals(GameStatistics(), StatisticsStore(backing).load())
    }
}
