package com.nexshell.extrakeys

import com.termux.shared.termux.extrakeys.ExtraKeyButton
import com.termux.shared.termux.extrakeys.ExtraKeysConstants
import com.termux.shared.termux.terminal.io.TerminalExtraKeys
import com.termux.view.TerminalView
import org.json.JSONObject

class ExtraKeysBridge(terminalView: TerminalView) {

    private val delegate = TerminalExtraKeys(terminalView)

    fun send(keyOrMacro: String) {
        val config = JSONObject().apply { put(ExtraKeyButton.KEY_KEY_NAME, keyOrMacro) }
        val button = ExtraKeyButton(
            config,
            ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY,
            ExtraKeysConstants.CONTROL_CHARS_ALIASES
        )
        delegate.onExtraKeyButtonClick(null, button, null)
    }
}
