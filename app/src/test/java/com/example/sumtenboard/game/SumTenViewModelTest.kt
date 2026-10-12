package com.example.sumtenboard.game

import com.example.sumtenboard.InMemoryStore
import com.example.sumtenboard.boardOf
import com.example.sumtenboard.cellAt
import com.example.sumtenboard.data.SettingsStore
import com.example.sumtenboard.data.StatisticsStore
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.TOTAL_TILES
import com.example.sumtenboard.idAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SumTenViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var settingsBacking: InMemoryStore
    private lateinit var statsBacking: InMemoryStore
    private lateinit var vm: SumTenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settingsBacking = InMemoryStore()
        statsBacking = InMemoryStore()
        vm = SumTenViewModel(SettingsStore(settingsBacking), StatisticsStore(statsBacking), SumTenGameEngine())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun load(vararg placed: Pair<String, Int>) {
        vm.startGame(SumTenGameEngine().newGame(boardOf(*placed)))
    }

    private fun id(label: String) = vm.gameState.value.idAt(label)

    @Test
    fun `initial game has 36 tiles`() {
        assertEquals(TOTAL_TILES, vm.gameState.value.tilesRemaining)
        assertEquals(TOTAL_TILES, vm.gameState.value.activeCells.size)
    }

    @Test
    fun `valid pair is removed after the animation delay`() = runTest(dispatcher) {
        load("A1" to 4, "B4" to 6, "C1" to 3, "D1" to 7)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B4"))
        assertEquals(GamePhase.RESOLVING, vm.gameState.value.phase)
        advanceUntilIdle()
        val s = vm.gameState.value
        assertEquals(GamePhase.PLAYING, s.phase)
        assertEquals(CellState.EMPTY, s.cellAt("A1").state)
        assertEquals(2, s.tilesRemaining)
        assertEquals(1, s.pairsRemoved)
    }

    @Test
    fun `rapid taps during resolution are ignored`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        // Hammer the board while the pair resolves.
        repeat(5) {
            vm.selectCell(id("C1"))
            vm.selectCell(id("D1"))
            vm.selectCell(id("B1"))
        }
        advanceUntilIdle()
        val s = vm.gameState.value
        assertEquals(1, s.moves)
        assertEquals(1, s.pairsRemoved)
        assertEquals(13, s.score)
        assertEquals(2, s.tilesRemaining)
        assertTrue(s.selectedCellIds.isEmpty())
    }

    @Test
    fun `invalid attempt clears selection and counts a failure`() = runTest(dispatcher) {
        load("A1" to 4, "B4" to 7, "C1" to 6, "D1" to 3)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B4"))
        advanceUntilIdle()
        val s = vm.gameState.value
        assertEquals(1, s.failedAttempts)
        assertEquals(1, s.moves)
        assertEquals(4, s.tilesRemaining)
        assertTrue(s.selectedCellIds.isEmpty())
    }

    @Test
    fun `clearing the board records statistics once`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        advanceUntilIdle()
        assertEquals(GamePhase.COMPLETED, vm.gameState.value.phase)
        val summary = vm.lastSummary.value!!
        assertEquals(GameResult.BOARD_CLEARED, summary.result)
        assertTrue(summary.isPerfectClear)
        vm.endGame() // already finished — must not double count
        assertEquals(1, vm.statistics.value.gamesPlayed)
        assertEquals(1, vm.statistics.value.perfectClears)
        assertEquals(1, StatisticsStore(statsBacking).load().gamesPlayed)
    }

    @Test
    fun `ending a game records an incomplete result`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        advanceUntilIdle()
        vm.endGame()
        val summary = vm.lastSummary.value!!
        assertEquals(GameResult.PLAYER_ENDED, summary.result)
        assertEquals(1, vm.statistics.value.incompleteGames)
    }

    @Test
    fun `restart deals a fresh full board`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        vm.startNewGame() // restart mid-resolution
        advanceUntilIdle()
        val s = vm.gameState.value
        assertEquals(TOTAL_TILES, s.tilesRemaining)
        assertEquals(0, s.moves)
        assertEquals(0, s.score)
        assertEquals(GamePhase.PLAYING, s.phase)
        assertEquals(0, vm.statistics.value.gamesPlayed)
    }

    @Test
    fun `no-move dialog state appears once and new board records the run`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6, "C1" to 1, "D1" to 1)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        advanceUntilIdle()
        assertEquals(GamePhase.NO_MOVES, vm.gameState.value.phase)
        vm.selectCell(id("C1")) // input ignored in NO_MOVES
        assertTrue(vm.gameState.value.selectedCellIds.isEmpty())
        vm.recordAndStartNewGame()
        assertEquals(GameResult.NO_MOVES, vm.lastSummary.value!!.result)
        assertEquals(TOTAL_TILES, vm.gameState.value.tilesRemaining)
    }

    @Test
    fun `settings changes persist and update hints live`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6)
        vm.selectCell(id("A1"))
        assertEquals(CellState.VALID_TARGET, vm.gameState.value.cellAt("B1").state)
        vm.updateSettings { it.copy(showValidMatches = false, showCoordinates = true) }
        assertEquals(CellState.ACTIVE, vm.gameState.value.cellAt("B1").state)
        val reloaded = SettingsStore(settingsBacking).load()
        assertFalse(reloaded.showValidMatches)
        assertTrue(reloaded.showCoordinates)
        vm.resetSettings()
        assertTrue(vm.settings.value.showValidMatches)
    }

    @Test
    fun `reshuffle through the view model`() = runTest(dispatcher) {
        load("A1" to 4, "F6" to 6)
        val before = vm.gameState.value.activeCells.associate { it.id to it.coordinate }
        vm.reshuffle()
        val after = vm.gameState.value
        assertEquals(1, after.reshufflesUsed)
        assertNotEquals(before, after.activeCells.associate { it.id to it.coordinate })
    }

    @Test
    fun `reset statistics clears persisted values`() = runTest(dispatcher) {
        load("A1" to 4, "B1" to 6)
        vm.selectCell(id("A1"))
        vm.selectCell(id("B1"))
        advanceUntilIdle()
        vm.resetStatistics()
        assertEquals(0, vm.statistics.value.gamesPlayed)
        assertEquals(0, StatisticsStore(statsBacking).load().gamesPlayed)
    }
}
