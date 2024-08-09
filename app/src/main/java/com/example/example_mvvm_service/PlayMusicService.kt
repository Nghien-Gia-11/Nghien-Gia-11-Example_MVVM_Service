package com.example.example_mvvm_service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayMusicService : Service() {

    companion object {
        const val PLAY = "PLAY"
        const val NEXT = "NEXT"
        const val BACK = "BACK"
        const val INCREASE = "INCREASE"
        const val DECREASE = "DECREASE"
        var isplaying = false
    }


    private val _musicStateFlow = MutableStateFlow("PAUSE")
    val musicStateFlow: StateFlow<String> get() = _musicStateFlow
    private lateinit var listSong: List<Song>
    private var current = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var notificationLayout: RemoteViews
    fun setSongList(songs: List<Song>) {
        listSong = songs
        if (mediaPlayer == null) {
            initMediaPlayer()
        }
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this@PlayMusicService, listSong[current].song)
        mediaPlayer?.setOnCompletionListener {
            _musicStateFlow.value = "NEXT"
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return PlayMusicBinder()
    }

    inner class PlayMusicBinder : Binder() {
        fun getService(): PlayMusicService = this@PlayMusicService
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotifyChannel()
        startNotify()
        startForeground(1, startNotify())
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PLAY -> {
                playOrPause()
                isplaying = !isplaying
            }
            BACK -> back()
            NEXT -> next()
            INCREASE -> increaseVolume()
            DECREASE -> decreaseVolume()
            }
        updateNotification()
        return START_STICKY
    }

    private fun playOrPause() {
        if (isplaying) {
            pause()
        } else {
            play()
        }
    }

    fun increaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
        _musicStateFlow.value = INCREASE
    }

    fun decreaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
        _musicStateFlow.value = DECREASE
    }

    fun pause() {
        mediaPlayer?.pause()
        _musicStateFlow.value = PLAY
        isplaying = !isplaying
    }

    fun next() {
        mediaPlayer?.stop()
        current = (current + 1) % listSong.size
        initMediaPlayer()
        play()
    }

    fun back() {
        mediaPlayer?.stop()
        current = (current + 1) % listSong.size
        initMediaPlayer()
        play()
    }

    fun play() {
        mediaPlayer?.start()
        _musicStateFlow.value = PLAY
        isplaying = !isplaying
    }

    private fun createNotifyChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "user_input_notification_channel",
                "Trình nghe nhạc",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startNotify(): Notification {
        val playIntent = Intent(this, PlayMusicService::class.java).apply { action = PLAY }


        val playPendingIntent = PendingIntent.getService(
            this,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val backIntent = Intent(this, PlayMusicService::class.java).apply { action = BACK }
        val backPendingIntent = PendingIntent.getService(
            this,
            1,
            backIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val nextIntent = Intent(this, PlayMusicService::class.java).apply { action = NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val decreaseIntent =
            Intent(this, PlayMusicService::class.java).apply { action = DECREASE }
        val decreasePendingIntent = PendingIntent.getService(
            this,
            4,
            decreaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val increaseIntent =
            Intent(this, PlayMusicService::class.java).apply { action = INCREASE }
        val increasePendingIntent = PendingIntent.getService(
            this,
            5,
            increaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationLayout = RemoteViews(packageName, R.layout.layout_notify).apply {
            setOnClickPendingIntent(R.id.btn_Play, playPendingIntent)
            setOnClickPendingIntent(R.id.btnBack, backPendingIntent)
            setOnClickPendingIntent(R.id.btnNext, nextPendingIntent)
            setOnClickPendingIntent(R.id.btnDecreaseVolume, decreasePendingIntent)
            setOnClickPendingIntent(R.id.btnIncreaseVolume, increasePendingIntent)
            val icon = if (isplaying) R.drawable.pause else R.drawable.play
            setImageViewResource(R.id.btn_Play, icon)
        }


        return NotificationCompat.Builder(this, "user_input_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomBigContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, startNotify())
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
