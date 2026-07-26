package com.nexshell.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceRepository(context: Context) {

    private val storage = WorkspaceStorage(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexshell_state", Context.MODE_PRIVATE)

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _activeWorkspaceId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))
    val activeWorkspaceId: StateFlow<String?> = _activeWorkspaceId.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        val ids = storage.listWorkspaceIds()
        val loaded = ids.mapNotNull { storage.loadWorkspace(it) }
        _workspaces.value = loaded

        // If no active workspace is set yet but workspaces exist, activate the first.
        if (_activeWorkspaceId.value == null && loaded.isNotEmpty()) {
            setActiveWorkspace(loaded.first().id)
        }
    }

    fun createWorkspace(distro: Distro, displayName: String): Workspace {
        val id = storage.newWorkspaceId(distro.name.lowercase())
        val ws = storage.createWorkspace(id, distro, displayName)
        storage.writeDistroMarker(ws)
        reload()
        if (_activeWorkspaceId.value == null) setActiveWorkspace(id)
        return ws
    }

    fun cloneWorkspace(sourceId: String, newDisplayName: String): Workspace {
        val newId = storage.newWorkspaceId("${sourceId}-clone")
        val ws = storage.cloneWorkspace(sourceId, newId, newDisplayName)
        reload()
        return ws
    }

    // in WorkspaceRepository — verification helper for UI to confirm independence
    fun verifyCloneIsIndependent(originalId: String, cloneId: String): Boolean {
        val original = storage.loadWorkspace(originalId) ?: return false
        val clone = storage.loadWorkspace(cloneId) ?: return false
        return original.rootDir.canonicalPath != clone.rootDir.canonicalPath &&
               clone.filesDir.exists()
    }

    fun deleteWorkspace(id: String) {
        storage.deleteWorkspace(id)
        if (_activeWorkspaceId.value == id) {
            _activeWorkspaceId.value = null
            prefs.edit().remove(KEY_ACTIVE).apply()
        }
        reload()
    }

    fun setActiveWorkspace(id: String) {
        _activeWorkspaceId.value = id
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    fun properties(id: String): WorkspaceProperties {
        val ws = _workspaces.value.find { it.id == id } ?: return WorkspaceProperties()
        return WorkspaceProperties.parse(ws.propertiesFile)
    }

    fun saveProperties(id: String, props: WorkspaceProperties) {
        val ws = _workspaces.value.find { it.id == id } ?: return
        ws.propertiesFile.writeText(props.toFileText())
    }

    companion object {
        private const val KEY_ACTIVE = "active_workspace_id"

        @Volatile private var instance: WorkspaceRepository? = null
        fun getInstance(context: Context): WorkspaceRepository =
            instance ?: synchronized(this) {
                instance ?: WorkspaceRepository(context.applicationContext).also { instance = it }
            }
    }
}