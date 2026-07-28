package com.nexshell.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexshell.core.Workspace
import kotlinx.coroutines.launch

enum class DrawerDestination { WORKSPACES, SNAPSHOTS, ROOTFS, FONT, SERVICES, SETUP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    activeWorkspace: Workspace?,
    onNavigate: (DrawerDestination) -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("NexShell", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                activeWorkspace?.let {
                    Text(it.displayName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Workspaces") },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onNavigate(DrawerDestination.WORKSPACES) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Snapshots") },
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onNavigate(DrawerDestination.SNAPSHOTS) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("RootFS Manager") },
                    icon = { Icon(Icons.Filled.Storage, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onNavigate(DrawerDestination.ROOTFS) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Font") },
                    icon = { Icon(Icons.Filled.FontDownload, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onNavigate(DrawerDestination.FONT) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        content { scope.launch { drawerState.open() } }
    }
}