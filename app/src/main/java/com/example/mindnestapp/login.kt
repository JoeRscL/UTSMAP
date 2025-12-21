package com.example.mindnestapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    // Konstanta untuk SharedPreferences
    companion object {
        const val PREFS_NAME = "MindNestPrefs"
        const val KEY_EMAIL = "saved_email"
        const val KEY_IS_REMEMBERED = "is_remembered"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // --- LOGIC REMEMBER ME (LOAD DATA) ---
        // Cek apakah data tersimpan sebelumnya
        val isRemembered = sharedPreferences.getBoolean(KEY_IS_REMEMBERED, false)
        if (isRemembered) {
            val savedEmail = sharedPreferences.getString(KEY_EMAIL, "")
            binding.etEmail.setText(savedEmail)
            binding.cbRememberMe.isChecked = true
        }
        // -------------------------------------

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 1. Validasi Input Kosong
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- LOGIC REMEMBER ME (SAVE DATA) ---
            val editor = sharedPreferences.edit()
            if (binding.cbRememberMe.isChecked) {
                // Jika dicentang, simpan email dan status boolean
                editor.putString(KEY_EMAIL, email)
                editor.putBoolean(KEY_IS_REMEMBERED, true)
            } else {
                // Jika tidak dicentang, hapus data
                editor.remove(KEY_EMAIL)
                editor.putBoolean(KEY_IS_REMEMBERED, false)
            }
            editor.apply() // Simpan perubahan
            // -------------------------------------

            // 2. MULAI LOADING
            binding.progressBar.visibility = View.INVISIBLE // Ubah ke VISIBLE (sebelumnya INVISIBLE salah context jika logic loading)
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Loading..."

            // 3. Proses Login ke Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    // 4. SELESAI LOADING
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ScheduleActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Login gagal, silakan coba lagi"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, register::class.java))
        }
    }
}