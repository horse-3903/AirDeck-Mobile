package io.github.kitswas.virtualgamepadmobile.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.kitswas.virtualgamepadmobile.MainActivity
import io.github.kitswas.virtualgamepadmobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Keeps the connection to the PC alive while the app is in the background or the screen is off,
 * and exposes Stop / Connect buttons in a persistent notification.
 */
class ConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observing = false
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val connection = ConnectionHub.get(applicationContext)
        when (intent?.action) {
            ACTION_STOP -> connection.disconnect()
            ACTION_CONNECT -> {
                if (connection.lastIpAddress.isNotBlank()) {
                    connection.connect(connection.lastIpAddress, connection.lastPort)
                }
            }

            ACTION_CLOSE -> {
                connection.disconnect()
                releaseLocks()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(connection),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        acquireLocks()

        if (!observing) {
            observing = true
            scope.launch {
                connection.uiState.collect {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(connection))
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseLocks()
        scope.cancel()
        super.onDestroy()
    }

    private fun acquireLocks() {
        if (wifiLock == null) {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "vgp:wifi")
                .apply { setReferenceCounted(false); acquire() }
        }
        if (wakeLock == null) {
            val power = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vgp:connection")
                .apply { setReferenceCounted(false); acquire() }
        }
    }

    private fun releaseLocks() {
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wifiLock = null
        wakeLock = null
    }

    private fun servicePending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, ConnectionService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(connection: ConnectionViewModel): Notification {
        val state = connection.uiState.value
        val text = when {
            state.connected -> {
                val slot = state.controllerSlot?.let {
                    getString(R.string.notification_controller, it)
                }
                listOfNotNull(
                    getString(R.string.notification_connected, state.ipAddress, state.port),
                    slot
                ).joinToString("  |  ")
            }

            state.isConnecting -> getString(R.string.notification_connecting)
            state.error != null -> getString(R.string.notification_error, state.error)
            else -> getString(R.string.notification_disconnected)
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (state.connected || state.isConnecting) {
            builder.addAction(0, getString(R.string.notification_stop), servicePending(ACTION_STOP, 1))
        } else {
            if (connection.lastIpAddress.isNotBlank()) {
                builder.addAction(
                    0, getString(R.string.notification_connect), servicePending(ACTION_CONNECT, 2)
                )
            }
            builder.addAction(0, getString(R.string.notification_close), servicePending(ACTION_CLOSE, 3))
        }
        return builder.build()
    }

    companion object {
        const val ACTION_START = "io.github.kitswas.virtualgamepadmobile.action.START"
        const val ACTION_STOP = "io.github.kitswas.virtualgamepadmobile.action.STOP"
        const val ACTION_CONNECT = "io.github.kitswas.virtualgamepadmobile.action.CONNECT"
        const val ACTION_CLOSE = "io.github.kitswas.virtualgamepadmobile.action.CLOSE"
        private const val CHANNEL_ID = "connection"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            androidx.core.content.ContextCompat.startForegroundService(
                context,
                Intent(context, ConnectionService::class.java).setAction(ACTION_START)
            )
        }

        fun close(context: Context) {
            context.startService(
                Intent(context, ConnectionService::class.java).setAction(ACTION_CLOSE)
            )
        }
    }
}
