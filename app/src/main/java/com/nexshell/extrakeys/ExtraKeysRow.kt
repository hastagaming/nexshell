package com.nexshell.ui.extrakeys

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexshell.extrakeys.ExtraKeyMacros
import com.nexshell.extrakeys.ExtraKeysState
import com.nexshell.terminal.TerminalSessionHolder

@Composable
fun ExtraKeysRow(
    rows: List<List<String>>,
    session: TerminalSessionHolder,
    state: ExtraKeysState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val isModifier = ExtraKeyMacros.isModifier(label)
                    val isActive = when (label.uppercase()) {
                        "CTRL" -> state.ctrlActive
                        "ALT" -> state.altActive
                        "SHIFT" -> state.shiftActive
                        "FN" -> state.fnActive
                        else -> false
                    }

                    Surface(
                        color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(1.dp)
                            .clickable {
                                if (isModifier) {
                                    state.toggle(label)
                                } else {
                                    val bytes = ExtraKeyMacros.resolve(label, state.ctrlActive, state.altActive)
                                    if (bytes != null) session.sendInput(bytes)
                                    state.releaseNonSticky()
                                }
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}