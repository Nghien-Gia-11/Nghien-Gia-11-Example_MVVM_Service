package com.example.example_mvvm_service

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.example_mvvm_service.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private lateinit var binding : ActivityMainBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var listSong: List<Song>
    private lateinit var intentForeground: Intent
    private var currentSong = 0
    private var check = false
    private val viewModel: SongViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intentForeground = Intent(this, PlayMusicService::class.java)

        viewModel.song.observe(this) { item ->
            listSong = item
        }

        binding.btnPlay.setOnClickListener {
            playSong(0)
            binding.btnPlay.visibility = View.GONE
            binding.btnPause.visibility = View.VISIBLE
        }
        binding.btnBack.setOnClickListener {
            playSong(1)
        }
        binding.btnNext.setOnClickListener {
            playSong(2)
        }
        binding.btnPause.setOnClickListener {
            playSong(3)
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnPause.visibility = View.GONE
        }
    }

    private fun startService(song: Song, action : Int) {
        intentForeground.putExtra("user_input", song)
        intentForeground.putExtra("action", action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentForeground)
        } else {
            startService(intentForeground)
        }
        setSeekBar(song)
    }

    private fun playSong(selected: Int) {
        when (selected) {
            0 -> {
                if (check) {
                    mediaPlayer.start()
                    startSeekBarUpdate()
                } else {
                    startService(listSong[currentSong], 0)
                    check = false
                }
            }

            1 -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                currentSong = (currentSong - 1) % listSong.size
                startService(listSong[currentSong], 1)
            }

            2 -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                currentSong = (currentSong + 1) % listSong.size
                startService(listSong[currentSong], 2)
            }

            else -> {
                startService(listSong[currentSong], 3)
                mediaPlayer.pause()
                check = true
            }
        }
    }

    private fun setSeekBar(song: Song) {
        binding.seekBar.max = song.time
        mediaPlayer = MediaPlayer.create(this, song.song)
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            startSeekBarUpdate()
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

    private fun startSeekBarUpdate() {
        lifecycleScope.launch {
            while (mediaPlayer.isPlaying) {
                binding.seekBar.progress = mediaPlayer.currentPosition
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

}