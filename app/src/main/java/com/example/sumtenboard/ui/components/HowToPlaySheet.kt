package com.example.sumtenboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val steps = listOf(
    "FIND" to "Choose any two active numbers.",
    "MAKE TEN" to "Their total must equal exactly 10. 1+9, 2+8, 3+7, 4+6 and 5+5 are the only pairs. Matching numbers only count for 5+5.",
    "REMOVE" to "Valid pairs disappear from the board. Empty spaces stay where they were.",
    "PLAN" to "Choose your pairs carefully because removing one number changes future options.",
    "CLEAR" to "Remove all tiles to complete the board. Fewer failed attempts means a higher score."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "HOW TO PLAY",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(16.dp))
            steps.forEach { (title, body) ->
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
            }
            FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("GOT IT")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
