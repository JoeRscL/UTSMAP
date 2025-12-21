package com.example.mindnestapp

import android.content.Context
import android.util.Log
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextClassificationHelper(
    private val context: Context,
    private val listener: TextResultsListener
) {

    private var classifier: NLClassifier? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        initClassifier()
    }

    private fun initClassifier() {
        try {
            // Memuat model dari assets
            classifier = NLClassifier.createFromFile(context, "text_classifier.tflite")
        } catch (e: IOException) {
            Log.e("TextClassification", "Gagal memuat model", e)
        }
    }

    fun classify(text: String) {
        if (classifier == null) {
            initClassifier()
        }

        executor.execute {
            // Proses klasifikasi (Positive/Negative)
            val results = classifier?.classify(text) ?: emptyList()

            // Mencari hasil dengan skor tertinggi
            if (results.isNotEmpty()) {
                val bestCategory = results.maxByOrNull { it.score }
                listener.onResult(bestCategory?.label ?: "Unknown", bestCategory?.score ?: 0f)
            } else {
                listener.onError("Gagal mengklasifikasi")
            }
        }
    }

    interface TextResultsListener {
        fun onResult(label: String, score: Float)
        fun onError(error: String)
    }
}