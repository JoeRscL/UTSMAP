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

            // 1. Hapus sesi login sebelumnya (Logout paksa)
            // Ini memastikan user harus login ulang setiap kali membuka aplikasi
            FirebaseAuth.getInstance().signOut()

            // 2. Selalu arahkan ke halaman Login
            // Dari halaman Login, user bisa memilih untuk Register jika belum punya akun
            val intent = Intent(this, login::class.java)
            startActivity(intent)

            // 3. Tutup SplashActivity agar tidak bisa kembali dengan tombol back
            finish()

        }, 3000) // Tunda 3 detik
    }
}