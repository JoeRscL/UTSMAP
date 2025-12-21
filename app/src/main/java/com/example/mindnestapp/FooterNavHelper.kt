package com.example.mindnestapp

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast

class FooterNavHelper(private val activity: Activity) {

    fun setupFooterNavigation() {
        // Menggunakan ? (nullable) agar tidak crash jika ID tidak ditemukan di layout saat ini
        val navHome = activity.findViewById<LinearLayout?>(R.id.navHome)
        val navCalendar = activity.findViewById<LinearLayout?>(R.id.navCalendar)
        val navAdd = activity.findViewById<LinearLayout?>(R.id.navAdd)
        val navProfile = activity.findViewById<LinearLayout?>(R.id.navProfile)
        val navMood = activity.findViewById<LinearLayout?>(R.id.navMood)

        // --- SETUP LISTENERS ---
        // Gunakan operator ?.setOnClickListener

        navHome?.setOnClickListener {
            if (activity !is ScheduleActivity) { // Asumsi Home diarahkan ke ScheduleActivity
                val intent = Intent(activity, ScheduleActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }

        navCalendar?.setOnClickListener {
            if (activity !is ViewScheduleActivity) {
                val intent = Intent(activity, ViewScheduleActivity::class.java)
                activity.startActivity(intent)
            }
        }

        navAdd?.setOnClickListener {
            if (activity is AddTaskActivity) {
                // Logika khusus jika sedang di halaman AddTask (Mode Edit vs Baru)
                val isEditMode = activity.intent.getBooleanExtra("isEditMode", false)
                if (isEditMode) {
                    val intent = Intent(activity, AddTaskActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    activity.startActivity(intent)
                    activity.finish()
                }
            } else {
                val intent = Intent(activity, AddTaskActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // Logic Tombol Mood (AI Check)
        navMood?.setOnClickListener {
            // Cek apakah class MoodCheckActivity benar-benar ada
            try {
                if (activity !is MoodCheckActivity) {
                    val intent = Intent(activity, MoodCheckActivity::class.java)
                    activity.startActivity(intent)
                }
            } catch (e: Exception) {
                // Tampilkan pesan error jika Activity belum dibuat/gagal dibuka
                Toast.makeText(activity, "Error membuka Mood Check: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                val intent = Intent(activity, ProfileActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}