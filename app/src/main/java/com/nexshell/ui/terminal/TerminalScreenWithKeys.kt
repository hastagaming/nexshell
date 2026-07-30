package com.nexshell.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nexshell.core.WorkspaceProperties
import com.nexshell.extrakeys.ExtraKeysBridge
import com.nexshell.extrakeys.ExtraKeysState
import com.nexshell.terminal.TerminalSessionHolder
import com.nexshell.ui.extrakeys.ExtraKeysRow
import com.termux.view.TerminalView as TermuxTerminalView

@Composable
fun TerminalScreenWithKeys(
    session: TerminalSessionHolder,
    properties: WorkspaceProperties,
    modifier: Modifier = Modifier
) {
    val extraKeysState = remember { ExtraKeysState() }
    var bridge by remember { mutableStateOf<ExtraKeysBridge?>(null) }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        TerminalView(
            session = session,
            extraKeysState = extraKeysState,
            onViewReady = { view -> if (bridge == null) bridge = ExtraKeysBridge(view) },
            modifier = Modifier.weight(1f)
        )
        bridge?.let {
            ExtraKeysRow(
                rows = properties.extraKeys,
                bridge = it,
                state = extraKeysState
            )
        }
    }
}