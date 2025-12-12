package com.example.mindnestapp

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast

class FooterNavHelper(private val activity: Activity) {

    fun setupFooterNavigation() {
        val navHome = activity.findViewById<LinearLayout>(R.id.navHome)
        val navCalendar = activity.findViewById<LinearLayout>(R.id.navCalendar)
        val navAdd = activity.findViewById<LinearLayout>(R.id.navAdd)

        val navSettings = activity.findViewById<LinearLayout>(R.id.navSettings)

        // ========================================================
        //          PERBAIKAN LOGIKA NAVIGASI FINAL
        // ========================================================

        // 1. Tombol Home -> Mengarah ke ScheduleActivity (Halaman Welcome)
        navHome.setOnClickListener {
            if (activity !is ScheduleActivity) {
                val intent = Intent(activity, ScheduleActivity::class.java)
                // Bendera ini membersihkan semua activity di atas ScheduleActivity.
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                activity.startActivity(intent)
                // JANGAN PANGGIL activity.finish() di sini.
                // FLAG_ACTIVITY_CLEAR_TOP sudah cukup untuk merapikan tumpukan.
            }
        }

        // 2. Tombol Calendar -> Mengarah ke ViewScheduleActivity (Kalender Dinamis)
        navCalendar.setOnClickListener {
            if (activity !is ViewScheduleActivity) {
                val intent = Intent(activity, ViewScheduleActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // 3. Tombol Add -> Mengarah ke AddTaskActivity
        navAdd.setOnClickListener {
            if (activity !is AddTaskActivity) {
                val intent = Intent(activity, AddTaskActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // 4. Tombol File -> Tetap menampilkan Toast


        // 5. Tombol Settings -> Mengarah ke ProfileActivity
        navSettings.setOnClickListener {
            if (activity !is ProfileActivity) {
                val intent = Intent(activity, ProfileActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}