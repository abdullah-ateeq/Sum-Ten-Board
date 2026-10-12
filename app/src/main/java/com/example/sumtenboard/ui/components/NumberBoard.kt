package com.example.sumtenboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.sumtenboard.game.model.BOARD_SIZE
import com.example.sumtenboard.game.model.CellState
import com.example.sumtenboard.game.model.Coordinate
import com.example.sumtenboard.game.model.GamePhase
import com.example.sumtenboard.game.model.NumberCell
import com.example.sumtenboard.game.model.SumTenGameState
import com.example.sumtenboard.game.model.TOTAL_TILES
import com.example.sumtenboard.ui.theme.TileNumberStyle
import com.example.sumtenboard.util.cellAccessibilityLabel

private const val RESHUFFLE_MILLIS = 500
private const val REMOVE_MILLIS = 500

/**
 * The 6 × 6 board. Empty slots are drawn underneath and tiles are positioned
 * absolutely on top, so a reshuffle slides tiles into place and removed tiles
 * simply reveal the empty slot beneath them. The board never collapses.
 */
@Composable
fun NumberBoard(
    state: SumTenGameState,
    showCoordinates: Boolean,
    animationsEnabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val side = minOf(maxWidth, maxHeight)
        val labelSpace = if (showCoordinates) 18.dp else 0.dp
        val gridSize = side - labelSpace
        val gap = gridSize * 0.024f
        val cellSize = (gridSize - gap * (BOARD_SIZE - 1)) / BOARD_SIZE
        fun offsetOf(coordinate: Coordinate): Pair<Dp, Dp> =
            labelSpace + (cellSize + gap) * coordinate.column to labelSpace + (cellSize + gap) * coordinate.row

        val reshuffleFade = remember { Animatable(1f) }
        LaunchedEffect(state.reshuffleCount) {
            if (state.reshuffleCount > 0 && animationsEnabled) {
                reshuffleFade.snapTo(0.4f)
                reshuffleFade.animateTo(1f, tween(RESHUFFLE_MILLIS, easing = FastOutSlowInEasing))
            }
        }

        Box(Modifier.size(side)) {
            if (showCoordinates) {
                CoordinateLabels(labelSpace, cellSize, gap)
            }

            val occupied = state.cells.filter { it.state != CellState.EMPTY }.map { it.coordinate }.toSet()
            for (index in 0 until TOTAL_TILES) {
                val coordinate = Coordinate.fromIndex(index)
                val (x, y) = offsetOf(coordinate)
                val isEmpty = coordinate !in occupied
                Box(
                    Modifier
                        .offset(x, y)
                        .size(cellSize)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(cellSize * 0.22f))
                        .then(
                            if (isEmpty) Modifier.semantics { contentDescription = "Empty cell at ${coordinate.label}" }
                            else Modifier
                        )
                )
            }

            val mismatch = state.phase == GamePhase.RESOLVING && state.lastWasValid == false
            Box(Modifier.graphicsLayer { alpha = reshuffleFade.value }) {
                state.cells.filter { it.state != CellState.EMPTY }.forEach { cell ->
                    key(cell.id) {
                        val (x, y) = offsetOf(cell.coordinate)
                        NumberTile(
                            cell = cell,
                            x = x,
                            y = y,
                            size = cellSize,
                            isMismatch = mismatch && cell.id in state.selectedCellIds,
                            resolutionCount = state.resolutionCount,
                            animationsEnabled = animationsEnabled,
                            onClick = { onCellClick(cell.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateLabels(labelSpace: Dp, cellSize: Dp, gap: Dp) {
    val style = MaterialTheme.typography.labelSmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    for (i in 0 until BOARD_SIZE) {
        val along = labelSpace + (cellSize + gap) * i
        Box(
            Modifier.offset(along, 0.dp).size(cellSize, labelSpace).clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) { Text("${'A' + i}", style = style, color = color) }
        Box(
            Modifier.offset(0.dp, along).size(labelSpace, cellSize).clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) { Text("${i + 1}", style = style, color = color) }
    }
}

@Composable
private fun NumberTile(
    cell: NumberCell,
    x: Dp,
    y: Dp,
    size: Dp,
    isMismatch: Boolean,
    resolutionCount: Int,
    animationsEnabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val slide: AnimationSpec<Dp> = if (animationsEnabled) tween(RESHUFFLE_MILLIS, easing = FastOutSlowInEasing) else snap()
    val animX by animateDpAsState(x, slide, label = "tileX")
    val animY by animateDpAsState(y, slide, label = "tileY")

    val container = when {
        isMismatch -> colors.errorContainer
        cell.state == CellState.SELECTED -> colors.primaryContainer
        cell.state == CellState.REMOVING -> colors.primaryContainer
        else -> colors.surfaceContainerHighest
    }
    val content = when {
        isMismatch -> colors.onErrorContainer
        cell.state == CellState.SELECTED || cell.state == CellState.REMOVING -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val borderColor = when {
        isMismatch -> colors.error
        cell.state == CellState.SELECTED || cell.state == CellState.REMOVING -> colors.primary
        cell.state == CellState.VALID_TARGET -> colors.secondary.copy(alpha = 0.85f)
        else -> Color.Transparent
    }
    val borderWidth = when {
        cell.state == CellState.SELECTED || cell.state == CellState.REMOVING || isMismatch -> 2.5.dp
        cell.state == CellState.VALID_TARGET -> 2.dp
        else -> 0.dp
    }
    val colorSpec = if (animationsEnabled) tween<Color>(160) else snap()
    val animContainer by animateColorAsState(container, colorSpec, label = "tileContainer")
    val animContent by animateColorAsState(content, colorSpec, label = "tileContent")
    val animBorder by animateColorAsState(borderColor, colorSpec, label = "tileBorder")
    val selectScale by animateFloatAsState(
        targetValue = if (cell.state == CellState.SELECTED) 1.06f else 1f,
        animationSpec = if (animationsEnabled) tween(140) else snap(),
        label = "tileSelectScale"
    )

    // Removal: pulse up, then shrink and fade away.
    val removeScale = remember { Animatable(1f) }
    val removeAlpha = remember { Animatable(1f) }
    LaunchedEffect(cell.state == CellState.REMOVING) {
        if (cell.state == CellState.REMOVING && animationsEnabled) {
            removeScale.animateTo(0.35f, keyframes {
                durationMillis = REMOVE_MILLIS
                1f at 0
                1.14f at 160
                1.1f at 240
            })
        }
    }
    LaunchedEffect(cell.state == CellState.REMOVING) {
        if (cell.state == CellState.REMOVING && animationsEnabled) {
            removeAlpha.animateTo(0f, tween(REMOVE_MILLIS - 220, delayMillis = 220))
        }
    }

    // Mismatch: brief horizontal shake.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isMismatch, resolutionCount) {
        if (isMismatch && animationsEnabled) {
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                0f at 0
                -7f at 50
                7f at 110
                -5f at 170
                5f at 230
                -2f at 290
                0f at 360
            })
        }
    }

    val shape = RoundedCornerShape(size * 0.22f)
    val density = LocalDensity.current
    val fontSize = with(density) { (size * 0.46f).toSp() }
    val elevation = if (cell.state == CellState.SELECTED) 4.dp else 1.5.dp
    val label = cellAccessibilityLabel(cell)

    Box(
        modifier = Modifier
            .offset { IntOffset(animX.roundToPx(), animY.roundToPx()) }
            .size(size)
            .graphicsLayer {
                val s = selectScale * removeScale.value
                scaleX = s
                scaleY = s
                alpha = removeAlpha.value
                translationX = shake.value * density.density
            }
            .shadow(elevation, shape, clip = false)
            .background(animContainer, shape)
            .border(borderWidth, animBorder, shape)
            .clickable(
                enabled = cell.state != CellState.REMOVING,
                role = Role.Button,
                onClickLabel = "Select",
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.value.toString(),
            style = TileNumberStyle,
            fontSize = fontSize,
            color = animContent,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
