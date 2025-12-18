package com.example.mindnestapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Tambahkan Log untuk debugging
        Log.d("AlarmReceiver", "Alarm received!")

        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Ada tugas yang menunggu!"
        val taskId = intent.getStringExtra("TASK_ID") ?: System.currentTimeMillis().toString()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk notifikasi pengingat tugas."
                enableVibration(true) // Aktifkan getaran untuk channel ini
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_mindnest_logo)
            .setContentTitle("Pengingat Tugas")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Aktifkan suara, getaran, dan lampu default
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
        Log.d("AlarmReceiver", "Notification shown for task: $taskTitle")
    }
}
