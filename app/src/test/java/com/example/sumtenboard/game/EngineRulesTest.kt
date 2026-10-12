package com.example.sumtenboard.game

import com.example.sumtenboard.boardOf
import com.example.sumtenboard.cellAt
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.accuracyPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineRulesTest {
    private val engine = SumTenGameEngine()

    @Test fun `1 + 9 is valid`() = assertTrue(engine.isSumTen(1, 9))
    @Test fun `2 + 8 is valid`() = assertTrue(engine.isSumTen(2, 8))
    @Test fun `3 + 7 is valid`() = assertTrue(engine.isSumTen(3, 7))
    @Test fun `4 + 6 is valid`() = assertTrue(engine.isSumTen(4, 6))
    @Test fun `5 + 5 is valid`() = assertTrue(engine.isSumTen(5, 5))
    @Test fun `order does not matter`() = assertTrue(engine.isSumTen(9, 1) && engine.isSumTen(6, 4))
    @Test fun `4 + 4 is invalid`() = assertFalse(engine.isSumTen(4, 4))
    @Test fun `7 + 7 is invalid`() = assertFalse(engine.isSumTen(7, 7))

    @Test
    fun `only complements of ten are valid across all 1-9 combinations`() {
        for (a in 1..9) for (b in 1..9) {
            assertEquals("$a + $b", a + b == 10, engine.isSumTen(a, b))
        }
        listOf(1 to 1, 1 to 8, 2 to 7, 3 to 6, 4 to 5, 6 to 6, 7 to 8, 8 to 9, 8 to 8)
            .forEach { (a, b) -> assertFalse("$a + $b", engine.isSumTen(a, b)) }
    }

    @Test
    fun `findValidPairs returns each pair once with no reversed duplicates`() {
        val state = engine.newGame(boardOf("A1" to 4, "B1" to 4, "C1" to 6, "D1" to 6, "E1" to 5, "F1" to 5, "A2" to 2, "B2" to 8))
        val pairs = engine.findValidPairs(state.cells)
        // 4×6 combinations (2×2) + 5+5 + 2+8
        assertEquals(6, pairs.size)
        assertTrue(pairs.all { (a, b) -> a.value + b.value == 10 && a.id != b.id })
        val unordered = pairs.map { setOf(it.first.id, it.second.id) }
        assertEquals(unordered.size, unordered.toSet().size)
    }

    @Test
    fun `findValidPairs ignores empty cells`() {
        val state = engine.newGame(boardOf("A1" to 4))
        // All other cells are EMPTY with value 1; none should pair with anything.
        assertTrue(engine.findValidPairs(state.cells).isEmpty())
    }

    @Test
    fun `valid targets for 4 are only 6s`() {
        val state = engine.newGame(boardOf("A1" to 4, "B1" to 4, "C1" to 6, "D1" to 6, "E1" to 5))
        val targets = engine.getValidTargets(state.cellAt("A1"), state.cells)
        assertEquals(listOf(6, 6), targets.map { it.value })
    }

    @Test
    fun `valid targets for 5 are the other 5s but not itself`() {
        val state = engine.newGame(boardOf("A1" to 5, "B1" to 5, "C1" to 5, "D1" to 4))
        val targets = engine.getValidTargets(state.cellAt("A1"), state.cells)
        assertEquals(setOf(state.cellAt("B1").id, state.cellAt("C1").id), targets.map { it.id }.toSet())
    }

    @Test
    fun `valid targets for each value are its complement`() {
        val placements = (1..9).map { "${'A' + (it - 1) % 6}${(it - 1) / 6 + 1}" to it }.toTypedArray()
        val state = engine.newGame(boardOf(*placements))
        for (cell in state.activeCells) {
            val targets = engine.getValidTargets(cell, state.cells)
            if (cell.value == 5) assertTrue(targets.isEmpty())
            else assertEquals(listOf(10 - cell.value), targets.map { it.value })
        }
    }

    @Test
    fun `no-move detection with 1 1 4 4 7 7`() {
        val state = engine.newGame(boardOf("A1" to 1, "B1" to 1, "C1" to 4, "D1" to 4, "E1" to 7, "F1" to 7))
        assertFalse(engine.hasValidMove(state.cells))
        assertTrue(engine.findValidPairs(state.cells).isEmpty())
    }

    @Test
    fun `a single 5 is not a move but two 5s are`() {
        assertFalse(engine.hasValidMove(engine.newGame(boardOf("A1" to 5, "B1" to 4)).cells))
        assertTrue(engine.hasValidMove(engine.newGame(boardOf("A1" to 5, "F6" to 5)).cells))
    }

    @Test
    fun `pair score base and bonuses`() {
        assertEquals(13, engine.calculatePairScore(hadFailureSinceLastSuccess = false, usedReshuffle = false))
        assertEquals(11, engine.calculatePairScore(hadFailureSinceLastSuccess = true, usedReshuffle = false))
        assertEquals(12, engine.calculatePairScore(hadFailureSinceLastSuccess = false, usedReshuffle = true))
        assertEquals(10, engine.calculatePairScore(hadFailureSinceLastSuccess = true, usedReshuffle = true))
    }

    @Test
    fun `accuracy is successful attempts over total attempts`() {
        assertEquals(75, accuracyPercent(12, 16))
        assertEquals(100, accuracyPercent(18, 18))
        assertEquals(82, accuracyPercent(14, 17))
        assertNull(accuracyPercent(0, 0))
    }
}
