package com.example.sumtenboard.game.logic

import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.DEFAULT_RESHUFFLES
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary
import com.example.sumtenboard.game.model.NumberCell
import com.example.sumtenboard.game.model.PairScore
import com.example.sumtenboard.game.model.SumTenGameState
import com.example.sumtenboard.game.model.TARGET_SUM
import kotlin.random.Random

sealed interface SelectionOutcome {
    /** Input was not accepted (wrong phase, empty cell, unknown id). */
    data object Ignored : SelectionOutcome

    /** The already-selected cell was tapped again; it stays selected. */
    data object SameCell : SelectionOutcome

    data class Selected(val state: SumTenGameState) : SelectionOutcome

    /** A second cell was chosen and the attempt was scored; phase is RESOLVING. */
    data class Resolved(val state: SumTenGameState, val valid: Boolean) : SelectionOutcome
}

sealed interface ReshuffleOutcome {
    data object NotAvailable : ReshuffleOutcome

    data class Shuffled(val state: SumTenGameState) : ReshuffleOutcome

    /** The remaining values cannot form any sum-10 pair, so rearranging cannot help. */
    data class NoPossiblePairs(val state: SumTenGameState) : ReshuffleOutcome
}

/**
 * Pure game rules. Every function takes a state and returns a new one; there is
 * no Android or Compose code here.
 */
