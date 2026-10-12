package com.example.sumtenboard.game

import com.example.sumtenboard.attempt
import com.example.sumtenboard.boardOf
import com.example.sumtenboard.cellAt
import com.example.sumtenboard.game.logic.SelectionOutcome
import com.example.sumtenboard.game.logic.SumTenBoardGenerator
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.idAt
import com.example.sumtenboard.solve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EngineSelectionTest {
    private val engine = SumTenGameEngine()

    @Test
    fun `4 at A1 plus 6 at B4 removes both`() {
        val start = engine.newGame(boardOf("A1" to 4, "B4" to 6, "C1" to 3, "D1" to 7))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        val outcome = engine.selectCell(selected, start.idAt("B4"), true) as SelectionOutcome.Resolved
        assertTrue(outcome.valid)
        assertEquals(GamePhase.RESOLVING, outcome.state.phase)
        assertEquals(CellState.REMOVING, outcome.state.cellAt("A1").state)
        assertEquals(10, outcome.state.lastSum)
        assertEquals(4, outcome.state.lastFirstValue)
        assertEquals(6, outcome.state.lastSecondValue)

        val after = engine.finishResolution(outcome.state, autoDetectNoMoves = true)
        assertEquals(CellState.EMPTY, after.cellAt("A1").state)
        assertEquals(CellState.EMPTY, after.cellAt("B4").state)
        assertEquals(GamePhase.PLAYING, after.phase)
        assertTrue(after.selectedCellIds.isEmpty())
    }

    @Test
    fun `5 plus 5 removes both`() {
        val start = engine.newGame(boardOf("A1" to 5, "B4" to 5, "C1" to 3, "D1" to 7))
        val after = engine.attempt(start, start.idAt("A1"), start.idAt("B4"))
        assertEquals(CellState.EMPTY, after.cellAt("A1").state)
        assertEquals(CellState.EMPTY, after.cellAt("B4").state)
        assertEquals(1, after.pairsRemoved)
    }

    @Test
    fun `4 plus 7 is rejected without removal`() {
        val start = engine.newGame(boardOf("A1" to 4, "B4" to 7, "C1" to 6, "D1" to 3))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        val outcome = engine.selectCell(selected, start.idAt("B4"), true) as SelectionOutcome.Resolved
        assertFalse(outcome.valid)
        assertEquals(11, outcome.state.lastSum)
        assertEquals(false, outcome.state.lastWasValid)

        val after = engine.finishResolution(outcome.state, true)
        assertEquals(CellState.ACTIVE, after.cellAt("A1").state)
        assertEquals(CellState.ACTIVE, after.cellAt("B4").state)
        assertEquals(4, after.tilesRemaining)
        assertEquals(0, after.pairsRemoved)
        assertEquals(1, after.failedAttempts)
        assertEquals(1, after.moves)
        assertEquals(0, after.score)
        assertTrue(after.selectedCellIds.isEmpty())
    }

    @Test
    fun `identical numbers other than 5 are rejected`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 4, "C1" to 7, "D1" to 7, "E1" to 6, "F1" to 3))
        val afterFours = engine.attempt(start, start.idAt("A1"), start.idAt("B1"))
        val afterSevens = engine.attempt(afterFours, start.idAt("C1"), start.idAt("D1"))
        assertEquals(6, afterSevens.tilesRemaining)
        assertEquals(2, afterSevens.failedAttempts)
    }

    @Test
    fun `selecting the same cell twice keeps a single selection`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        assertEquals(SelectionOutcome.SameCell, engine.selectCell(selected, start.idAt("A1"), true))
        assertEquals(listOf(start.idAt("A1")), selected.selectedCellIds)
        assertEquals(0, selected.moves)
    }

    @Test
    fun `empty cells are ignored`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6))
        assertEquals(SelectionOutcome.Ignored, engine.selectCell(start, start.idAt("F6"), true))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        assertEquals(SelectionOutcome.Ignored, engine.selectCell(selected, start.idAt("F6"), true))
    }

    @Test
    fun `input is ignored while resolving`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        val resolving = (engine.selectCell(selected, start.idAt("B1"), true) as SelectionOutcome.Resolved).state
        assertEquals(SelectionOutcome.Ignored, engine.selectCell(resolving, start.idAt("C1"), true))
        // Finishing twice is a no-op the second time.
        val done = engine.finishResolution(resolving, true)
        assertEquals(done, engine.finishResolution(done, true))
    }

    @Test
    fun `hints mark only complements when enabled`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 4, "D1" to 5))
        val on = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        assertEquals(CellState.SELECTED, on.cellAt("A1").state)
        assertEquals(CellState.VALID_TARGET, on.cellAt("B1").state)
        assertEquals(CellState.ACTIVE, on.cellAt("C1").state)
        assertEquals(CellState.ACTIVE, on.cellAt("D1").state)

        val off = (engine.selectCell(start, start.idAt("A1"), false) as SelectionOutcome.Selected).state
        assertTrue(off.cells.none { it.state == CellState.VALID_TARGET })
    }

    @Test
    fun `counters track tiles pairs moves failures and score`() {
        val start = engine.newGame(boardOf("A1" to 4, "B4" to 6, "C1" to 3, "C2" to 5, "D1" to 2, "D2" to 8))
        var s = engine.attempt(start, start.idAt("A1"), start.idAt("B4")) // valid, +13
        assertEquals(1, s.moves); assertEquals(13, s.score); assertEquals(4, s.tilesRemaining)
        s = engine.attempt(s, start.idAt("C1"), start.idAt("C2")) // invalid
        assertEquals(2, s.moves); assertEquals(1, s.failedAttempts); assertEquals(13, s.score)
        s = engine.attempt(s, start.idAt("D1"), start.idAt("D2")) // valid after a failure, +11
        assertEquals(3, s.moves); assertEquals(24, s.score); assertEquals(2, s.pairsRemoved)
        assertEquals(2, s.tilesRemaining)
        assertEquals(11, s.lastPairScore?.total)
    }

    @Test
    fun `removing the last pair completes the board`() {
        val start = engine.newGame(boardOf("C3" to 4, "E5" to 6))
        val after = engine.attempt(start, start.idAt("C3"), start.idAt("E5"))
        assertEquals(0, after.tilesRemaining)
        assertEquals(1, after.pairsRemoved)
        assertEquals(GamePhase.COMPLETED, after.phase)
        assertEquals(GameResult.BOARD_CLEARED, after.result)
    }

    @Test
    fun `perfect clear of a generated board`() {
        val start = engine.newGame(SumTenBoardGenerator(Random(7)).generate())
        val end = engine.solve(start)
        assertEquals(18, end.pairsRemoved)
        assertEquals(18, end.moves)
        assertEquals(18 * 13, end.score)
        val summary = engine.summary(end)!!
        assertTrue(summary.isPerfectClear)
        assertEquals(100, summary.accuracyPercent)
    }

    @Test
    fun `a failed attempt prevents a perfect clear`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7))
        var s = engine.attempt(start, start.idAt("A1"), start.idAt("C1"))
        s = engine.solve(s)
        val summary = engine.summary(s)!!
        assertEquals(GameResult.BOARD_CLEARED, summary.result)
        assertFalse(summary.isPerfectClear)
    }

    @Test
    fun `no-move state is detected after a removal`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 1, "D1" to 1, "E1" to 7, "F1" to 7))
        val after = engine.attempt(start, start.idAt("A1"), start.idAt("B1"), autoDetect = true)
        assertEquals(GamePhase.NO_MOVES, after.phase)
        val ended = engine.endGame(after)
        assertEquals(GameResult.NO_MOVES, ended.result)
        assertEquals(GamePhase.ENDED, ended.phase)
    }

    @Test
    fun `no-move state is not raised when auto detect is off`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 1, "D1" to 1))
        val after = engine.attempt(start, start.idAt("A1"), start.idAt("B1"), autoDetect = false)
        assertEquals(GamePhase.PLAYING, after.phase)
    }

    @Test
    fun `ending with moves left is a player-ended result`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6))
        val ended = engine.endGame(start)
        assertEquals(GameResult.PLAYER_ENDED, ended.result)
        assertEquals(GamePhase.ENDED, ended.phase)
    }
}
