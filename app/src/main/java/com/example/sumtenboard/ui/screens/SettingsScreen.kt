package com.example.sumtenboard.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sumtenboard.data.AppSettings
import com.example.sumtenboard.data.ThemeMode
import com.example.sumtenboard.game.SumTenViewModel
import kotlinx.coroutines.launch

private enum class PendingReset { STATISTICS, SETTINGS }

@Composable
fun SettingsScreen(viewModel: SumTenViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var pendingReset by rememberSaveable { mutableStateOf<PendingReset?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val update: ((AppSettings) -> AppSettings) -> Unit = viewModel::updateSettings

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Text(
                    "SETTINGS",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() }
                )
                Spacer(Modifier.height(16.dp))

                SettingsSection("APPEARANCE") {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                    )
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { update { it.copy(themeMode = mode) } },
                                label = { Text(mode.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                            )
                        }
                    }
                    Divider()
                    SwitchRow(
                        "Dynamic color",
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Use colors from your wallpaper"
                        else "Requires Android 12 or newer",
                        settings.dynamicColor
                    ) { v -> update { it.copy(dynamicColor = v) } }
                    Divider()
                    SwitchRow("Animations", "Removal, shake and reshuffle motion", settings.animations) { v ->
                        update { it.copy(animations = v) }
                    }
                }

                SettingsSection("FEEDBACK") {
                    SwitchRow("Sound", "Short tones for pairs and misses", settings.sound) { v ->
                        update { it.copy(sound = v) }
                    }
                    Divider()
                    SwitchRow("Haptic feedback", "Vibration on taps and pairs", settings.haptics) { v ->
                        update { it.copy(haptics = v) }
                    }
                }

                SettingsSection("GAMEPLAY") {
                    SwitchRow(
                        "Show valid matches",
                        "Outline numbers that make 10 with your selection",
                        settings.showValidMatches
                    ) { v -> update { it.copy(showValidMatches = v) } }
                    Divider()
                    SwitchRow("Show coordinates", "Label columns A–F and rows 1–6", settings.showCoordinates) { v ->
                        update { it.copy(showCoordinates = v) }
                    }
                    Divider()
                    SwitchRow(
                        "Auto detect no moves",
                        "Tell me when no pair can make 10",
                        settings.autoDetectNoMoves
                    ) { v -> update { it.copy(autoDetectNoMoves = v) } }
                    Divider()
                    SwitchRow("Confirm restart", "Ask before dealing a new board", settings.confirmRestart) { v ->
                        update { it.copy(confirmRestart = v) }
                    }
                }

                SettingsSection("DATA") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { pendingReset = PendingReset.STATISTICS },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("RESET STATISTICS") }
                        OutlinedButton(
                            onClick = { pendingReset = PendingReset.SETTINGS },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("RESET SETTINGS") }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    pendingReset?.let { reset ->
        val stats = reset == PendingReset.STATISTICS
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(if (stats) "Reset statistics?" else "Reset settings?") },
            text = {
                Text(
                    if (stats) "Games played, best scores and totals will be cleared."
                    else "All settings return to their defaults."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingReset = null
                    if (stats) viewModel.resetStatistics() else viewModel.resetSettings()
                    scope.launch { snackbar.showSnackbar(if (stats) "Statistics reset" else "Settings reset") }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { pendingReset = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp).semantics { heading() }
    )
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) { content() }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
