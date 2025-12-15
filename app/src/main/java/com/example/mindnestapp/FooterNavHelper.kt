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
            // Cek jika activity saat ini BUKAN ScheduleActivity agar tidak reload halaman yang sama
            if (activity !is ScheduleActivity) {
                val intent = Intent(activity, ScheduleActivity::class.java)
                // Membersihkan tumpukan activity di atas ScheduleActivity agar tombol Back berfungsi logis
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                // Opsional: finish activity saat ini jika ingin hanya ada satu activity aktif (tergantung UX yang diinginkan)
                // activity.finish() 
            }
        }

        // 2. Tombol Calendar -> Mengarah ke ViewScheduleActivity (Kalender Dinamis)
        navCalendar.setOnClickListener {
            if (activity !is ViewScheduleActivity) {
                val intent = Intent(activity, ViewScheduleActivity::class.java)
                // Tidak perlu CLEAR_TOP agar user bisa back ke Home
                activity.startActivity(intent)
            }
        }

        // 3. Tombol Add -> Mengarah ke AddTaskActivity
        navAdd.setOnClickListener {
            // Logic baru: Jika kita sedang di AddTaskActivity dan user klik Add, 
            // kita harus reset activity ini ke mode "Add New" alih-alih Edit Mode.
            // Atau cukup restart activity-nya tanpa extra.
            
            if (activity is AddTaskActivity) {
                // Jika sudah di AddTaskActivity, kita cek apakah sedang dalam mode edit?
                // Jika ya, kita reload activity ini sebagai halaman Add baru.
                val isEditMode = activity.intent.getBooleanExtra("isEditMode", false)
                if (isEditMode) {
                    val intent = Intent(activity, AddTaskActivity::class.java)
                    // Hapus extra agar menjadi mode Add baru
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    activity.startActivity(intent)
                    activity.finish() // Tutup halaman edit yang lama
                } else {
                    // Jika sudah di halaman Add (bukan edit), mungkin tidak perlu ngapa-ngapain
                    // atau beri feedback ke user.
                }
            } else {
                // Jika di halaman lain, buka AddTaskActivity biasa
                val intent = Intent(activity, AddTaskActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // 4. Tombol File -> Tetap menampilkan Toast
        // (Jika ada tombol File, tambahkan di sini)


        // 5. Tombol Settings -> Mengarah ke ProfileActivity
        navSettings.setOnClickListener {
            if (activity !is ProfileActivity) {
                val intent = Intent(activity, ProfileActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }
}