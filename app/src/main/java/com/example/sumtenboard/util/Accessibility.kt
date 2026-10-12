package com.example.sumtenboard.util

import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.NumberCell
import com.example.sumtenboard.game.model.SumTenGameState

/** Spoken description of a tile, independent of color or animation. */
fun cellAccessibilityLabel(cell: NumberCell): String {
    val at = cell.coordinate.label
    return when (cell.state) {
        CellState.ACTIVE -> "Number ${cell.value} at $at"
        CellState.SELECTED -> "Selected number ${cell.value} at $at"
        CellState.VALID_TARGET -> "Number ${cell.value} at $at, makes 10"
        CellState.REMOVING -> "Number ${cell.value} at $at, removing"
        CellState.EMPTY -> "Empty cell at $at"
    }
}

fun gameStatusAccessibilityLabel(state: SumTenGameState): String = buildString {
    append("${state.tilesRemaining} tiles remaining, ")
    append("${state.pairsRemoved} pairs removed, ")
    append("${state.failedAttempts} failed attempts, ")
    append("score ${state.score}")
}

fun equationText(first: Int, second: Int): String {
    val sum = first + second
    return if (sum == 10) "$first + $second = 10 ✓" else "$first + $second = $sum"
}
