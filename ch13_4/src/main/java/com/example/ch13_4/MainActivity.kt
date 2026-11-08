package com.example.ch13_4

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ch13_4.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channel = Channel<Long>()

        lifecycleScope.launch(Dispatchers.Main) {
            channel.consumeEach { result ->
                binding.resultView.text = "결과: $result"
                binding.calcButton.isEnabled = true
            }
        }

        binding.calcButton.setOnClickListener {
            binding.calcButton.isEnabled = false
            binding.resultView.text = "계산 중..."

            lifecycleScope.launch(Dispatchers.Default) {
                var sum = 0L
                var time = measureTimeMillis {
                    for (i in 1..2_000_000_000L) {
                        sum += i
                    }
                }
                Log.d("choe", "time : $time")

                channel.send(sum)
            }
        }
    }
}