package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ScreenshotDatabase
import com.example.data.model.ScreenshotEvent
import com.example.data.preferences.AppPreferences
import com.example.network.DiscordWebhookSender
import com.example.network.WebhookResult
import com.example.util.DateTimeUtil
import com.example.util.ImageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScreenshotListenerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var screenshotObserver: ScreenshotObserver? = null

    companion object {
        const val ACTION_START = "com.example.shotcord.ACTION_START"
        const val ACTION_STOP = "com.example.shotcord.ACTION_STOP"
        const val ACTION_MANUAL_CAPTURE = "com.example.shotcord.ACTION_MANUAL_CAPTURE"
        const val NOTIFICATION_CHANNEL_SERVICE = "shotcord_screenshot_listener"
        const val NOTIFICATION_CHANNEL_ALERTS = "shotcord_screenshot_alerts"
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_ALERT = 2002

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotListenerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenshotListenerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerManualCapture(context: Context) {
            val intent = Intent(context, ScreenshotListenerService::class.java).apply {
                action = ACTION_MANUAL_CAPTURE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    val prefs = AppPreferences(applicationContext)
                    prefs.setServiceActive(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_MANUAL_CAPTURE -> {
                serviceScope.launch {
                    val media = ScreenshotObserver.queryLatestScreenshot(applicationContext)
                    if (media != null) {
                        handleScreenshotDetected(
                            media.uri,
                            media.displayName,
                            media.sizeBytes,
                            media.resolution,
                            media.timestampEpoch
                        )
                    }
                }
            }
            else -> {
                startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification("Active"))
                registerObserver()
                serviceScope.launch {
                    val prefs = AppPreferences(applicationContext)
                    prefs.setServiceActive(true)
                }
            }
        }
        return START_STICKY
    }

    private fun registerObserver() {
        if (screenshotObserver != null) return

        screenshotObserver = ScreenshotObserver(applicationContext) { uri, name, size, resolution, epochMillis ->
            handleScreenshotDetected(uri, name, size, resolution, epochMillis)
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenshotObserver!!
            )
        } catch (_: Exception) {}
    }

    private fun handleScreenshotDetected(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        resolution: String,
        epochMillis: Long
    ) {
        serviceScope.launch {
            val prefs = AppPreferences(applicationContext)
            val database = ScreenshotDatabase.getInstance(applicationContext)
            val dao = database.screenshotDao()

            val webhookUrl = prefs.webhookUrlFlow.first()
            val userName = prefs.userNameFlow.first()
            val autoSend = prefs.autoSendFlow.first()
            val quality = prefs.compressQualityFlow.first()

            val phTime = DateTimeUtil.getPhilippineTimeFormatted(epochMillis)
            val thumbnailBytes = ImageUtil.loadScaledThumbnail(
                context = applicationContext,
                uri = uri,
                maxDimension = 1080,
                quality = quality
            )

            val event = ScreenshotEvent(
                timestampEpoch = epochMillis,
                phFormattedTime = phTime,
                capturedByUser = userName,
                imageUri = uri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                resolution = resolution,
                status = ScreenshotEvent.STATUS_PENDING,
                thumbnailBytes = thumbnailBytes
            )

            val insertedId = dao.insert(event)

            if (autoSend && webhookUrl.isNotBlank()) {
                val result = DiscordWebhookSender.sendScreenshotWebhook(
                    webhookUrl = webhookUrl,
                    phFormattedTime = phTime,
                    capturedByUser = userName,
                    fileName = fileName,
                    resolution = resolution,
                    fileSizeBytes = fileSize,
                    thumbnailBytes = thumbnailBytes
                )

                val updatedEvent = when (result) {
                    is WebhookResult.Success -> {
                        event.copy(
                            id = insertedId,
                            status = ScreenshotEvent.STATUS_SUCCESS,
                            httpCode = result.code,
                            errorMessage = null
                        )
                    }
                    is WebhookResult.Failure -> {
                        event.copy(
                            id = insertedId,
                            status = ScreenshotEvent.STATUS_FAILED,
                            httpCode = result.code,
                            errorMessage = result.message
                        )
                    }
                }
                dao.update(updatedEvent)

                if (result is WebhookResult.Success) {
                    sendDeviceAlertNotification(fileName, phTime, userName, thumbnailBytes)
                }
            }
        }
    }

    private fun sendDeviceAlertNotification(
        fileName: String,
        phTime: String,
        userName: String,
        thumbnailBytes: ByteArray?
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeBitmap = if (thumbnailBytes != null && thumbnailBytes.isNotEmpty()) {
            try {
                BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
            } catch (_: Exception) { null }
        } else null

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ALERTS)
            .setContentTitle("📸 Screenshot Sent to Discord")
            .setContentText("$userName • $phTime")
            .setSubText("Asia/Manila (UTC+8)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                if (largeBitmap != null) {
                    setLargeIcon(largeBitmap)
                    setStyle(NotificationCompat.BigPictureStyle().bigPicture(largeBitmap).bigLargeIcon(null as android.graphics.Bitmap?))
                }
            }
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt() + 2000, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_SERVICE,
                "ShotCord Background Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors screenshot captures"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ALERTS,
                "Screenshot Relay Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when screenshots are sent to Discord"
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    private fun buildServiceNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenshotListenerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SERVICE)
            .setContentTitle("ShotCord")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_pause, "Stop", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        screenshotObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
            screenshotObserver = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
