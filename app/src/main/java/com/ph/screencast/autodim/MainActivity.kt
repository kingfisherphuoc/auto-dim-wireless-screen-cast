package com.ph.screencast.autodim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var pendingEnable by mutableStateOf(false)

    private val settingsPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingEnable) {
            if (Settings.System.canWrite(this)) requestNotificationsAndStart()
            else {
                pendingEnable = false
                Toast.makeText(this, "Allow system settings changes to enable AutoDim.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notification denial does not prevent foreground service operation.
        if (pendingEnable) startMonitoring()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingEnable = savedInstanceState?.getBoolean("pendingEnable") ?: false
        setContent {
            MaterialTheme {
                AutoDimScreen(
                    enabled = DimService.isRunning || pendingEnable,
                    onToggle = ::setMonitoring
                )
            }
        }
        if (savedInstanceState == null) handleTileIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTileIntent(intent)
    }

    private fun handleTileIntent(intent: Intent) {
        if (intent.getBooleanExtra(AutoDimTileService.EXTRA_ENABLE, false)) {
            intent.removeExtra(AutoDimTileService.EXTRA_ENABLE)
            if (!DimService.isRunning && !pendingEnable) setMonitoring(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pendingEnable", pendingEnable)
        super.onSaveInstanceState(outState)
    }

    private fun setMonitoring(enabled: Boolean) {
        if (!enabled) {
            pendingEnable = false
            val serviceIntent = Intent(this, DimService::class.java)
            stopService(serviceIntent)
            DimService.restore(this)
            return
        }
        pendingEnable = true
        if (!Settings.System.canWrite(this)) {
            settingsPermission.launch(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            )
        } else requestNotificationsAndStart()
    }

    private fun requestNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else startMonitoring()
    }

    private fun startMonitoring() {
        try {
            if (Settings.System.canWrite(this)) {
                startForegroundService(Intent(this, DimService::class.java))
            } else {
                Toast.makeText(this, "System settings permission is required.", Toast.LENGTH_LONG).show()
            }
        } catch (error: RuntimeException) {
            Toast.makeText(this, "Unable to start AutoDim: ${error.message}", Toast.LENGTH_LONG).show()
        } finally {
            pendingEnable = false
        }
    }
}

