package com.example.sumtenboard.game

import com.example.sumtenboard.attempt
import com.example.sumtenboard.boardOf
import com.example.sumtenboard.game.logic.ReshuffleOutcome
import com.example.sumtenboard.game.logic.SelectionOutcome
import com.example.sumtenboard.game.logic.SumTenBoardGenerator
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.idAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReshuffleTest {
    private val engine = SumTenGameEngine(random = Random(42))

    @Test
    fun `reshuffle preserves values ids states and tile count`() {
        val start = engine.newGame(SumTenBoardGenerator(Random(5)).generate())
        val (a, b) = engine.findValidPairs(start.cells).first()
        val partly = engine.attempt(start, a.id, b.id)
        val shuffled = engine.reshuffleActiveCells(partly.cells)

        assertEquals(partly.cells.map { it.id to it.value }, shuffled.map { it.id to it.value })
        assertEquals(partly.cells.map { it.state }, shuffled.map { it.state })
        assertEquals(partly.activeCells.size, shuffled.count { it.isPlayable })
        assertEquals(partly.activeCells.map { it.value }.sorted(), shuffled.filter { it.isPlayable }.map { it.value }.sorted())
        // Empty cells stay where they were; removed tiles are not restored.
        val emptyBefore = partly.cells.filter { it.state == CellState.EMPTY }.map { it.coordinate }.toSet()
        val emptyAfter = shuffled.filter { it.state == CellState.EMPTY }.map { it.coordinate }.toSet()
        assertEquals(emptyBefore, emptyAfter)
        // No two cells share a coordinate.
        assertEquals(36, shuffled.map { it.coordinate }.toSet().size)
    }

    @Test
    fun `reshuffle with far-apart 4 and 6 changes coordinates and keeps a valid move`() {
        val start = engine.newGame(boardOf("A1" to 4, "F6" to 6, "C3" to 1, "D4" to 2))
        val outcome = engine.reshuffle(start) as ReshuffleOutcome.Shuffled
        val after = outcome.state
        assertNotEquals(
            start.activeCells.associate { it.id to it.coordinate },
            after.activeCells.associate { it.id to it.coordinate }
        )
        assertEquals(start.activeCells.map { it.value }.sorted(), after.activeCells.map { it.value }.sorted())
        assertTrue(engine.hasValidMove(after.cells))
        assertEquals(1, after.reshufflesUsed)
        assertEquals(2, after.reshufflesLeft)
        assertEquals(GamePhase.PLAYING, after.phase)
    }

    @Test
    fun `reshuffle always produces a different arrangement`() {
        val start = engine.newGame(boardOf("A1" to 5, "B1" to 5))
        repeat(20) {
            val after = (engine.reshuffle(start) as ReshuffleOutcome.Shuffled).state
            assertNotEquals(start.cellAtId(0), after.cellAtId(0))
        }
    }

    @Test
    fun `reshuffle clears a pending selection`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6))
        val selected = (engine.selectCell(start, start.idAt("A1"), true) as SelectionOutcome.Selected).state
        val after = (engine.reshuffle(selected) as ReshuffleOutcome.Shuffled).state
        assertTrue(after.selectedCellIds.isEmpty())
        assertTrue(after.activeCells.all { it.state == CellState.ACTIVE })
    }

    @Test
    fun `reshuffle is limited to three per game`() {
        var s = engine.newGame(boardOf("A1" to 4, "B1" to 6))
        repeat(3) { s = (engine.reshuffle(s) as ReshuffleOutcome.Shuffled).state }
        assertEquals(3, s.reshufflesUsed)
        assertEquals(ReshuffleOutcome.NotAvailable, engine.reshuffle(s))
    }

    @Test
    fun `impossible remaining values are reported instead of shuffled`() {
        val start = engine.newGame(boardOf("A1" to 1, "B1" to 1, "C1" to 4, "D1" to 4, "E1" to 7, "F1" to 7))
        val outcome = engine.reshuffle(start)
        assertTrue(outcome is ReshuffleOutcome.NoPossiblePairs)
        val state = (outcome as ReshuffleOutcome.NoPossiblePairs).state
        assertEquals(GamePhase.NO_MOVES, state.phase)
        assertEquals(0, state.reshufflesUsed)
    }

    @Test
    fun `using a reshuffle removes the no-reshuffle bonus`() {
        val start = engine.newGame(boardOf("A1" to 4, "B1" to 6, "C1" to 3, "D1" to 7))
        val shuffled = (engine.reshuffle(start) as ReshuffleOutcome.Shuffled).state
        val four = shuffled.activeCells.first { it.value == 4 }
        val six = shuffled.activeCells.first { it.value == 6 }
        val after = engine.attempt(shuffled, four.id, six.id)
        assertEquals(12, after.score)
    }

    private fun com.example.sumtenboard.game.model.SumTenGameState.cellAtId(id: Int) = cells.first { it.id == id }.coordinate
}
