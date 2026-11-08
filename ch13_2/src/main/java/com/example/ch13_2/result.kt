package com.example.ch13_2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ch13_2.databinding.ActivityResultBinding

class result : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var inIntent = intent
        var hapValue = inIntent.getIntExtra("Num1", 0) + inIntent.getIntExtra("Num2", 0)

        binding.btnReturn.setOnClickListener {
            var outIntent = Intent().apply {
                putExtra("Hap", hapValue)
            }
            setResult(Activity.RESULT_OK, outIntent)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}