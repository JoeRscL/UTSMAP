package com.example.mindnestapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Ada tugas yang menunggu!"
        val taskId = intent.getStringExtra("TASK_ID") ?: System.currentTimeMillis().toString() // Fallback ID

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat channel notifikasi untuk Android 8.0 (Oreo) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_reminder_channel", // ID Channel
                "Task Reminders",        // Nama Channel yang terlihat oleh user
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk notifikasi pengingat tugas."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Buat notifikasi
        val notification = NotificationCompat.Builder(context, "task_reminder_channel")
            .setSmallIcon(R.drawable.ic_mindnest_logo) // Pastikan Anda punya ikon ini di drawable
            .setContentTitle("Pengingat Tugas")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notifikasi hilang saat di-klik
            .build()

        // Tampilkan notifikasi dengan ID unik berdasarkan taskId
        notificationManager.notify(taskId.hashCode(), notification)
    }
}
