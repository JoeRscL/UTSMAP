package com.example.mindnestapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    data class Schedule(
        val title: String = "",
        val description: String = "",
        val date: String = "",
        val time: String = "",
        val priority: String = ""
    )

    private lateinit var dayContainer: LinearLayout
    private lateinit var dayCircles: MutableList<LinearLayout>
    private lateinit var days: List<String>
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var tempCal: Calendar
    private var todayIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // Inisialisasi kalender
        dayContainer = findViewById(R.id.dayContainer)
        dayCircles = mutableListOf()
        days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        dateFormat = SimpleDateFormat("dd", Locale.getDefault())
        tempCal = Calendar.getInstance()
        todayIndex = tempCal.get(Calendar.DAY_OF_WEEK) - 1

        val dayTexts = mutableListOf<Pair<TextView, TextView>>()

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

        // Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val scheduleRef = database.getReference("schedules")
        val scheduleContainer = findViewById<LinearLayout>(R.id.scheduleContainer)

        scheduleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scheduleContainer.removeAllViews() // hapus jadwal lama
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.let {
                            val view = layoutInflater.inflate(R.layout.item_schedule, scheduleContainer, false)
                            view.findViewById<TextView>(R.id.tvTitle).text = it.title
                            view.findViewById<TextView>(R.id.tvDate).text = it.date
                            view.findViewById<TextView>(R.id.tvTime).text = it.time
                            view.findViewById<TextView>(R.id.tvPriority).text = it.priority
                            view.findViewById<TextView>(R.id.tvCategory).text = it.description
                            scheduleContainer.addView(view)
                        }
                    }
                } else {
                    Toast.makeText(this@ScheduleActivity, "Tidak ada data jadwal", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Footer Navigation
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navCalendar)?.setOnClickListener {
            Toast.makeText(this, "You are already on Schedule page", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navAdd)?.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navFile)?.setOnClickListener {
            Toast.makeText(this, "Open Activity Files", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navSettings)?.setOnClickListener {
            Toast.makeText(this, "Open Settings", Toast.LENGTH_SHORT).show()
        }
    }

    // fungsi konversi dp ke px
    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
