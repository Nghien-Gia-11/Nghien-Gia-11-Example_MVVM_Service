package com.example.example_mvvm_service

import android.annotation.SuppressLint
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
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayMusicService : Service() {

    companion object {
        const val PLAY = "PLAY"
        const val PAUSE = "PAUSE"
        const val NEXT = "NEXT"
        const val BACK = "BACK"
        const val INCREASE = "INCREASE"
        const val DECREASE = "DECREASE"
    }

    private val _musicStateFlow = MutableStateFlow(PAUSE)
    val musicStateFlow: StateFlow<String> get() = _musicStateFlow // flow quản lý state chơi nhạc

    private val _currentTimeSong = MutableStateFlow(0)
    val currentTimeSong: StateFlow<Int> get() = _currentTimeSong // flow quản lý thời gian thực chạy nhạc

    private val _timeSong = MutableStateFlow(0) // flow quản lý thời gian của bài hát
    val timeSong: StateFlow<Int> get() = _timeSong

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var timeUpdateJob: Job? = null // job quản lý lấy thời gian thực nhạc đang phát

    private lateinit var listSong: List<Song>
    private var current = 0
    var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var notificationLayout: RemoteViews
    private lateinit var notificationBuilder: NotificationCompat.Builder


    fun setSongList(songs: List<Song>) {
        listSong = songs
        if (mediaPlayer == null) {
            initMediaPlayer()
        }
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this@PlayMusicService, listSong[current].song)
        mediaPlayer?.setOnCompletionListener {
            _musicStateFlow.value = NEXT
            _currentTimeSong.value = mediaPlayer?.currentPosition ?: 0
            next()
        }
        startSeekbar()
        // set thời gian bài hát
        _timeSong.value = listSong[current].time
    }

    private fun startSeekbar() {
        // bắt đầu đếm thời gian thực
        timeUpdateJob = coroutineScope.launch {
            while (isActive) {
                delay(1000)
                _currentTimeSong.value = mediaPlayer?.currentPosition ?: 0
            }
        }
    }

    private fun stopSeekbar() {
        timeUpdateJob?.cancel()
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
        notificationBuilder = NotificationCompat.Builder(this, "user_input_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomBigContentView(createNotificationLayout())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        startForeground(1, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PlayMusicService", "Received action: ${intent?.action}")
        when (intent?.action) {
            PLAY -> {
                Log.d("PlayMusicService", "Action: PLAY")
                play()
            }
            PAUSE -> {
                Log.d("PlayMusicService", "Action: PAUSE")
                pause()
            }
            BACK -> back()
            NEXT -> next()
            INCREASE -> increaseVolume()
            DECREASE -> decreaseVolume()
        }
        return START_STICKY
    }

    fun increaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
        _musicStateFlow.value = INCREASE
    }

    fun decreaseVolume() {
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
        _musicStateFlow.value = DECREASE
    }

    fun play() {
        _musicStateFlow.value = PLAY
        mediaPlayer?.start()
        updateNotification()
    }

    fun pause() {
        _musicStateFlow.value = PAUSE
        mediaPlayer?.pause()
        updateNotification()
    }

    fun next() {
        mediaPlayer?.stop()
        current = (current + 1) % listSong.size
        initMediaPlayer()
        play()
    }

    fun back() {
        mediaPlayer?.stop()
        current = (current - 1 + listSong.size) % listSong.size
        initMediaPlayer()
        play()
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

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotificationLayout(): RemoteViews {
        val playAction = if (_musicStateFlow.value == PLAY) PAUSE else PLAY
        val playIntent = Intent(this, PlayMusicService::class.java).apply { action = playAction }
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

        val decreaseIntent = Intent(this, PlayMusicService::class.java).apply { action = DECREASE }
        val decreasePendingIntent = PendingIntent.getService(
            this,
            4,
            decreaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val increaseIntent = Intent(this, PlayMusicService::class.java).apply { action = INCREASE }
        val increasePendingIntent = PendingIntent.getService(
            this,
            5,
            increaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(packageName, R.layout.layout_notify).apply {
            setOnClickPendingIntent(R.id.btn_Play, playPendingIntent)
            setOnClickPendingIntent(R.id.btnBack, backPendingIntent)
            setOnClickPendingIntent(R.id.btnNext, nextPendingIntent)
            setOnClickPendingIntent(R.id.btnDecreaseVolume, decreasePendingIntent)
            setOnClickPendingIntent(R.id.btnIncreaseVolume, increasePendingIntent)
        }
    }

    private fun updateNotification() {
        val icon = when (_musicStateFlow.value) {
            PLAY -> R.drawable.pause
            PAUSE -> R.drawable.play
            else -> R.drawable.play // Fallback icon
        }
        Log.d("PlayMusicService", "Updating notification icon to: $icon")
        notificationLayout = createNotificationLayout().apply {
            setImageViewResource(R.id.btn_Play, icon)
        }
        notificationBuilder.setCustomBigContentView(notificationLayout)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSeekbar()
        mediaPlayer?.release()
    }
}
