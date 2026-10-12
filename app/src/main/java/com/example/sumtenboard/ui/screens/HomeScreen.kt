package com.example.sumtenboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sumtenboard.data.GameStatistics
import com.example.sumtenboard.game.SumTenViewModel
import com.example.sumtenboard.ui.components.HowToPlaySheet
import com.example.sumtenboard.ui.components.StatItem

@Composable
fun HomeScreen(viewModel: SumTenViewModel, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    var showHowTo by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) { heading() }
        ) {
            Text("SUM TEN", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Text("BOARD", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Find the pair. Make ten. Clear the board.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onPlay,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("PLAY", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))

        StatsCard(stats, Modifier.widthIn(max = 420.dp))
        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { showHowTo = true },
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("HOW TO PLAY")
        }
    }

    if (showHowTo) HowToPlaySheet(onDismiss = { showHowTo = false })
}

@Composable
private fun StatsCard(stats: GameStatistics, modifier: Modifier = Modifier) {
    val accuracy = if (stats.bestAccuracy >= 0) "${stats.bestAccuracy}%" else "—"
    val description = "Games ${stats.gamesPlayed}, boards cleared ${stats.boardsCleared}, " +
        "best clear ${stats.bestClear} tiles, best score ${stats.bestScore}, best accuracy $accuracy"
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                StatItem("GAMES", "${stats.gamesPlayed}", Modifier.weight(1f))
                StatItem("CLEARED", "${stats.boardsCleared}", Modifier.weight(1f))
                StatItem("BEST CLEAR", "${stats.bestClear}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                StatItem("BEST SCORE", "${stats.bestScore}", Modifier.weight(1f))
                StatItem("BEST ACCURACY", accuracy, Modifier.weight(1f))
                StatItem("PERFECT", "${stats.perfectClears}", Modifier.weight(1f))
            }
        }
    }
}
