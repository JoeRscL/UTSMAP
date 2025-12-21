package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

// --- DATA CLASS ---
data class Schedule(
    var id: String? = null,
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val priority: String = "",
    val category: String = "",
    val isCompleted: Boolean = false
)

// --- DECORATORS ---

class EventDecorator(private val color: Int, dates: Collection<CalendarDay>) : DayViewDecorator {
    private val dates: HashSet<CalendarDay> = HashSet(dates)
    override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(6f, color))
    }
}

class HolidayDecorator(private val holidays: Set<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = holidays.contains(day)
    override fun decorate(view: DayViewFacade) {
        view.addSpan(android.text.style.ForegroundColorSpan(Color.RED))
        view.addSpan(android.text.style.StyleSpan(Typeface.BOLD))
    }
}

class TodayDecorator : DayViewDecorator {
    private val date = CalendarDay.today()
    override fun shouldDecorate(day: CalendarDay): Boolean = day == date
    override fun decorate(view: DayViewFacade) {
        view.addSpan(android.text.style.StyleSpan(Typeface.BOLD))
    }
}

// --- ACTIVITY UTAMA ---

class ViewScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewScheduleBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var holidaysRef: DatabaseReference

    private val allSchedules = mutableListOf<Schedule>()
    private val holidayMap = mutableMapOf<String, String>()

    private lateinit var footerHelper: FooterNavHelper
    private var selectedDateStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)
        holidaysRef = FirebaseDatabase.getInstance().getReference("holidays")
        footerHelper = FooterNavHelper(this)
        footerHelper.setupFooterNavigation()

        setupCalendar()
        loadHolidaysAndSchedules()
    }

    private fun setupCalendar() {
        // --- PERBAIKAN UTAMA DI SINI ---
        // Matikan header bawaan lewat kode agar tidak error XML
        binding.calendarView.topbarVisible = false
        // --------------------------------

        val today = CalendarDay.today()
        binding.calendarView.selectedDate = today

        selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthFormat.format(today.date)

        binding.calendarView.setOnMonthChangedListener { _, date ->
            binding.tvMonthYear.text = monthFormat.format(date.date)
        }

        binding.btnPrevMonth.setOnClickListener { binding.calendarView.goToPrevious() }
        binding.btnNextMonth.setOnClickListener { binding.calendarView.goToNext() }

        binding.calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDateStr = sdf.format(date.date)
                displaySchedulesForDate(selectedDateStr)
            }
        }

        binding.btnViewAll.setOnClickListener {
            Toast.makeText(this, "Menampilkan semua event bulan ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHolidaysAndSchedules() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()

        holidaysRef.child(currentYear).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                holidayMap.clear()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val dateKey = child.key
                        val name = child.getValue(String::class.java)
                        if (dateKey != null && name != null) {
                            holidayMap[dateKey] = name
                        }
                    }
                }
                loadUserSchedules()
            }

            override fun onCancelled(error: DatabaseError) {
                loadUserSchedules()
            }
        })
    }

    private fun loadUserSchedules() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSchedules.clear()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.id = child.key
                        if (schedule != null) {
                            allSchedules.add(schedule)
                        }
                    }
                }
                updateCalendarDecorators()
                displaySchedulesForDate(selectedDateStr)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewScheduleActivity, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCalendarDecorators() {
        binding.calendarView.removeDecorators()

        val holidayDates = HashSet<CalendarDay>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        holidayMap.keys.forEach { dateStr ->
            try {
                val date = sdf.parse(dateStr)
                if (date != null) holidayDates.add(CalendarDay.from(date))
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (holidayDates.isNotEmpty()) {
            binding.calendarView.addDecorator(HolidayDecorator(holidayDates))
        }

        val highPriorityDates = HashSet<CalendarDay>()
        val normalDates = HashSet<CalendarDay>()

        allSchedules.forEach { schedule ->
            try {
                val date = sdf.parse(schedule.date)
                if (date != null && !schedule.isCompleted) {
                    val day = CalendarDay.from(date)
                    if (schedule.priority.equals("high", ignoreCase = true)) {
                        highPriorityDates.add(day)
                    } else {
                        normalDates.add(day)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        if (highPriorityDates.isNotEmpty()) {
            binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FF3B30"), highPriorityDates))
        }
        if (normalDates.isNotEmpty()) {
            binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#007AFF"), normalDates))
        }

        binding.calendarView.addDecorator(TodayDecorator())
    }

    private fun displaySchedulesForDate(dateStr: String) {
        binding.eventList.removeAllViews()

        try {
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val headerFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            binding.tvSelectedDate.text = headerFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            binding.tvSelectedDate.text = dateStr
        }

        if (holidayMap.containsKey(dateStr)) {
            val holidayName = holidayMap[dateStr]
            val holidayView = TextView(this).apply {
                text = "Libur Nasional: $holidayName"
                setTextColor(Color.RED)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, 24)
            }
            binding.eventList.addView(holidayView)
        }

        val filteredList = allSchedules.filter { it.date == dateStr && !it.isCompleted }.sortedBy { it.time }

        if (filteredList.isEmpty()) {
            if (!holidayMap.containsKey(dateStr)) {
                try {
                    val emptyView = layoutInflater.inflate(R.layout.item_no_schedule, binding.eventList, false)
                    binding.eventList.addView(emptyView)
                } catch (e: Exception) {
                    val tvEmpty = TextView(this)
                    tvEmpty.text = "Tidak ada jadwal."
                    tvEmpty.setTextColor(Color.GRAY)
                    binding.eventList.addView(tvEmpty)
                }
            }
        } else {
            for (schedule in filteredList) {
                val itemView = layoutInflater.inflate(R.layout.item_schedule_home, binding.eventList, false)

                val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
                val ivPriority = itemView.findViewById<ImageView>(R.id.ivPriority)
                val ivCategory = itemView.findViewById<ImageView>(R.id.ivCategory)
                val container = itemView as LinearLayout

                tvTitle.text = schedule.title
                tvTime.text = schedule.time

                val bgDrawable = ContextCompat.getDrawable(this, R.drawable.bg_card)?.mutate() as? GradientDrawable

                when (schedule.priority.lowercase(Locale.getDefault())) {
                    "high" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_red)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_high_bg))
                    }
                    "medium" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_yellow)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_medium_bg))
                    }
                    "low" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_green)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_low_bg))
                    }
                    else -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_blue)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_default_bg))
                    }
                }
                container.background = bgDrawable

                val categoryIcon = when (schedule.category.lowercase(Locale.getDefault())) {
                    "work" -> R.drawable.ic_work
                    "gym" -> R.drawable.ic_gym
                    "doctor" -> R.drawable.ic_doctor
                    "study" -> R.drawable.ic_file
                    "home" -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar
                }
                ivCategory.setImageResource(categoryIcon)

                itemView.setOnClickListener {
                    val intent = Intent(this, AddTaskActivity::class.java)
                    intent.putExtra("taskId", schedule.id)
                    intent.putExtra("isEditMode", true)
                    startActivity(intent)
                }

                binding.eventList.addView(itemView)
            }
        }
    }
}