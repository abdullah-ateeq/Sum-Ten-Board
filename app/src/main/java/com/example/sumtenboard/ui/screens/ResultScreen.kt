package com.example.sumtenboard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sumtenboard.game.SumTenViewModel
import com.example.sumtenboard.game.model.GameResult
import com.example.sumtenboard.game.model.GameSummary
import com.example.sumtenboard.game.model.TOTAL_PAIRS
import com.example.sumtenboard.game.model.TOTAL_TILES

fun resultTitle(summary: GameSummary): String = when {
    summary.isPerfectClear -> "PERFECT CLEAR"
    summary.result == GameResult.BOARD_CLEARED -> "BOARD CLEARED"
    summary.result == GameResult.NO_MOVES -> "NO MORE PAIRS"
    else -> "GAME ENDED"
}

@Composable
fun ResultScreen(viewModel: SumTenViewModel, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val summary by viewModel.lastSummary.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = summary

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (s == null) {
                Text("No finished game yet.", style = MaterialTheme.typography.titleMedium)
            } else {
                ResultContent(s, animate = settings.animations)
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(56.dp)
            ) { Text("PLAY AGAIN") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(48.dp)
            ) { Text("HOME") }
        }
    }
}

@Composable
private fun ResultContent(summary: GameSummary, animate: Boolean) {
    val badgeScale = remember { Animatable(if (animate) 0.6f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) badgeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    val perfect = summary.isPerfectClear
    val cleared = summary.result == GameResult.BOARD_CLEARED
    val colors = MaterialTheme.colorScheme

    Surface(
        color = when {
            perfect -> colors.tertiaryContainer
            cleared -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.graphicsLayer {
            scaleX = badgeScale.value
            scaleY = badgeScale.value
        }
    ) {
        Column(
            Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (perfect) {
                Text("★ ★ ★", style = MaterialTheme.typography.titleLarge, color = colors.tertiary)
            }
            Text(
                resultTitle(summary),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "${summary.tilesCleared} / $TOTAL_TILES TILES CLEARED",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
            if (perfect) {
                Text(
                    "Every attempt made ten. No reshuffles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    ElevatedCard(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            val accuracy = summary.accuracyPercent?.let { "$it%" } ?: "—"
            ResultRow("PAIRS", "${summary.pairsRemoved} / $TOTAL_PAIRS")
            ResultRow("SCORE", "${summary.score}")
            ResultRow("MOVES", "${summary.moves}")
            ResultRow("FAILED", "${summary.failedAttempts}")
            ResultRow("ACCURACY", accuracy)
            ResultRow("RESHUFFLES USED", "${summary.reshufflesUsed}", last = true)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, last: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
