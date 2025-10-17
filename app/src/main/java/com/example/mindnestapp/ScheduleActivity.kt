package com.example.mindnestapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.R
import java.text.SimpleDateFormat
import java.util.*

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
        FooterNavHelper(this).setupFooterNavigation()

        // ======================
        // BAGIAN KALENDER OVAL OTOMATIS + INTERAKTIF
        // ======================
        val dayContainer = findViewById<LinearLayout>(R.id.dayContainer)
        val days = listOf("S", "M", "T", "W", "T", "F", "S")

        val calendar = Calendar.getInstance()
        val todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) - 1) // 0 = Sunday
        val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val dayCircles = mutableListOf<LinearLayout>()
        val dayTexts = mutableListOf<Pair<TextView, TextView>>() // (tvDay, tvDate)

        for (i in days.indices) {
            val dayWrapper = LinearLayout(this)
            val wrapperParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            wrapperParams.setMargins(12, 0, 12, 0)
            dayWrapper.layoutParams = wrapperParams
            dayWrapper.gravity = Gravity.CENTER

            val circle = LinearLayout(this)
            val circleParams = LinearLayout.LayoutParams(70.dp, 70.dp)
            circle.layoutParams = circleParams
            circle.orientation = LinearLayout.VERTICAL
            circle.gravity = Gravity.CENTER
            circle.setBackgroundResource(
                if (i == todayIndex) R.drawable.bg_day_selected else R.drawable.bg_day_unselected
            )

            val tvDay = TextView(this)
            tvDay.text = days[i]
            tvDay.textSize = 18f
            tvDay.setTypeface(null, Typeface.BOLD)
            tvDay.gravity = Gravity.CENTER
            tvDay.setTextColor(
                if (i == todayIndex)
                    getColor(R.color.blue_700)
                else
                    getColor(R.color.gray_600)
            )

            val tvDate = TextView(this)
            tvDate.text = dateFormat.format(tempCal.time)
            tvDate.textSize = 12f
            tvDate.gravity = Gravity.CENTER
            tvDate.setTextColor(
                if (i == todayIndex)
                    getColor(R.color.blue_700)
                else
                    getColor(R.color.gray_600)
            )

            circle.addView(tvDay)
            circle.addView(tvDate)
            dayWrapper.addView(circle)
            dayContainer.addView(dayWrapper)

            dayCircles.add(circle)
            dayTexts.add(tvDay to tvDate)

            // EVENT KLIK UNTUK UBAH HARI AKTIF
            circle.setOnClickListener {
                for (j in dayCircles.indices) {
                    dayCircles[j].setBackgroundResource(R.drawable.bg_day_unselected)
                    dayTexts[j].first.setTextColor(getColor(R.color.gray_600))
                    dayTexts[j].second.setTextColor(getColor(R.color.gray_600))
                }
                circle.setBackgroundResource(R.drawable.bg_day_selected)
                tvDay.setTextColor(getColor(R.color.blue_700))
                tvDate.setTextColor(getColor(R.color.blue_700))
            }

            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // ======================
        // BAGIAN DAFTAR JADWAL
        // ======================
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

        // ======================
// FOOTER NAVIGATION (MENU BAWAH)
// ======================
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navCalendar)?.setOnClickListener {
            Toast.makeText(this, "You are already on Schedule page", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navAdd)?.setOnClickListener {
            // ⬇️ ini bagian pentingnya: pindah ke halaman AddTask
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navFile)?.setOnClickListener {
            Toast.makeText(this, "Open Activity Files", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navSettings)?.setOnClickListener {
            Toast.makeText(this, "Open Settings", Toast.LENGTH_SHORT).show()
        }

    }

    // Fungsi konversi dp ke pixel biar ukuran oval tetap proporsional
    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
