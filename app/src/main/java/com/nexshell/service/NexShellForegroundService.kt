package com.nexshell.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nexshell.MainActivity
import com.nexshell.R
import com.nexshell.terminal.SessionManager

class NexShellForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isWakeLockHeld = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXIT -> {
                handleExit()
                return START_NOT_STICKY
            }
            ACTION_ACQUIRE_WAKELOCK -> {
                acquireWakelock()
                updateNotification()
                return START_STICKY
            }
            ACTION_RELEASE_WAKELOCK -> {
                releaseWakelock()
                updateNotification()
                return START_STICKY
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun refreshNotification() = updateNotification()

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())

        // Auto-stop the service once nothing is running and no wakelock is held —
        // a foreground service with a stale "0 sessions" notification is clutter,
        // not a real running state.
        if (SessionManager.totalRunningCount() == 0 && !isWakeLockHeld) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val runningWorkspaces = SessionManager.runningWorkspaceNames()
        val sessionCount = SessionManager.totalRunningCount()

        val rootfsText = formatRootfsText(runningWorkspaces)
        val sessionText = if (sessionCount == 1) "1 session is running" else "$sessionCount sessions are running"

        val contentText = if (runningWorkspaces.isEmpty()) "No workspace running" else "$rootfsText\n$sessionText"

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val exitIntent = PendingIntent.getService(
            this, 1, Intent(this, NexShellForegroundService::class.java).setAction(ACTION_EXIT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val wakelockIntent = if (isWakeLockHeld) {
            PendingIntent.getService(
                this, 2, Intent(this, NexShellForegroundService::class.java).setAction(ACTION_RELEASE_WAKELOCK),
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this, 3, Intent(this, NexShellForegroundService::class.java).setAction(ACTION_ACQUIRE_WAKELOCK),
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexShell")
            .setContentText(contentText.lines().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText + if (isWakeLockHeld) "\n\nWakelock acquired" else ""))
            .setSmallIcon(R.drawable.ic_nexshell_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Exit", exitIntent)
            .addAction(0, if (isWakeLockHeld) "Release Wakelock" else "Acquire Wakelock", wakelockIntent)

        return builder.build()
    }

    private fun formatRootfsText(workspaceNames: List<String>): String {
        val count = workspaceNames.size
        return when (count) {
            0 -> "0 rootfs running"
            1 -> "1 rootfs is running from ${workspaceNames[0]}"
            2 -> "2 rootfs are running from ${workspaceNames[0]} and ${workspaceNames[1]}"
            else -> {
                val allButLast = workspaceNames.dropLast(1).joinToString(", ")
                "$count rootfs are running from $allButLast, and ${workspaceNames.last()}"
            }
        }
    }

    private fun handleExit() {
        // Stop every live session's real process, every registered service's
        // real process, release the wakelock if held, then tear the
        // foreground service down. Order matters: sessions/services first so
        // no orphaned child processes are left behind once the service dies.
        SessionManager.sessions.value.forEach { session ->
            SessionManager.closeSession(session.id)
        }
        releaseWakelock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakelock() {
        if (isWakeLockHeld) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NexShell:SessionWakeLock").apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }
        isWakeLockHeld = true
    }

    private fun releaseWakelock() {
        if (!isWakeLockHeld) return
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        isWakeLockHeld = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "NexShell Sessions", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows running workspaces and sessions"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "nexshell_sessions"
        const val NOTIFICATION_ID = 1001
        const val ACTION_EXIT = "com.nexshell.action.EXIT"
        const val ACTION_ACQUIRE_WAKELOCK = "com.nexshell.action.ACQUIRE_WAKELOCK"
        const val ACTION_RELEASE_WAKELOCK = "com.nexshell.action.RELEASE_WAKELOCK"
        // Partial wakelock auto-expires after 12h as a safety net against a
        // leaked "hold forever" state if the user forgets to release it.
        const val WAKELOCK_TIMEOUT_MS = 12 * 60 * 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, NexShellForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun refresh(context: Context) {
            context.startService(Intent(context, NexShellForegroundService::class.java))
        }
    }
}