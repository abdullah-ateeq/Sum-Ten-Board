package com.example.sumtenboard.game

import com.example.sumtenboard.game.logic.SumTenBoardGenerator
import com.example.sumtenboard.solve
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.TOTAL_PAIRS
import com.example.sumtenboard.game.model.TOTAL_TILES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardGeneratorTest {

    private fun generator(seed: Int) = SumTenBoardGenerator(Random(seed))

    @Test
    fun `board has exactly 36 active cells with unique coordinates and ids`() {
        val cells = generator(1).generate()
        assertEquals(TOTAL_TILES, cells.size)
        assertTrue(cells.all { it.state == CellState.ACTIVE })
        assertEquals(TOTAL_TILES, cells.map { it.coordinate }.toSet().size)
        assertEquals(TOTAL_TILES, cells.map { it.id }.toSet().size)
    }

    @Test
    fun `all numbers are between 1 and 9`() {
        repeat(50) { seed ->
            assertTrue(generator(seed).generate().all { it.value in 1..9 })
        }
    }

    @Test
    fun `exactly 18 pair units are generated and each sums to ten`() {
        repeat(50) { seed ->
            val units = generator(seed).generatePairUnits()
            assertEquals(TOTAL_PAIRS, units.size)
            assertTrue(units.all { (a, b) -> a + b == 10 })
            assertTrue(units.all { it in SumTenBoardGenerator.PAIR_TEMPLATES })
        }
    }

    @Test
    fun `generated values split completely into sum-ten pairs`() {
        repeat(100) { seed ->
            val values = generator(seed).generate().map { it.value }
            assertTrue(SumTenBoardGenerator.canPairCompletely(values))
        }
    }

    @Test
    fun `positions are randomized between games`() {
        val a = generator(1).generate().map { it.value }
        val b = generator(2).generate().map { it.value }
        assertNotEquals(a, b)
    }

    @Test
    fun `pair distribution varies between games`() {
        val distributions = (0 until 40).map { seed ->
            generator(seed).generatePairUnits().groupingBy { it }.eachCount()
        }.toSet()
        assertTrue("expected several different distributions", distributions.size > 1)
    }

    @Test
    fun `pair units are not kept together as blocks`() {
        // If units were shuffled as blocks, every reading-order pair would sum to 10.
        val blockLike = (0 until 50).count { seed ->
            generator(seed).generate().chunked(2).all { (a, b) -> a.value + b.value == 10 }
        }
        assertEquals(0, blockLike)
    }

    @Test
    fun `random boards are valid and fully clearable`() {
        val engine = SumTenGameEngine()
        repeat(200) { seed ->
            val cells = generator(seed).generate()
            assertTrue(SumTenBoardGenerator.isValidBoard(cells))
            val start = engine.newGame(cells)
            assertTrue(engine.hasValidMove(start.cells))
            val end = engine.solve(start)
            assertEquals(0, end.tilesRemaining)
        }
    }

    @Test
    fun `isValidBoard rejects bad boards`() {
        val cells = generator(3).generate()
        assertFalse(SumTenBoardGenerator.isValidBoard(cells.drop(1)))
        assertFalse(SumTenBoardGenerator.isValidBoard(cells.mapIndexed { i, c -> if (i == 0) c.copy(value = 0) else c }))
        // Changing one value breaks the complement balance.
        val first = cells[0]
        val broken = cells.mapIndexed { i, c -> if (i == 0) c.copy(value = if (first.value == 9) 8 else first.value + 1) else c }
        assertFalse(SumTenBoardGenerator.isValidBoard(broken))
    }
}
