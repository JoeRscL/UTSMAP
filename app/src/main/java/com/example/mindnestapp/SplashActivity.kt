package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Cek status login user
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User sudah login, langsung ke halaman utama
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
            } else {
                // User belum login, ke halaman login
                val intent = Intent(this, login::class.java)
                startActivity(intent)
            }
            finish() // Tutup SplashActivity
        }, 3000) // Tunda 3 detik
    }
}