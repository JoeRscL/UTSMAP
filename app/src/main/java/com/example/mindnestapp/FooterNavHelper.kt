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
        // **PERUBAHAN**: Menggunakan ID baru 'navProfile' dari layout footer
        val navProfile = activity.findViewById<LinearLayout>(R.id.navProfile)


        navHome.setOnClickListener {
            if (activity !is ScheduleActivity) {
                val intent = Intent(activity, ScheduleActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
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
            if (activity is AddTaskActivity) {
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

        // 4. Tombol Profile (di footer) -> Mengarah ke ProfileActivity
        navProfile.setOnClickListener {
            if (activity !is ProfileActivity) {
                val intent = Intent(activity, ProfileActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}