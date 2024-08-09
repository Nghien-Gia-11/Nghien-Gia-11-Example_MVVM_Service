package com.example.example_mvvm_service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.example_mvvm_service.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private lateinit var binding : ActivityMainBinding
    private lateinit var mediaPlayer: MediaPlayer
    private var connect = false
    private var playMusicService: PlayMusicService? = null


    private val viewModel: SongViewModel by viewModels()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayMusicService.PlayMusicBinder
            playMusicService = binder.getService()
            /*observeMusicState()*/
            connect = true
            viewModel.song.value?.let { playMusicService?.setSongList(it)}
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connect = false
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Intent(this, PlayMusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        binding.btnPlay.setOnClickListener {
            if (PlayMusicService.isplaying){
                playMusicService?.pause()
            }else{
                playMusicService?.play()
            }
        }
        binding.btnBack.setOnClickListener {
            playMusicService?.back()
        }
        binding.btnNext.setOnClickListener {
            playMusicService?.next()
        }

        binding.btnDecreaseVolume.setOnClickListener {
            playMusicService?.decreaseVolume()
        }

        binding.btnIncreaseVolume.setOnClickListener {
            playMusicService?.increaseVolume()
        }
    }

    /*private fun observeMusicState() {
        lifecycleScope.launch {
            playMusicService?.musicStateFlow?.asLiveData()?.observe(this@MainActivity) { state ->
                when (state){
                    PlayMusicService.PLAY -> {
                        PlayMusicService.isplaying = !PlayMusicService.isplaying
                    }
                }
            }
        }
    }*/


    private fun setSeekBar(song: Song) {
        binding.seekBar.max = 100000
        lifecycleScope.launch {
            while (mediaPlayer.isPlaying) {
                binding.seekBar.progress = mediaPlayer.currentPosition
                delay(1000)
            }
        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        if (connect){
            unbindService(connection)
            connect = false
        }
    }

}