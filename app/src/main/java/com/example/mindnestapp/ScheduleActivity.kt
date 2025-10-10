package com.example.mindnest

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.R

class ScheduleActivity : AppCompatActivity() {

    data class Schedule(
        val title: String,
        val date: String,
        val time: String,
        val priority: String,
        val category: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val scheduleContainer = findViewById<LinearLayout>(R.id.scheduleContainer)

        val scheduleList = listOf(
            Schedule("Meeting tim projek", "Sunday 2 Oct 2025", "12:00 AM - 17:00", "Medium", "Work"),
            Schedule("UI Review", "Monday 3 Oct 2025", "09:00 AM - 11:00", "High", "Design"),
            Schedule("Lunch with Client", "Monday 3 Oct 2025", "12:00 PM - 13:00", "Low", "Client"),
            Schedule("Sprint Planning", "Tuesday 4 Oct 2025", "10:00 AM - 12:00", "Medium", "Work"),
            Schedule("Team Sync", "Wednesday 5 Oct 2025", "14:00 PM - 15:00", "Low", "Work"),
            Schedule("Testing App", "Thursday 6 Oct 2025", "09:00 AM - 12:00", "Medium", "QA"),
            Schedule("Fix Bug", "Friday 7 Oct 2025", "10:00 AM - 16:00", "High", "Development"),
            Schedule("Deployment", "Saturday 8 Oct 2025", "08:00 AM - 10:00", "High", "Ops"),
            Schedule("Weekly Review", "Sunday 9 Oct 2025", "15:00 PM - 16:00", "Medium", "Work")
        )

        for (schedule in scheduleList) {
            val view = layoutInflater.inflate(R.layout.item_schedule, scheduleContainer, false)
            view.findViewById<TextView>(R.id.tvTitle).text = schedule.title
            view.findViewById<TextView>(R.id.tvDate).text = schedule.date
            view.findViewById<TextView>(R.id.tvTime).text = schedule.time
            view.findViewById<TextView>(R.id.tvPriority).text = schedule.priority
            view.findViewById<TextView>(R.id.tvCategory).text = schedule.category
            scheduleContainer.addView(view)
        }
    }
}
