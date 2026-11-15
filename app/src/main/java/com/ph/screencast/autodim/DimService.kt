package com.ph.screencast.autodim

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRouter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Suppress("DEPRECATION")
class DimService : Service() {
    private lateinit var router: MediaRouter
    private lateinit var brightness: BrightnessController
    private val callback = object : MediaRouter.SimpleCallback() {
        override fun onRouteSelected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) = checkRoute()
        override fun onRouteUnselected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) = checkRoute()
        override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) = checkRoute()
        override fun onRouteRemoved(router: MediaRouter, info: MediaRouter.RouteInfo) = checkRoute()
    }

    override fun onCreate() {
        super.onCreate()
        brightness = BrightnessController(this)
        val channel = NotificationChannel(CHANNEL, "Cast monitoring", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, DimService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("AutoDim is monitoring casting")
            .setContentText("Automatically dim while connected to a TV")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Turn off", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(1, notification)
        router = getSystemService(MediaRouter::class.java)
        router.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, callback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        AutoDimTileService.refresh(this)
        // Check once on start; subsequent updates arrive through MediaRouter callbacks.
        checkRoute()
        return START_STICKY
    }

    private fun checkRoute() {
        runCatching {
            check(Settings.System.canWrite(this)) { "System settings permission is required. Open AutoDim to grant access." }
            val route = router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
            val casting = route != null && route != router.defaultRoute &&
                route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_VIDEO != 0 && !route.isConnecting
            if (casting) {
                brightness.dim()
                status("Casting to ${route.name}. Brightness set to minimum.")
            } else {
                brightness.restore()
                status("Waiting for Screen Cast. If already connected, the firmware may not expose the cast route.")
            }
        }.onFailure { status("Error: ${it.message}") }
    }

    private fun status(value: String) {
        val preferences = getSharedPreferences("state", MODE_PRIVATE)
        if (preferences.getString("status", null) != value) {
            preferences.edit().putString("status", value).apply()
        }
    }

    override fun onDestroy() {
        if (::router.isInitialized) router.removeCallback(callback)
        restore(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isRunning = false
        AutoDimTileService.refresh(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning by mutableStateOf(false)
            private set

        private const val CHANNEL = "cast"
        private const val STOP = "com.ph.screencast.autodim.STOP"

        fun restore(context: Context) {
            val result = runCatching { BrightnessController(context).restore() }
            context.getSharedPreferences("state", MODE_PRIVATE).edit().putString(
                "status", result.fold(
                    onSuccess = { "Monitoring stopped and brightness restored." },
                    onFailure = { "Unable to restore brightness: ${it.message}" }
                )
            ).apply()
        }
    }
}
