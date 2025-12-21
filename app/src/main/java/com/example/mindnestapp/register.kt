package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Tombol Daftar
        binding.btnDaftar.setOnClickListener {
            // 1. AMBIL INPUT DATA BARU
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // 2. VALIDASI INPUT
            // Pastikan nama depan dan belakang diisi
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Silakan isi Nama Depan dan Belakang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Silakan isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password dan konfirmasi tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Daftar user ke Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        if (userId != null) {
                            // 3. SIMPAN STRUKTUR DATA LENGKAP KE REALTIME DATABASE
                            // Kita simpan field kosong untuk data lain agar di ProfileActivity tidak null
                            val userProfile = hashMapOf(
                                "userId" to userId,
                                "email" to email,
                                "firstName" to firstName,  // Data baru
                                "lastName" to lastName,    // Data baru
                                "phoneNumber" to "",       // Default kosong
                                "gender" to "",           // Default kosong
                                "dateOfBirth" to "",      // Default kosong
                                "profileImageBase64" to "", // Default kosong
                                "createdAt" to System.currentTimeMillis()
                            )

                            database.reference.child("users").child(userId)
                                .setValue(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registrasi berhasil! Silakan login.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Pindah ke halaman login
                                    val intent = Intent(this, login::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Gagal menyimpan data profile: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Registrasi gagal: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Tombol Login (langsung ke halaman login)
//        binding.tvLogin.setOnClickListener {
//            startActivity(Intent(this, login::class.java))
//            finish()
//        }
    }
}