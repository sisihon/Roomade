package com.example.ch13_3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ch13_3.databinding.ActivityInputBinding

class Input : AppCompatActivity() {
    private lateinit var binding: ActivityInputBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnDeliver.setOnClickListener {
            val inputText = binding.editInput.text.toString()
            val outIntent = Intent()
            outIntent.putExtra("userInput", inputText)
            setResult(RESULT_OK, outIntent)
            finish()
        }
    }
}