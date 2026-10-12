package com.example.sumtenboard

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sumtenboard.data.SettingsStore
import com.example.sumtenboard.data.SharedPreferencesStore
import com.example.sumtenboard.data.StatisticsStore
import com.example.sumtenboard.game.SumTenViewModel
import com.example.sumtenboard.navigation.SumTenApp
import com.example.sumtenboard.ui.theme.SumTenBoardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SumTenViewModel by viewModels {
        SumTenViewModel.Factory(
            settingsStore = SettingsStore(
                SharedPreferencesStore(getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE))
            ),
            statisticsStore = StatisticsStore(
                SharedPreferencesStore(getSharedPreferences(StatisticsStore.PREFS_NAME, Context.MODE_PRIVATE))
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SumTenBoardTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                SumTenApp(viewModel)
            }
        }
    }
}
