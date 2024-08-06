package com.example.example_mvvm_service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.example_mvvm_service.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener {
            val userInput = binding.editTextUserInput.text.toString()
            val intent = Intent(this, PlayMusicService::class.java)
            intent.putExtra("user_input", userInput)
            startService(intent)
        }
    }
}