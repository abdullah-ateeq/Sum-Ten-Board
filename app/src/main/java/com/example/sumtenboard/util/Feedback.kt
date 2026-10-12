package com.example.sumtenboard.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.sumtenboard.game.GameEvent

/**
 * Short synthesized tones for game feedback. Uses the platform tone generator so
 * the app ships without audio assets; failures are swallowed because sound is
 * never required to play.
 */
class SoundPlayer {
    private val tones: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    } catch (_: RuntimeException) {
        null
    }

    fun play(event: GameEvent) {
        val (tone, duration) = when (event) {
            GameEvent.SELECTED -> ToneGenerator.TONE_PROP_BEEP to 40
            GameEvent.VALID_PAIR -> ToneGenerator.TONE_PROP_ACK to 120
            GameEvent.INVALID_PAIR -> ToneGenerator.TONE_PROP_NACK to 120
            GameEvent.REMOVED -> ToneGenerator.TONE_PROP_BEEP2 to 60
            GameEvent.RESHUFFLED -> ToneGenerator.TONE_PROP_PROMPT to 120
            GameEvent.BOARD_CLEARED -> ToneGenerator.TONE_CDMA_CONFIRM to 300
            GameEvent.NO_MOVES, GameEvent.NO_POSSIBLE_PAIRS -> ToneGenerator.TONE_CDMA_ABBR_INTERCEPT to 220
            GameEvent.SAME_CELL -> return
        }
        try {
            tones?.startTone(tone, duration)
        } catch (_: RuntimeException) {
            // Audio focus or hardware issues should never interrupt play.
        }
    }

    fun release() {
        tones?.release()
    }

    private companion object {
        const val VOLUME = 45
    }
}

enum class HapticStrength { LIGHT, MEDIUM, SUCCESS }

fun hapticFor(event: GameEvent): HapticStrength? = when (event) {
    GameEvent.SELECTED, GameEvent.INVALID_PAIR -> HapticStrength.LIGHT
    GameEvent.VALID_PAIR, GameEvent.REMOVED, GameEvent.RESHUFFLED -> HapticStrength.MEDIUM
    GameEvent.BOARD_CLEARED -> HapticStrength.SUCCESS
    GameEvent.NO_MOVES, GameEvent.NO_POSSIBLE_PAIRS -> HapticStrength.MEDIUM
    GameEvent.SAME_CELL -> null
}

fun View.performHaptic(strength: HapticStrength) {
    val constant = when (strength) {
        HapticStrength.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticStrength.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticStrength.SUCCESS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
