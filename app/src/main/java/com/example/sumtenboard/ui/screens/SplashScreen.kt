package com.example.sumtenboard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Decorative board used only for the intro animation — not gameplay state.
private val SplashValues = listOf(
    2, 9, 5, 4, 8, 1,
    6, 1, 8, 2, 7, 5,
    4, 5, 3, 6, 9, 2,
    8, 7, 1, 7, 4, 3,
    5, 2, 6, 9, 1, 8,
    3, 4, 9, 5, 6, 7
)
private const val PAIR_A = 14 // 3 at C3
private const val PAIR_B = 21 // 7 at D4
private const val SPLASH_MILLIS = 1400

private fun phase(progress: Float, start: Float, end: Float) =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_MILLIS, easing = LinearEasing))
        onFinished()
    }
    val p = progress.value
    val colors = MaterialTheme.colorScheme
    val cell = 34.dp
    val gap = 5.dp
    val step = cell + gap
    val boardSide = cell * 6 + gap * 5

    val gridIn = phase(p, 0f, 0.22f)
    val numbersIn = phase(p, 0.15f, 0.35f)
    val connect = phase(p, 0.35f, 0.52f)
    val tenIn = phase(p, 0.48f, 0.6f)
    val pairOut = phase(p, 0.62f, 0.78f)
    val titleIn = phase(p, 0.72f, 0.95f)

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { contentDescription = "Sum Ten Board" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(boardSide)) {
                SplashValues.forEachIndexed { index, value ->
                    val row = index / 6
                    val col = index % 6
                    val stagger = phase(gridIn, (row + col) / 14f, (row + col) / 14f + 0.4f)
                    val isPair = index == PAIR_A || index == PAIR_B
                    val tileAlpha = if (isPair) stagger * (1f - pairOut) else stagger * (1f - 0.55f * connect)
                    val scale = if (isPair) 1f + 0.12f * connect - 0.6f * pairOut else 0.9f + 0.1f * stagger
                    Box(
                        Modifier
                            .offset(step * col, step * row)
                            .size(cell)
                            .graphicsLayer {
                                alpha = tileAlpha
                                scaleX = scale
                                scaleY = scale
                            }
                            .background(
                                if (isPair && connect > 0f) colors.primaryContainer else colors.surfaceContainerHighest,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isPair && connect > 0f) 2.dp else 0.dp,
                                if (isPair) colors.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$value",
                            color = colors.onSurface.copy(alpha = numbersIn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Canvas(Modifier.size(boardSide)) {
                    if (connect <= 0f || pairOut >= 1f) return@Canvas
                    val s = step.toPx()
                    val half = cell.toPx() / 2
                    val a = Offset(s * (PAIR_A % 6) + half, s * (PAIR_A / 6) + half)
                    val b = Offset(s * (PAIR_B % 6) + half, s * (PAIR_B / 6) + half)
                    drawLine(
                        color = colors.primary.copy(alpha = 1f - pairOut),
                        start = a,
                        end = a + (b - a) * connect,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                if (tenIn > 0f && pairOut < 1f) {
                    val midX = step * ((PAIR_A % 6 + PAIR_B % 6) / 2f)
                    val midY = step * ((PAIR_A / 6 + PAIR_B / 6) / 2f)
                    Box(
                        Modifier
                            .offset(midX - 4.dp, midY - 4.dp)
                            .size(cell + 8.dp)
                            .graphicsLayer {
                                alpha = tenIn * (1f - pairOut)
                                scaleX = 0.6f + 0.4f * tenIn
                                scaleY = 0.6f + 0.4f * tenIn
                            }
                            .background(colors.primary, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("10", color = colors.onPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleIn
                    translationY = (1f - titleIn) * 24f
                }
            ) {
                listOf("SUM", "TEN", "BOARD").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.displaySmall,
                        color = if (it == "TEN") colors.primary else colors.onBackground
                    )
                }
            }
        }
    }
}
