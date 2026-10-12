package com.example.sumtenboard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sumtenboard.game.GameEvent
import com.example.sumtenboard.game.SumTenViewModel
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.SumTenGameState
import com.example.sumtenboard.ui.components.EquationBubble
import com.example.sumtenboard.ui.components.NumberBoard
import com.example.sumtenboard.ui.components.ProgressCard
import com.example.sumtenboard.ui.components.RuleCard
import com.example.sumtenboard.ui.components.StatusLine

@Composable
fun GameScreen(
    viewModel: SumTenViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == GameEvent.SAME_CELL) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Choose another number.", duration = SnackbarDuration.Short)
            }
        }
    }
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    val restart = {
        if (settings.confirmRestart) confirmRestart = true else viewModel.startNewGame()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val wide = maxWidth > maxHeight || maxWidth >= 600.dp
            val sidePanelWidth = if (maxWidth >= 840.dp) 360.dp else 300.dp
            if (wide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    BoardArea(
                        state = state,
                        showCoordinates = settings.showCoordinates,
                        animations = settings.animations,
                        onCellClick = viewModel::selectCell,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(
                        Modifier
                            .width(sidePanelWidth)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameHeader(onBack = onBack, onRestart = restart)
                        ProgressCard(state)
                        RuleCard()
                        StatusLine(state)
                        GameControls(state, onReshuffle = viewModel::reshuffle, onEnd = { confirmEnd = true })
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameHeader(onBack = onBack, onRestart = restart)
                    ProgressCard(state)
                    RuleCard()
                    BoardArea(
                        state = state,
                        showCoordinates = settings.showCoordinates,
                        animations = settings.animations,
                        onCellClick = viewModel::selectCell,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    StatusLine(state)
                    GameControls(state, onReshuffle = viewModel::reshuffle, onEnd = { confirmEnd = true })
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    if (state.phase == GamePhase.NO_MOVES) {
        NoMovesDialog(onEndGame = viewModel::endGame, onNewBoard = viewModel::recordAndStartNewGame)
    }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Restart this board?") },
            text = { Text("Your current progress on this board will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    viewModel.startNewGame()
                }) { Text("Restart") }
            },
            dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancel") } }
        )
    }
    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this game?") },
            text = { Text("You'll see your results for the tiles cleared so far.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    viewModel.endGame()
                }) { Text("End game") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep playing") } }
        )
    }
}

@Composable
private fun GameHeader(onBack: () -> Unit, onRestart: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SUM TEN BOARD",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Find two numbers that make 10.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestart) {
            Icon(Icons.Filled.Refresh, contentDescription = "Restart board")
        }
    }
}

@Composable
private fun BoardArea(
    state: SumTenGameState,
    showCoordinates: Boolean,
    animations: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Box(Modifier.size(side)) {
            NumberBoard(
                state = state,
                showCoordinates = showCoordinates,
                animationsEnabled = animations,
                onCellClick = onCellClick,
                modifier = Modifier.fillMaxSize()
            )
            val first = state.lastFirstValue
            val second = state.lastSecondValue
            AnimatedVisibility(
                visible = state.phase == GamePhase.RESOLVING && first != null && second != null,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                if (first != null && second != null) {
                    EquationBubble(first, second, valid = state.lastWasValid == true)
                }
            }
        }
    }
}

@Composable
private fun GameControls(state: SumTenGameState, onReshuffle: () -> Unit, onEnd: () -> Unit) {
    val canAct = state.phase == GamePhase.PLAYING
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
            onClick = onReshuffle,
            enabled = canAct && state.reshufflesLeft > 0,
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("RESHUFFLE ${state.reshufflesLeft}/${state.reshufflesAllowed}")
        }
        OutlinedButton(
            onClick = onEnd,
            enabled = canAct,
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("END GAME")
        }
    }
}

@Composable
private fun NoMovesDialog(onEndGame: () -> Unit, onNewBoard: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("NO SUM-10 PAIRS", textAlign = TextAlign.Center) },
        text = {
            Text(
                "There are no remaining pairs that make 10. Any two tiles can be paired wherever " +
                    "they sit, so reshuffling can't create a new pair from these numbers."
            )
        },
        confirmButton = { TextButton(onClick = onEndGame) { Text("END GAME") } },
        dismissButton = { TextButton(onClick = onNewBoard) { Text("NEW BOARD") } }
    )
}
