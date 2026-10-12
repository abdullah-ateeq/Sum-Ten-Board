package com.example.sumtenboard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sumtenboard.game.logic.SumTenBoardGenerator
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.PairScore
import com.example.sumtenboard.game.model.SumTenGameState
import com.example.sumtenboard.game.model.TOTAL_PAIRS
import com.example.sumtenboard.game.model.TOTAL_TILES
import com.example.sumtenboard.game.model.accuracyPercent
import com.example.sumtenboard.util.equationText
import com.example.sumtenboard.util.gameStatusAccessibilityLabel

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun ProgressCard(state: SumTenGameState, modifier: Modifier = Modifier) {
    val status = gameStatusAccessibilityLabel(state)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = status
                liveRegion = LiveRegionMode.Polite
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("TILES LEFT", "${state.tilesRemaining}", Modifier.weight(1f))
                StatItem("PAIRS", "${state.pairsRemoved} / $TOTAL_PAIRS", Modifier.weight(1f))
                StatItem("SCORE", "${state.score}", Modifier.weight(1f))
                StatItem("MOVES", "${state.moves}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.tilesCleared / TOTAL_TILES.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(6.dp))
            val accuracy = accuracyPercent(state.pairsRemoved, state.moves)?.let { "$it%" } ?: "—"
            Text(
                "FAILED ${state.failedAttempts}  ·  ACCURACY $accuracy  ·  ${state.tilesCleared} / $TOTAL_TILES CLEARED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RuleCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Make 10: 1 plus 9, 2 plus 8, 3 plus 7, 4 plus 6, 5 plus 5"
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "MAKE 10",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            SumTenBoardGenerator.PAIR_TEMPLATES.forEach { (a, b) ->
                Box(
                    Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$a+$b",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private sealed interface StatusMessage {
    data object Prompt : StatusMessage
    data class Selected(val value: Int) : StatusMessage
    data class Valid(val first: Int, val second: Int, val points: PairScore?) : StatusMessage
    data class Invalid(val first: Int, val second: Int) : StatusMessage
}

private fun statusMessage(state: SumTenGameState): StatusMessage {
    val selectedValue = state.selectedCellIds.singleOrNull()
        ?.let { id -> state.cells.firstOrNull { it.id == id }?.value }
    val first = state.lastFirstValue
    val second = state.lastSecondValue
    return when {
        state.phase == GamePhase.PLAYING && selectedValue != null -> StatusMessage.Selected(selectedValue)
        first != null && second != null && state.lastWasValid == true ->
            StatusMessage.Valid(first, second, state.lastPairScore)
        first != null && second != null && state.lastWasValid == false -> StatusMessage.Invalid(first, second)
        else -> StatusMessage.Prompt
    }
}

/** Line under the board: what to do next, or the last equation and its points. */
@Composable
fun StatusLine(state: SumTenGameState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = statusMessage(state),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        label = "status"
    ) { message ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (message) {
                is StatusMessage.Selected -> StatusText("Selected ${message.value} — find a ${10 - message.value}")
                is StatusMessage.Valid -> {
                    StatusText(equationText(message.first, message.second), MaterialTheme.colorScheme.primary)
                    message.points?.let { p ->
                        val parts = buildList {
                            add("PAIR +${p.base}")
                            if (p.efficiencyBonus > 0) add("EFFICIENCY +${p.efficiencyBonus}")
                            if (p.noReshuffleBonus > 0) add("NO RESHUFFLE +${p.noReshuffleBonus}")
                        }
                        Text(
                            parts.joinToString("  ·  ") + "  =  +${p.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is StatusMessage.Invalid -> {
                    StatusText("${equationText(message.first, message.second)}  ≠ 10", MaterialTheme.colorScheme.error)
                    Text(
                        "NOT TEN — try another pair",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusMessage.Prompt -> StatusText("Tap two numbers that add up to 10")
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = color, textAlign = TextAlign.Center)
}

/** Small pill that floats over the board while a pair resolves. */
@Composable
fun EquationBubble(first: Int, second: Int, valid: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container = if (valid) colors.primaryContainer else colors.errorContainer
    val content = if (valid) colors.onPrimaryContainer else colors.onErrorContainer
    val text = if (valid) equationText(first, second) else "${equationText(first, second)} · NOT TEN"
    Card(
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}
