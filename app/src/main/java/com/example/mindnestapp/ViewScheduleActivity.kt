package com.example.mindnestapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mindnestapp.databinding.ActivityViewScheduleBinding
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

// Data class disesuaikan dengan data di Firebase
data class ScheduleItem(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val priority: String = "", // High, Medium, Low
    val category: String = "" // Work, Gym, Doctor, etc.
)

// Class untuk Decorator (memberi titik pada kalender)
class EventDecorator(private val color: Int, dates: Collection<CalendarDay>) : DayViewDecorator {
    private val dates: HashSet<CalendarDay> = HashSet(dates)
    override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(7f, color))
    }
}

class ViewScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewScheduleBinding
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("schedules")
    private val allSchedules = mutableMapOf<String, MutableList<ScheduleItem>>()
    private lateinit var footerHelper: FooterNavHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        footerHelper = FooterNavHelper(this)

        setupCalendar()
        setupListeners()
        fetchSchedulesFromFirebase()
        footerHelper.setupFooterNavigation()
    }

    private fun setupCalendar() {
        binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(binding.calendarView.currentDate.date)
        binding.calendarView.selectedDate = CalendarDay.today()
    }

    private fun setupListeners() {
        binding.calendarView.setOnMonthChangedListener { _, date ->
            binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date.date)
        }
        binding.btnPrevMonth.setOnClickListener { binding.calendarView.goToPrevious() }
        binding.btnNextMonth.setOnClickListener { binding.calendarView.goToNext() }
        binding.calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                val selectedDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.date)
                showSchedulesForDate(selectedDateKey)
            }
        }
        binding.btnViewAll.setOnClickListener {
            Toast.makeText(this, "Menampilkan semua event bulan ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchSchedulesFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSchedules.clear()
                for (scheduleSnapshot in snapshot.children) {
                    val schedule = scheduleSnapshot.getValue(ScheduleItem::class.java)
                    schedule?.let {
                        if (it.date.isNotBlank()) {
                            if (!allSchedules.containsKey(it.date)) {
                                allSchedules[it.date] = mutableListOf()
                            }
                            allSchedules[it.date]?.add(it)
                        }
                    }
                }
                updateCalendarDecorators()
                val currentSelectedDate = binding.calendarView.selectedDate ?: CalendarDay.today()
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentSelectedDate.date)
                showSchedulesForDate(todayKey)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewScheduleActivity, "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCalendarDecorators() {
        val redDates = mutableSetOf<CalendarDay>()
        val yellowDates = mutableSetOf<CalendarDay>()
        val greenDates = mutableSetOf<CalendarDay>()
        val blueDates = mutableSetOf<CalendarDay>()

        allSchedules.values.flatten().forEach { schedule ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(schedule.date)
                date?.let {
                    val calendarDay = CalendarDay.from(it)
                    val priority = schedule.priority.toLowerCase(Locale.ROOT)
                    when {
                        priority == "high" || priority == "tinggi" -> redDates.add(calendarDay)
                        priority == "medium" || priority == "sedang" -> yellowDates.add(calendarDay)
                        priority == "low" || priority == "rendah" -> greenDates.add(calendarDay)
                        else -> blueDates.add(calendarDay)
                    }
                }
            } catch (e: Exception) { }
        }

        binding.calendarView.removeDecorators()
        if (redDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FF3B30"), redDates))
        if (yellowDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FFCC00"), yellowDates))
        if (greenDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#34C759"), greenDates))
        if (blueDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#007AFF"), blueDates))
    }

    private fun showSchedulesForDate(dateKey: String) {
        binding.eventList.removeAllViews()

        try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
            val formattedTitle = SimpleDateFormat("EEEE, d MMMM", Locale("en", "US")).format(parsedDate)
            binding.tvSelectedDate.text = "Events on $formattedTitle"
        } catch (e: Exception) {
            binding.tvSelectedDate.text = "Events on $dateKey"
        }

        val schedulesForDate = allSchedules[dateKey]

        if (schedulesForDate.isNullOrEmpty()) {
            val noEventView = layoutInflater.inflate(R.layout.item_no_schedule, binding.eventList, false)
            binding.eventList.addView(noEventView)
        } else {
            schedulesForDate.sortBy { it.time }

            schedulesForDate.forEach { schedule ->
                val itemView = layoutInflater.inflate(R.layout.item_schedule, binding.eventList, false)

                val itemRoot = itemView.findViewById<LinearLayout>(R.id.itemRoot)
                val priorityDot = itemView.findViewById<ImageView>(R.id.priorityDot)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
                val ivCategoryIcon = itemView.findViewById<ImageView>(R.id.ivCategoryIcon)

                tvTitle.text = schedule.title
                tvTime.text = schedule.time

                // Set Priority Colors and Background
                val priority = schedule.priority.toLowerCase(Locale.ROOT)
                val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_card)?.mutate() as? GradientDrawable

                when {
                    priority == "high" || priority == "tinggi" -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_red)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_high_bg))
                    }
                    priority == "medium" || priority == "sedang" -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_yellow)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_medium_bg))
                    }
                    priority == "low" || priority == "rendah" -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_green)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_low_bg))
                    }
                    else -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_blue)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_default_bg))
                    }
                }
                itemRoot.background = bgDrawable

                // Set Category Icon
                val category = schedule.category.toLowerCase(Locale.ROOT)
                val iconRes = when {
                    category.contains("work") || category.contains("pekerjaan") -> R.drawable.ic_work
                    category.contains("gym") || category.contains("olahraga") -> R.drawable.ic_gym
                    category.contains("doctor") || category.contains("dokter") -> R.drawable.ic_doctor
                    category.contains("study") || category.contains("belajar") -> R.drawable.ic_file
                    category.contains("home") || category.contains("rumah") -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar
                }
                ivCategoryIcon.setImageResource(iconRes)

                binding.eventList.addView(itemView)
            }
        }
    }
}