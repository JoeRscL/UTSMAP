package com.example.mindnestapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText

class MoodCheckActivity : AppCompatActivity(), TextClassificationHelper.TextResultsListener {

    private lateinit var helper: TextClassificationHelper
    private lateinit var tvResult: TextView
    private lateinit var etInput: TextInputEditText
    private lateinit var btnAnalyze: Button
    private lateinit var btnBack: ImageView
    private lateinit var cardResult: CardView
    private lateinit var footerHelper: FooterNavHelper // Helper Footer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_check)

        // Init Views
        tvResult = findViewById(R.id.tvResult)
        etInput = findViewById(R.id.etInputText)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnBack = findViewById(R.id.btnBack)
        cardResult = findViewById(R.id.cardResult)

        // 1. Setup Tombol Back
        btnBack.setOnClickListener {
            // Kembali ke halaman sebelumnya (atau ke Home jika dari navbar)
            onBackPressedDispatcher.onBackPressed()
        }

        // 2. Setup Footer Navigation
        footerHelper = FooterNavHelper(this)
        footerHelper.setupFooterNavigation()

        // 3. Init ML Helper
        helper = TextClassificationHelper(this, this)

        // 4. Aksi Tombol Analisis
        btnAnalyze.setOnClickListener {
            val text = etInput.text.toString()
            if (text.isNotEmpty()) {
                tvResult.text = "Menganalisis..."
                cardResult.visibility = View.VISIBLE // Tampilkan card
                helper.classify(text)
            } else {
                Toast.makeText(this, "Mohon isi teks terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hasil Sukses
    override fun onResult(label: String, score: Float) {
        runOnUiThread {
            val percentage = (score * 100).toInt()
            // Ubah teks berdasarkan hasil
            val resultText = if (label.equals("Positive", ignoreCase = true)) {
                "Positif / Bahagia 😊"
            } else {
                "Negatif / Sedih 😔"
            }

            tvResult.text = resultText

            // Opsional: Ubah warna card berdasarkan mood
            // if (label == "Positive") cardResult.setCardBackgroundColor(...)
        }
    }

    // Error
    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }
}