package com.example.ch17_2

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ch17_2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var myHelper: myDBHelper
    lateinit var sqlDB: SQLiteDatabase

    val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "가수 그룹 관리 DB (수정)"

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        myHelper = myDBHelper(this)

        binding.btnInit.setOnClickListener {
            sqlDB = myHelper.writableDatabase
            myHelper.onUpgrade(sqlDB, 1, 2)
            sqlDB.close()
            Toast.makeText(applicationContext, "초기화됨", Toast.LENGTH_SHORT).show()
            binding.btnSelect.callOnClick()
        }

        binding.btnInsert.setOnClickListener {
            sqlDB = myHelper.writableDatabase
            sqlDB.execSQL("INSERT INTO groupTBL VALUES ( '" +
                    binding.edtName.text.toString() + "' , " +
                    binding.edtNumber.text.toString() + ");")
            sqlDB.close()
            Toast.makeText(applicationContext, "입력됨", Toast.LENGTH_SHORT).show()
            binding.btnSelect.callOnClick()
        }

        binding.btnUpdate.setOnClickListener {
            sqlDB = myHelper.writableDatabase
            sqlDB.execSQL("UPDATE groupTBL SET gNumber = " +
                    binding.edtNumber.text.toString() + " WHERE gName = '" +
                    binding.edtName.text.toString() + "';")
            sqlDB.close()
            Toast.makeText(applicationContext, "수정됨", Toast.LENGTH_SHORT).show()
            binding.btnSelect.callOnClick()
        }

        binding.btnDelete.setOnClickListener {
            sqlDB = myHelper.writableDatabase
            sqlDB.execSQL("DELETE FROM groupTBL WHERE gName = '" +
                    binding.edtName.text.toString() + "';")
            sqlDB.close()
            Toast.makeText(applicationContext, "삭제됨", Toast.LENGTH_SHORT).show()
            binding.btnSelect.callOnClick()
        }

        binding.btnSelect.setOnClickListener {
            sqlDB = myHelper.readableDatabase
            var cursor: Cursor
            cursor = sqlDB.rawQuery("SELECT * FROM groupTBL;", null)

            var strNames = "그룹이름" + "\r\n" + "--------" + "\r\n"
            var strNumbers = "인원" + "\r\n" + "--------" + "\r\n"

            while (cursor.moveToNext()) {
                strNames += cursor.getString(0) + "\r\n"
                strNumbers += cursor.getString(1) + "\r\n"
            }

            binding.tvNameResult.setText(strNames)
            binding.tvNumberResult.setText(strNumbers)

            cursor.close()
            sqlDB.close()
        }
    }

    inner class myDBHelper(context: Context) : SQLiteOpenHelper(context, "groupDB", null, 1) {
        override fun onCreate(p0: SQLiteDatabase?) {
            p0!!.execSQL("CREATE TABLE groupTBL ( gName CHAR(20) PRIMARY KEY, gNumber INTEGER);")
        }

        override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
            p0!!.execSQL("DROP TABLE IF EXISTS groupTBL")
            onCreate(p0)
        }
    }
}