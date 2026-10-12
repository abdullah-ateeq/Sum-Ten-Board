package com.example.sumtenboard

import com.example.sumtenboard.data.KeyValueStore
import com.example.sumtenboard.game.logic.SelectionOutcome
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.Coordinate
import com.example.sumtenboard.game.model.NumberCell
import com.example.sumtenboard.game.model.SumTenGameState
import com.example.sumtenboard.game.model.TOTAL_TILES

/** Parses "B4" into column B (1), row 4 (index 3). */
fun coord(label: String) = Coordinate(row = label.substring(1).toInt() - 1, column = label[0] - 'A')

/** A 6 × 6 board where only [placed] cells hold numbers; everything else is empty. */
fun boardOf(vararg placed: Pair<String, Int>): List<NumberCell> {
    val byIndex = placed.associate { (label, value) -> coord(label).index to value }
    return (0 until TOTAL_TILES).map { i ->
        val value = byIndex[i]
        NumberCell(
            id = i,
            coordinate = Coordinate.fromIndex(i),
            value = value ?: 1,
            state = if (value != null) CellState.ACTIVE else CellState.EMPTY
        )
    }
}

fun SumTenGameState.idAt(label: String): Int = cells.first { it.coordinate == coord(label) }.id

fun SumTenGameState.cellAt(label: String): NumberCell = cells.first { it.coordinate == coord(label) }

/** Selects two cells and settles the result, as the ViewModel does after the animation. */
fun SumTenGameEngine.attempt(
    state: SumTenGameState,
    firstId: Int,
    secondId: Int,
    autoDetect: Boolean = true
): SumTenGameState {
    val selected = (selectCell(state, firstId, showHints = true) as SelectionOutcome.Selected).state
    val resolved = (selectCell(selected, secondId, showHints = true) as SelectionOutcome.Resolved).state
    return finishResolution(resolved, autoDetect)
}

/** Clears the whole board by always taking the first available valid pair. */
fun SumTenGameEngine.solve(start: SumTenGameState): SumTenGameState {
    var state = start
    while (state.tilesRemaining > 0) {
        val (a, b) = findValidPairs(state.cells).first()
        state = attempt(state, a.id, b.id)
    }
    return state
}

class InMemoryStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun getString(key: String, default: String) = values[key] as? String ?: default
    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
            override fun putInt(key: String, value: Int) { values[key] = value }
            override fun putString(key: String, value: String) { values[key] = value }
        }.block()
    }
    override fun remove(keys: Collection<String>) { keys.forEach { values.remove(it) } }
}
