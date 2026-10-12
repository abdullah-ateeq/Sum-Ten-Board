package com.example.sumtenboard.ui

import com.example.sumtenboard.boardOf
import com.example.sumtenboard.cellAt
import com.example.sumtenboard.game.logic.SelectionOutcome
import com.example.sumtenboard.game.logic.SumTenGameEngine
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary
import com.example.sumtenboard.idAt
import com.example.sumtenboard.ui.screens.resultTitle
import com.example.sumtenboard.util.cellAccessibilityLabel
import com.example.sumtenboard.util.equationText
import com.example.sumtenboard.util.gameStatusAccessibilityLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTest {
    private val engine = SumTenGameEngine()

    @Test
    fun `cell labels describe each state`() {
        val start = engine.newGame(boardOf("B3" to 6, "C4" to 4))
        assertEquals("Number 6 at B3", cellAccessibilityLabel(start.cellAt("B3")))
        assertEquals("Empty cell at A1", cellAccessibilityLabel(start.cellAt("A1")))

        val selected = (engine.selectCell(start, start.idAt("B3"), true) as SelectionOutcome.Selected).state
        assertEquals("Selected number 6 at B3", cellAccessibilityLabel(selected.cellAt("B3")))
        assertEquals("Number 4 at C4, makes 10", cellAccessibilityLabel(selected.cellAt("C4")))
    }

    @Test
    fun `status label reads remaining tiles pairs and failures`() {
        val state = engine.newGame(boardOf("A1" to 4, "B1" to 6)).copy(pairsRemoved = 6, failedAttempts = 3)
        val label = gameStatusAccessibilityLabel(state)
        assertTrue(label.contains("2 tiles remaining"))
        assertTrue(label.contains("6 pairs removed"))
        assertTrue(label.contains("3 failed attempts"))
    }

    @Test
    fun `equation text for valid and invalid pairs`() {
        assertEquals("4 + 6 = 10 ✓", equationText(4, 6))
        assertEquals("4 + 7 = 11", equationText(4, 7))
    }

    @Test
    fun `result titles`() {
        fun s(result: GameResult, tiles: Int, failed: Int, reshuffles: Int) =
            GameSummary(result, tiles, tiles / 2, 0, tiles / 2 + failed, failed, reshuffles)
        assertEquals("PERFECT CLEAR", resultTitle(s(GameResult.BOARD_CLEARED, 36, 0, 0)))
        assertEquals("BOARD CLEARED", resultTitle(s(GameResult.BOARD_CLEARED, 36, 1, 0)))
        assertEquals("BOARD CLEARED", resultTitle(s(GameResult.BOARD_CLEARED, 36, 0, 1)))
        assertEquals("NO MORE PAIRS", resultTitle(s(GameResult.NO_MOVES, 28, 2, 0)))
        assertEquals("GAME ENDED", resultTitle(s(GameResult.PLAYER_ENDED, 20, 0, 0)))
    }
}
