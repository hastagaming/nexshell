package com.nexshell.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nexshell.core.WorkspaceProperties
import com.nexshell.extrakeys.ExtraKeysState
import com.nexshell.terminal.TerminalSessionHolder
import com.nexshell.ui.extrakeys.ExtraKeysRow

@Composable
fun TerminalScreenWithKeys(
    session: TerminalSessionHolder,
    properties: WorkspaceProperties,
    modifier: Modifier = Modifier
) {
    val extraKeysState = remember { ExtraKeysState() }

    Column(modifier = modifier.fillMaxSize()) {
        TerminalView(
            session = session,
            extraKeysState = extraKeysState,
            modifier = Modifier.weight(1f)
        )
        ExtraKeysRow(
            rows = properties.extraKeys,
            session = session,
            state = extraKeysState
        )
    }
}