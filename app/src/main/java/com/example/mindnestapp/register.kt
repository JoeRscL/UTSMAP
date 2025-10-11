package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityRegisterBinding

class register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDaftar.setOnClickListener {
            // arahkan ke ScheduleActivity
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        binding.tvLogin.setOnClickListener {
            // arahkan ke login (bukan LoginActivity)
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }
    }
}
