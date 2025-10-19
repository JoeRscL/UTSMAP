package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityLoginBinding // Import binding class
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding // Declare binding variable
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater) // Inflate layout
        setContentView(binding.root) // Set content view

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener { // Access view via binding
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Silakan isi email dan password terlebih dahulu", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, ScheduleActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            when (val exception = task.exception) {
                                is FirebaseAuthInvalidUserException -> {
                                    Toast.makeText(this, "Email belum terdaftar, silakan daftar dulu", Toast.LENGTH_SHORT).show()
                                }
                                is FirebaseAuthInvalidCredentialsException -> {
                                    Toast.makeText(this, "Password salah", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(this, "Login gagal: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            }
        }

        binding.btnRegister.setOnClickListener { // Access view via binding
            startActivity(Intent(this, register::class.java))
        }
    }
}
