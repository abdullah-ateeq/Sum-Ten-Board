package com.example.sumtenboard.game.model

const val BOARD_SIZE = 6
const val TOTAL_TILES = BOARD_SIZE * BOARD_SIZE
const val TOTAL_PAIRS = TOTAL_TILES / 2
const val TARGET_SUM = 10
const val DEFAULT_RESHUFFLES = 3

data class Coordinate(
    val row: Int,
    val column: Int
) {
    /** Spreadsheet-style label: columns A–F, rows 1–6 (e.g. "C4"). */
    val label: String get() = "${'A' + column}${row + 1}"

    val index: Int get() = row * BOARD_SIZE + column

    companion object {
        fun fromIndex(index: Int) = Coordinate(index / BOARD_SIZE, index % BOARD_SIZE)
    }
}

enum class CellState {
    ACTIVE,
    SELECTED,
    VALID_TARGET,
    REMOVING,
    EMPTY
}

data class NumberCell(
    val id: Int,
    val coordinate: Coordinate,
    val value: Int,
    val state: CellState
) {
    /** A cell that still holds a number and can take part in a pair. */
    val isPlayable: Boolean
        get() = state == CellState.ACTIVE || state == CellState.SELECTED || state == CellState.VALID_TARGET
}

enum class GamePhase {
    PLAYING,
    RESOLVING,
    NO_MOVES,
    COMPLETED,
    ENDED
}

enum class GameResult {
    BOARD_CLEARED,
    NO_MOVES,
    PLAYER_ENDED
}

data class SumTenGameState(
    val cells: List<NumberCell>,
    val phase: GamePhase = GamePhase.PLAYING,
    val selectedCellIds: List<Int> = emptyList(),
    val pairsRemoved: Int = 0,
    val tilesRemaining: Int = cells.count { it.isPlayable },
    val moves: Int = 0,
    val failedAttempts: Int = 0,
    val score: Int = 0,
    val reshufflesUsed: Int = 0,
    val reshufflesAllowed: Int = DEFAULT_RESHUFFLES,
    val lastFirstValue: Int? = null,
    val lastSecondValue: Int? = null,
    val lastSum: Int? = null,
    val lastWasValid: Boolean? = null,
    val result: GameResult? = null,
    /** True when a failed attempt happened after the most recent successful pair. */
    val failedSinceLastSuccess: Boolean = false,
    /** Breakdown of the points awarded for the most recent successful pair. */
    val lastPairScore: PairScore? = null,
    /** Increments on every resolved attempt so the UI can key one-shot animations. */
    val resolutionCount: Int = 0,
    /** Increments on every reshuffle so the UI can key the reshuffle animation. */
    val reshuffleCount: Int = 0
) {
    val tilesCleared: Int get() = TOTAL_TILES - tilesRemaining
    val reshufflesLeft: Int get() = (reshufflesAllowed - reshufflesUsed).coerceAtLeast(0)
    val activeCells: List<NumberCell> get() = cells.filter { it.isPlayable }
    val isFinished: Boolean get() = phase == GamePhase.COMPLETED || phase == GamePhase.ENDED
}

data class PairScore(
    val base: Int,
    val efficiencyBonus: Int,
    val noReshuffleBonus: Int
) {
    val total: Int get() = base + efficiencyBonus + noReshuffleBonus
}

/** Snapshot of a finished game, used by the result screen and statistics. */
data class GameSummary(
    val result: GameResult,
    val tilesCleared: Int,
    val pairsRemoved: Int,
    val score: Int,
    val moves: Int,
    val failedAttempts: Int,
    val reshufflesUsed: Int
) {
    /** Accuracy as a whole percentage, or null if no attempt was made. */
    val accuracyPercent: Int? get() = accuracyPercent(pairsRemoved, moves)

    val isPerfectClear: Boolean
        get() = result == GameResult.BOARD_CLEARED && failedAttempts == 0 && reshufflesUsed == 0
}

fun accuracyPercent(successful: Int, totalAttempts: Int): Int? =
    if (totalAttempts <= 0) null else Math.round(successful * 100f / totalAttempts)
