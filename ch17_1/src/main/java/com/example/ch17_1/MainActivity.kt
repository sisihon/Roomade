package com.example.ch17_1

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ch17_1.databinding.ActivityMainBinding
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "파일 테스트"

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnWrite.setOnClickListener {
            var outFs: FileOutputStream = openFileOutput("file.txt", Context.MODE_PRIVATE)
            var str = "모바일 프로그래밍"
            outFs.write(str.toByteArray())
            outFs.close()
            Toast.makeText(applicationContext, "file.txt가 생성됨", Toast.LENGTH_SHORT).show()
        }

        binding.btnRead.setOnClickListener {
            try {
                var inFs: FileInputStream = openFileInput("file.txt")
                var txt = ByteArray(30)
                inFs.read(txt)
                var str = txt.toString(Charsets.UTF_8).trim { it <= ' ' }
                Toast.makeText(applicationContext, str, Toast.LENGTH_SHORT).show()
                inFs.close()
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "파일 없음", Toast.LENGTH_SHORT).show()
            }
        }
    }
}