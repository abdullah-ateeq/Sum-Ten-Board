package com.example.sumtenboard.game.logic

import com.example.sumtenboard.game.model.BOARD_SIZE
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.Coordinate
import com.example.sumtenboard.game.model.NumberCell
import com.example.sumtenboard.game.model.TARGET_SUM
import kotlin.random.Random

/**
 * Builds a playable board out of 18 sum-10 pair units whose individual
 * numbers are then scattered across all 36 positions.
 */
class SumTenBoardGenerator(private val random: Random = Random.Default) {

    /** Picks how many units of each template (1+9 … 5+5) go on the board. */
    fun generatePairUnits(): List<Pair<Int, Int>> {
        // Start from a balanced 4/4/4/3/3 split (18 units), then move a few units
        // between templates so every game has a slightly different mix.
        val counts = IntArray(PAIR_TEMPLATES.size) { if (it < 3) 4 else 3 }
        repeat(random.nextInt(1, 4)) {
            val from = random.nextInt(counts.size)
            val to = random.nextInt(counts.size)
            if (from != to && counts[from] > MIN_UNITS_PER_TEMPLATE && counts[to] < MAX_UNITS_PER_TEMPLATE) {
                counts[from]--
                counts[to]++
            }
        }
        return PAIR_TEMPLATES.flatMapIndexed { index, template -> List(counts[index]) { template } }
    }

    fun generate(): List<NumberCell> {
        repeat(MAX_ATTEMPTS) {
            val units = generatePairUnits()
            // Shuffle individual numbers, never units as blocks.
            val values = units.flatMap { listOf(it.first, it.second) }.shuffled(random)
            val cells = values.mapIndexed { index, value ->
                NumberCell(
                    id = index,
                    coordinate = Coordinate.fromIndex(index),
                    value = value,
                    state = CellState.ACTIVE
                )
            }
            if (isValidBoard(cells) && !hasAdjacentGeneratedPartnersEverywhere(cells)) return cells
        }
        // Statistically unreachable; the last attempt is still a valid board.
        val values = generatePairUnits().flatMap { listOf(it.first, it.second) }.shuffled(random)
        return values.mapIndexed { i, v -> NumberCell(i, Coordinate.fromIndex(i), v, CellState.ACTIVE) }
    }

    /**
     * Rejects the pathological shuffle where every number sits directly next to a
     * complement in reading order, which would look like the units were never mixed.
     */
    private fun hasAdjacentGeneratedPartnersEverywhere(cells: List<NumberCell>): Boolean =
        cells.chunked(2).all { (a, b) -> a.value + b.value == TARGET_SUM }

    companion object {
        val PAIR_TEMPLATES: List<Pair<Int, Int>> = listOf(1 to 9, 2 to 8, 3 to 7, 4 to 6, 5 to 5)
        private const val MIN_UNITS_PER_TEMPLATE = 2
        private const val MAX_UNITS_PER_TEMPLATE = 5
        private const val MAX_ATTEMPTS = 20

        /**
         * A starting board is valid when it is a full 6 × 6 grid of 1–9 values that
         * can be split completely into sum-10 pairs.
         */
        fun isValidBoard(cells: List<NumberCell>): Boolean {
            if (cells.size != BOARD_SIZE * BOARD_SIZE) return false
            if (cells.map { it.coordinate }.toSet().size != cells.size) return false
            if (cells.any { it.value !in 1..9 }) return false
            return canPairCompletely(cells.map { it.value })
        }

        /** True if [values] can be partitioned entirely into pairs summing to 10. */
        fun canPairCompletely(values: List<Int>): Boolean {
            val counts = values.groupingBy { it }.eachCount()
            fun c(v: Int) = counts[v] ?: 0
            return c(1) == c(9) && c(2) == c(8) && c(3) == c(7) && c(4) == c(6) && c(5) % 2 == 0
        }
    }
}