class SumTenGameEngine(
    private val generator: SumTenBoardGenerator = SumTenBoardGenerator(),
    private val random: Random = Random.Default
) {

    fun newGame(reshufflesAllowed: Int = DEFAULT_RESHUFFLES): SumTenGameState =
        newGame(generator.generate(), reshufflesAllowed)

    fun newGame(cells: List<NumberCell>, reshufflesAllowed: Int = DEFAULT_RESHUFFLES): SumTenGameState =
        SumTenGameState(cells = cells, reshufflesAllowed = reshufflesAllowed)

    fun isSumTen(first: Int, second: Int): Boolean = first + second == TARGET_SUM

    fun findValidPairs(activeCells: List<NumberCell>): List<Pair<NumberCell, NumberCell>> {
        val playable = activeCells.filter { it.isPlayable }
        val pairs = mutableListOf<Pair<NumberCell, NumberCell>>()
        for (i in playable.indices) {
            for (j in i + 1 until playable.size) {
                val a = playable[i]
                val b = playable[j]
                if (a.id != b.id && isSumTen(a.value, b.value)) pairs += a to b
            }
        }
        return pairs
    }

    fun hasValidMove(activeCells: List<NumberCell>): Boolean {
        val playable = activeCells.filter { it.isPlayable }
        val counts = playable.groupingBy { it.value }.eachCount()
        return counts.any { (value, count) ->
            val complement = TARGET_SUM - value
            if (complement == value) count >= 2 else (counts[complement] ?: 0) > 0
        }
    }

    fun getValidTargets(selectedCell: NumberCell, activeCells: List<NumberCell>): List<NumberCell> =
        activeCells.filter {
            it.isPlayable && it.id != selectedCell.id && isSumTen(selectedCell.value, it.value)
        }

    fun pairScore(hadFailureSinceLastSuccess: Boolean, usedReshuffle: Boolean): PairScore =
        PairScore(
            base = BASE_PAIR_POINTS,
            efficiencyBonus = if (hadFailureSinceLastSuccess) 0 else EFFICIENCY_BONUS,
            noReshuffleBonus = if (usedReshuffle) 0 else NO_RESHUFFLE_BONUS
        )

    fun calculatePairScore(hadFailureSinceLastSuccess: Boolean, usedReshuffle: Boolean): Int =
        pairScore(hadFailureSinceLastSuccess, usedReshuffle).total

    fun selectCell(state: SumTenGameState, cellId: Int, showHints: Boolean): SelectionOutcome {
        if (state.phase != GamePhase.PLAYING) return SelectionOutcome.Ignored
        val cell = state.cells.firstOrNull { it.id == cellId } ?: return SelectionOutcome.Ignored
        if (!cell.isPlayable) return SelectionOutcome.Ignored
        if (cellId in state.selectedCellIds) return SelectionOutcome.SameCell

        val firstId = state.selectedCellIds.firstOrNull()
            ?: return SelectionOutcome.Selected(selectFirst(state, cell, showHints))
        val first = state.cells.first { it.id == firstId }
        return resolve(state, first, cell)
    }

    private fun selectFirst(state: SumTenGameState, cell: NumberCell, showHints: Boolean): SumTenGameState =
        withHighlights(state.copy(selectedCellIds = listOf(cell.id)), showHints)

    /** Recomputes SELECTED / VALID_TARGET states, e.g. after the hint setting changes. */
    fun withHighlights(state: SumTenGameState, showHints: Boolean): SumTenGameState {
        if (state.phase != GamePhase.PLAYING) return state
        val selected = state.selectedCellIds.firstOrNull()?.let { id -> state.cells.firstOrNull { it.id == id } }
        val targetIds = if (selected != null && showHints) {
            getValidTargets(selected, state.cells).map { it.id }.toSet()
        } else {
            emptySet()
        }
        val cells = state.cells.map { c ->
            if (!c.isPlayable) c
            else c.copy(
                state = when (c.id) {
                    selected?.id -> CellState.SELECTED
                    in targetIds -> CellState.VALID_TARGET
                    else -> CellState.ACTIVE
                }
            )
        }
        return state.copy(cells = cells)
    }

    private fun resolve(state: SumTenGameState, first: NumberCell, second: NumberCell): SelectionOutcome {
        val sum = first.value + second.value
        val valid = isSumTen(first.value, second.value)
        val pairIds = setOf(first.id, second.id)
        val cells = state.cells.map { c ->
            when {
                !c.isPlayable -> c
                c.id in pairIds -> c.copy(state = if (valid) CellState.REMOVING else CellState.SELECTED)
                else -> c.copy(state = CellState.ACTIVE)
            }
        }
        val base = state.copy(
            cells = cells,
            phase = GamePhase.RESOLVING,
            selectedCellIds = listOf(first.id, second.id),
            moves = state.moves + 1,
            lastFirstValue = first.value,
            lastSecondValue = second.value,
            lastSum = sum,
            lastWasValid = valid,
            resolutionCount = state.resolutionCount + 1
        )
        val next = if (valid) {
            val points = pairScore(state.failedSinceLastSuccess, state.reshufflesUsed > 0)
            base.copy(
                pairsRemoved = state.pairsRemoved + 1,
                tilesRemaining = state.tilesRemaining - 2,
                score = state.score + points.total,
                lastPairScore = points,
                failedSinceLastSuccess = false
            )
        } else {
            base.copy(
                failedAttempts = state.failedAttempts + 1,
                failedSinceLastSuccess = true
            )
        }
        return SelectionOutcome.Resolved(next, valid)
    }

    /**
     * Called once the removal / mismatch feedback has played. Clears the selection,
     * turns removed tiles into empty cells and decides what happens next.
     */
    fun finishResolution(state: SumTenGameState, autoDetectNoMoves: Boolean): SumTenGameState {
        if (state.phase != GamePhase.RESOLVING) return state
        val cells = state.cells.map { c ->
            when (c.state) {
                CellState.REMOVING -> c.copy(state = CellState.EMPTY)
                CellState.SELECTED, CellState.VALID_TARGET -> c.copy(state = CellState.ACTIVE)
                else -> c
            }
        }
        val cleared = state.copy(cells = cells, selectedCellIds = emptyList(), phase = GamePhase.PLAYING)
        if (state.lastWasValid != true) return cleared
        return when {
            cleared.tilesRemaining == 0 ->
                cleared.copy(phase = GamePhase.COMPLETED, result = GameResult.BOARD_CLEARED)
            autoDetectNoMoves && !hasValidMove(cleared.cells) ->
                cleared.copy(phase = GamePhase.NO_MOVES)
            else -> cleared
        }
    }

    /**
     * Moves the remaining tiles among the positions they currently occupy. Values,
     * states and the set of empty cells are unchanged; only coordinates move.
     */
    fun reshuffleActiveCells(cells: List<NumberCell>): List<NumberCell> {
        val active = cells.filter { it.isPlayable }
        if (active.size < 2) return cells
        val coordinates = active.map { it.coordinate }
        var shuffled = coordinates
        var attempts = 0
        // Pairing does not depend on position, so any arrangement keeps the same
        // moves; we only need one that actually differs from the current layout.
        while (shuffled == coordinates && attempts < MAX_RESHUFFLE_ATTEMPTS) {
            shuffled = coordinates.shuffled(random)
            attempts++
        }
        if (shuffled == coordinates) shuffled = coordinates.drop(1) + coordinates.first()
        val newCoordinateById = active.indices.associate { active[it].id to shuffled[it] }
        return cells.map { c -> newCoordinateById[c.id]?.let { c.copy(coordinate = it) } ?: c }
    }

    fun reshuffle(state: SumTenGameState): ReshuffleOutcome {
        val canAct = state.phase == GamePhase.PLAYING || state.phase == GamePhase.NO_MOVES
        if (!canAct || state.reshufflesLeft <= 0 || state.tilesRemaining < 2) return ReshuffleOutcome.NotAvailable
        val deselected = state.copy(
            selectedCellIds = emptyList(),
            cells = state.cells.map { if (it.isPlayable) it.copy(state = CellState.ACTIVE) else it }
        )
        if (!hasValidMove(deselected.cells)) {
            return ReshuffleOutcome.NoPossiblePairs(deselected.copy(phase = GamePhase.NO_MOVES))
        }
        return ReshuffleOutcome.Shuffled(
            deselected.copy(
                cells = reshuffleActiveCells(deselected.cells),
                phase = GamePhase.PLAYING,
                reshufflesUsed = state.reshufflesUsed + 1,
                reshuffleCount = state.reshuffleCount + 1
            )
        )
    }

    /** Ends the run now. The result reflects the board at the moment of ending. */
    fun endGame(state: SumTenGameState): SumTenGameState {
        if (state.isFinished) return state
        val cells = state.cells.map { c ->
            when (c.state) {
                CellState.REMOVING -> c.copy(state = CellState.EMPTY)
                CellState.SELECTED, CellState.VALID_TARGET -> c.copy(state = CellState.ACTIVE)
                else -> c
            }
        }
        val result = when {
            state.tilesRemaining == 0 -> GameResult.BOARD_CLEARED
            !hasValidMove(cells) -> GameResult.NO_MOVES
            else -> GameResult.PLAYER_ENDED
        }
        return state.copy(
            cells = cells,
            selectedCellIds = emptyList(),
            phase = if (result == GameResult.BOARD_CLEARED) GamePhase.COMPLETED else GamePhase.ENDED,
            result = result
        )
    }

    fun summary(state: SumTenGameState): GameSummary? {
        val result = state.result ?: return null
        return GameSummary(
            result = result,
            tilesCleared = state.tilesCleared,
            pairsRemoved = state.pairsRemoved,
            score = state.score,
            moves = state.moves,
            failedAttempts = state.failedAttempts,
            reshufflesUsed = state.reshufflesUsed
        )
    }

    companion object {
        const val BASE_PAIR_POINTS = 10
        const val EFFICIENCY_BONUS = 2
        const val NO_RESHUFFLE_BONUS = 1
        private const val MAX_RESHUFFLE_ATTEMPTS = 20
    }
}
