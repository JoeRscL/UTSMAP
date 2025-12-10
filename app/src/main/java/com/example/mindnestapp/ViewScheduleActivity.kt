package com.example.mindnestapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    val priority: String = "" // Di AddTaskActivity, ini menyimpan 'Category' (ex: Work, Gym)
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
        val orangeDates = mutableSetOf<CalendarDay>()
        val blueDates = mutableSetOf<CalendarDay>()

        allSchedules.values.flatten().forEach { schedule ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(schedule.date)
                date?.let {
                    val calendarDay = CalendarDay.from(it)
                    val category = schedule.priority.toLowerCase(Locale.ROOT)
                    when {
                        category.contains("gym") || category.contains("sport") -> orangeDates.add(calendarDay)
                        category.contains("doctor") || category.contains("medis") -> redDates.add(calendarDay)
                        else -> blueDates.add(calendarDay)
                    }
                }
            } catch (e: Exception) { }
        }

        binding.calendarView.removeDecorators()
        if (redDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FF3B30"), redDates))
        if (orangeDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FF9500"), orangeDates))
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
                val priorityDot = itemView.findViewById<View>(R.id.priorityDot)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
                val ivCategoryIcon = itemView.findViewById<ImageView>(R.id.ivCategoryIcon)

                tvTitle.text = schedule.title
                tvTime.text = schedule.time

                val category = schedule.priority.toLowerCase(Locale.ROOT)

                // Default Blue (Work/General)
                var cardColor = Color.parseColor("#E3F2FD") // Light Blue
                var dotDrawable = R.drawable.priority_dot_blue
                var iconRes = R.drawable.ic_work

                when {
                    category.contains("gym") || category.contains("sport") || category.contains("olahraga") -> {
                        cardColor = Color.parseColor("#FFF3E0") // Light Orange
                        dotDrawable = R.drawable.priority_dot_orange
                        iconRes = R.drawable.ic_gym
                    }
                    category.contains("doctor") || category.contains("medis") || category.contains("sakit") || category.contains("hospital") -> {
                        cardColor = Color.parseColor("#FFEBEE") // Light Red
                        dotDrawable = R.drawable.priority_dot_red
                        iconRes = R.drawable.ic_doctor
                    }
                    else -> {
                        // Default / Work
                        cardColor = Color.parseColor("#E3F2FD")
                        dotDrawable = R.drawable.priority_dot_blue
                        iconRes = R.drawable.ic_work
                    }
                }

                itemRoot.setBackgroundColor(cardColor)
                priorityDot.setBackgroundResource(dotDrawable)
                ivCategoryIcon.setImageResource(iconRes)

                binding.eventList.addView(itemView)
            }
        }
    }
}
