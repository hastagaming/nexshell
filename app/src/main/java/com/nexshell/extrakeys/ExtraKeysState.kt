package com.nexshell.extrakeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared latch state for CTRL/ALT/SHIFT/FN, mirroring Termux's real
 * behavior: tapping a modifier key latches it until the next non-modifier
 * key is sent, at which point it auto-releases (unless the user long-holds
 * to keep it sticky — handled by the caller via `lock`).
 */
class ExtraKeysState {
    var ctrlActive by mutableStateOf(false)
        private set
    var altActive by mutableStateOf(false)
        private set
    var shiftActive by mutableStateOf(false)
        private set
    var fnActive by mutableStateOf(false)
        private set

    fun toggle(modifier: String) {
        when (modifier.uppercase()) {
            "CTRL" -> ctrlActive = !ctrlActive
            "ALT" -> altActive = !altActive
            "SHIFT" -> shiftActive = !shiftActive
            "FN" -> fnActive = !fnActive
        }
    }

    /** Called after any non-modifier key is dispatched — CTRL/ALT
     *  auto-release the same way a real terminal app's extra-keys row does,
     *  so the user doesn't need to tap them off manually each time. */
    fun releaseNonSticky() {
        ctrlActive = false
        altActive = false
    }
}