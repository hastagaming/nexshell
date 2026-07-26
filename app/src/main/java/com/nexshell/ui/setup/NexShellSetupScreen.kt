package com.nexshell.ui.setup

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexshell.core.Workspace
import com.nexshell.setup.NexShellSetup
import com.nexshell.setup.SetupResult

@Composable
fun NexShellSetupScreen(workspace: Workspace, onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val setup = remember { NexShellSetup(activity) }
    var status by remember { mutableStateOf(setup.checkStatus()) }

    LaunchedEffect(Unit) {
        // Re-check whenever this screen resumes (user might return from Settings)
        status = setup.checkStatus()
        if (status == SetupResult.AlreadyGranted) {
            setup.finalizeForWorkspace(workspace)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("nexshell-setup", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Grant storage access so ${workspace.displayName} can reach " +
            "~/storage/shared, downloads, pictures, and movies from Android."
        )
        Spacer(Modifier.height(24.dp))

        when (status) {
            SetupResult.AlreadyGranted -> {
                Text("✓ Access already granted.")
                Button(onClick = {
                    setup.finalizeForWorkspace(workspace)
                    onFinished()
                }) { Text("Continue") }
            }
            SetupResult.NeedsPermissionRequest -> {
                Button(onClick = { setup.requestAccess(requestCode = 4201) }) {
                    Text("Grant Storage Access")
                }
            }
            SetupResult.Granted -> {
                Text("✓ Granted.")
            }
        }
    }
}