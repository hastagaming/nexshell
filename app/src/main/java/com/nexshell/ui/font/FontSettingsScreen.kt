package com.nexshell.ui.font

import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeTypeface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexshell.core.Workspace
import com.nexshell.core.WorkspaceProperties
import com.nexshell.core.WorkspaceRepository
import com.nexshell.font.FontCatalog
import com.nexshell.font.FontOption
import com.nexshell.font.FontSource
import java.io.File

@Composable
fun FontSettingsScreen(workspace: Workspace, repository: WorkspaceRepository) {
    val context = LocalContext.current
    var currentProps by remember { mutableStateOf(repository.properties(workspace.id)) }
    var selectedFontName by remember { mutableStateOf(currentProps.fontFamily) }
    var fontSize by remember { mutableStateOf(currentProps.fontSize) }
    var customPath by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val destDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val destFile = File(destDir, "custom_${System.currentTimeMillis()}.ttf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            customPath = destFile.absolutePath
            selectedFontName = "Custom (${destFile.name})"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Font — ${workspace.displayName}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        (FontCatalog.nerdFonts + FontCatalog.systemDefault).forEach { option ->
            ListItem(
                headlineContent = { Text(option.displayName) },
                trailingContent = {
                    RadioButton(
                        selected = selectedFontName == option.displayName,
                        onClick = { selectedFontName = option.displayName; customPath = null }
                    )
                },
                modifier = androidx.compose.ui.Modifier
            )
            FontPreview(context = context, option = option, sizeSp = fontSize)
        }

        ListItem(
            headlineContent = { Text("Import Custom Font (.ttf / .otf)") },
            trailingContent = {
                RadioButton(
                    selected = customPath != null,
                    onClick = { importLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf")) }
                )
            }
        )

        Spacer(Modifier.height(16.dp))
        Text("Size: $fontSize sp")
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { fontSize = it.toInt() },
            valueRange = 10f..24f
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val newProps = currentProps.copy(
                fontFamily = customPath ?: selectedFontName,
                fontSize = fontSize
            )
            repository.saveProperties(workspace.id, newProps)
            currentProps = newProps
        }) { Text("Save") }
    }
}

@Composable
private fun FontPreview(context: android.content.Context, option: FontOption, sizeSp: Int) {
    val typeface = remember(option, sizeSp) {
        runCatching { FontCatalog.resolveTypeface(context, option) }.getOrDefault(Typeface.MONOSPACE)
    }
    Text(
        text = "The quick brown fox jumps 0123456789 \uf015 \ue795",
        fontFamily = FontFamily(typeface.asComposeTypeface()),
        fontSize = sizeSp.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

private fun Typeface.asComposeTypeface(): androidx.compose.ui.text.font.Typeface =
    androidx.compose.ui.text.font.Typeface(this)