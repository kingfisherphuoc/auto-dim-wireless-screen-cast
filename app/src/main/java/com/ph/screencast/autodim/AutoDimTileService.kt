package com.ph.screencast.autodim

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/** System-bound only when needed; does not poll or keep monitoring alive itself. */
class AutoDimTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { toggle() } else toggle()
    }

    private fun toggle() {
        if (DimService.isRunning) {
            stopService(Intent(this, DimService::class.java))
            return // DimService refreshes the tile after stopping and restoring brightness.
        }
        val needsNotifications = Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (!Settings.System.canWrite(this) || needsNotifications) {
            openPermissionFlow()
            return
        }
        try {
            startForegroundService(Intent(this, DimService::class.java))
        } catch (error: RuntimeException) {
            Toast.makeText(this, "Unable to start AutoDim: ${error.message}", Toast.LENGTH_LONG).show()
            updateTile()
        }
    }

    // The PendingIntent overload only exists on API 34+. Older Android needs Intent.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun openPermissionFlow() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_ENABLE, true)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(
                this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
        } else startActivityAndCollapse(intent)
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (DimService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "AutoDim"
            contentDescription = if (DimService.isRunning) "AutoDim on" else "AutoDim off"
            if (Build.VERSION.SDK_INT >= 29) subtitle = if (DimService.isRunning) "On" else "Off"
            updateTile()
        }
    }

    companion object {
        const val EXTRA_ENABLE = "com.ph.screencast.autodim.ENABLE_FROM_TILE"

        fun refresh(context: Context) {
            // No tile may have been added yet. Monitoring must still work in that case.
            runCatching {
                requestListeningState(context, ComponentName(context, AutoDimTileService::class.java))
            }
        }
    }
}
