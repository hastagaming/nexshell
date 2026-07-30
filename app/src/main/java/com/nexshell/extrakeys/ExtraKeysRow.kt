package com.nexshell.extrakeys

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
import com.nexshell.extrakeys.ExtraKeysBridge
import com.nexshell.extrakeys.ExtraKeysState

@Composable
fun ExtraKeysRow(
    rows: List<List<String>>,
    bridge: ExtraKeysBridge,
    state: ExtraKeysState,
    modifier: Modifier = Modifier
) {
    val modifierKeys = setOf("CTRL", "ALT", "SHIFT", "FN")

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val isModifier = label.uppercase() in modifierKeys
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
                            .padding(0.5.dp)
                            .clickable {
                                if (isModifier) {
                                    state.toggle(label)
                                } else {
                                    val macro = buildString {
                                        if (state.ctrlActive) append("CTRL ")
                                        if (state.altActive) append("ALT ")
                                        if (state.shiftActive) append("SHIFT ")
                                        if (state.fnActive) append("FN ")
                                        append(label)
                                    }
                                    bridge.send(macro)
                                    state.releaseNonSticky()
                                }
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
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
