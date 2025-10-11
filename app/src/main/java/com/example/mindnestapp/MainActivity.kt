package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Langsung arahkan ke halaman register
        val intent = Intent(this, login::class.java)
        startActivity(intent)

        // Tutup MainActivity biar gak numpuk
        finish()
    }
}
