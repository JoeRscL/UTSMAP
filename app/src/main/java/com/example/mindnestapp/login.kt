package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Ambil komponen dari layout
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // 🔹 Tombol LOGIN ditekan
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                // Tampilkan alert jika kosong
                Toast.makeText(
                    this,
                    "Silakan isi email dan password terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Jika sudah diisi, pindah ke ScheduleActivity
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
                finish() // Supaya user tidak bisa kembali ke login setelah login berhasil
            }
        }

        // 🔹 Tombol REGISTER (tidak diubah)
        btnRegister.setOnClickListener {
            val intent = Intent(this, register::class.java)
            startActivity(intent)
        }

        // 🔹 Biarkan layout adaptif ke sistem UI (status bar, dsb)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardLogin)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
