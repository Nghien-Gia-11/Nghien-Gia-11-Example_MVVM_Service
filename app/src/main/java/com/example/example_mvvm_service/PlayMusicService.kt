package com.example.example_mvvm_service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class PlayMusicService : Service() {

    companion object {
        var instance: PlayMusicService? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotifyChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userInput = intent?.getStringExtra("user_input")
        userInput?.let {
            startNotify(it)
        }
        return START_STICKY
    }

    private fun createNotifyChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "user_input_notification_channel",
                "Trình nghe nhạc",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startNotify(data: String) {
        val notificationLayout = RemoteViews(packageName, R.layout.layout_notify).apply {

        }

        val notification = NotificationCompat.Builder(this, "user_input_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
