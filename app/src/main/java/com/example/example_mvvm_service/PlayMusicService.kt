package com.example.example_mvvm_service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class PlayMusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        var instance: PlayMusicService? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setMedia()
        createNotifyChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userInput = intent?.getParcelableExtra<Song>("user_input")
        val selected = intent?.getIntExtra("action", -1)

        userInput?.let {
            startForeground(1, startNotify(it))
            startNotify(it)
            when (selected) {
                0 -> play(it)
                1 -> back(it)
                2 -> next(it)
                3 -> pause(it)
            }
        }
        return START_STICKY
    }


    @SuppressLint("ForegroundServiceType")
    private fun pause(song: Song) {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer!!.pause()
            startForeground(1, startNotify(song))
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun next(song: Song) {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer!!.stop()
            startForeground(1, startNotify(song))
        }
    }

    private fun back(song: Song) {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer!!.stop()
        }
    }

    private fun play(song: Song) {

    }

    private fun setMedia() {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(
            applicationContext,
            Uri.parse("android.resource://" + packageName + "/" + R.raw.baonhieuloaihoa)
        )
        mediaPlayer?.prepare()
        mediaPlayer?.start()
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
    private fun startNotify(song: Song) : Notification{
        val notificationLayout = RemoteViews(packageName, R.layout.layout_notify).apply {
            setTextViewText(R.id.tvNameSong, song.name)
        }

        val playIntent = Intent(this, PlayMusicService::class.java).apply { action = "play" }
        val playPendingIntent = PendingIntent.getService(
            this,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val backIntent = Intent(this, PlayMusicService::class.java).apply { action = "back" }
        val backPendingIntent = PendingIntent.getService(
            this,
            1,
            backIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val nextIntent = Intent(this, PlayMusicService::class.java).apply { action = "next" }
        val nextPendingIntent = PendingIntent.getService(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val pauseIntent = Intent(this, PlayMusicService::class.java).apply { action = "pause" }
        val pausePendingIntent = PendingIntent.getService(
            this,
            3,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationLayout.setOnClickPendingIntent(R.id.btnPlay, playPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.btnPause, pausePendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.btnBack, backPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.btnNext, nextPendingIntent)

        return NotificationCompat.Builder(this, "user_input_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()


    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
