package com.avr.volumeslider.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.avr.volumeslider.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VolumeControlService : Service() {
    private lateinit var audioManager: AudioManager
    private val binder = LocalBinder()
    private lateinit var notificationManager: NotificationManager

    private val _currentVolume = MutableStateFlow(0)
    val currentVolume = _currentVolume.asStateFlow()

    companion object {
        const val CHANNEL_ID = "VolumeControlChannel"
        const val NOTIFICATION_ID = 1
    }

    inner class LocalBinder : Binder() {
        fun getService(): VolumeControlService = this@VolumeControlService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        updateVolume()
        startForegroundService()
    }

    private fun updateVolume() {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _currentVolume.value = volume
    }

    fun increaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
        updateVolume()
        updateNotification()
    }

    fun decreaseVolume() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        updateVolume()
        updateNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Volume Control"
            val channelDescription = "Background volume control service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createVolumeControlNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Volume Up Intent
        val volumeUpIntent = Intent(this, VolumeControlReceiver::class.java).apply {
            action = "VOLUME_UP"
        }
        val volumeUpPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            volumeUpIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Volume Down Intent
        val volumeDownIntent = Intent(this, VolumeControlReceiver::class.java).apply {
            action = "VOLUME_DOWN"
        }
        val volumeDownPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            volumeDownIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Control")
            .setContentText("Current Volume: ${_currentVolume.value}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.arrow_up_float, "Volume Up", volumeUpPendingIntent)
            .addAction(android.R.drawable.arrow_down_float, "Volume Down", volumeDownPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val updatedNotification = createVolumeControlNotification()
        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
    }

    private fun startForegroundService() {
        val notification = createVolumeControlNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}