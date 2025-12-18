package com.example.mindnestapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

// Decorator untuk Hari Libur
class HolidayDecorator(private val holidays: Set<CalendarDay>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        return day in holidays
    }

    override fun decorate(view: DayViewFacade?) {
        view?.addSpan(android.text.style.ForegroundColorSpan(Color.RED))
        view?.addSpan(android.text.style.StyleSpan(Typeface.BOLD))
    }
}

class ViewScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewScheduleBinding
    private lateinit var userSchedulesRef: DatabaseReference
    private lateinit var holidaysRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val allSchedules = mutableMapOf<String, MutableList<ScheduleItem>>()
    private val holidays = mutableMapOf<String, String>() // Map untuk menyimpan ["2024-08-17", "Kemerdekaan RI"]
    private lateinit var footerHelper: FooterNavHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Path ke jadwal user
        userSchedulesRef = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)
        // Path ke data hari libur
        holidaysRef = FirebaseDatabase.getInstance().getReference("holidays")

        footerHelper = FooterNavHelper(this)

        setupCalendar()
        setupListeners()
        fetchDataFromFirebase() // Fungsi gabungan untuk fetch jadwal dan hari libur
        footerHelper.setupFooterNavigation()
    }

    private fun setupCalendar() {
        binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(binding.calendarView.currentDate.date)
        binding.calendarView.selectedDate = CalendarDay.today()
    }

    private fun setupListeners() {
        binding.calendarView.setOnMonthChangedListener { _, date ->
            binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date.date)
            // Fetch ulang hari libur jika berganti tahun
            fetchHolidaysForYear(date.year)
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
            Toast.makeText(this, "Fitur ini belum diimplementasikan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDataFromFirebase() {
        // Fetch jadwal user
        userSchedulesRef.addValueEventListener(object : ValueEventListener {
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
                // Setelah jadwal ter-load, update kalender
                updateCalendarDecorators()
                // Tampilkan jadwal untuk hari ini
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                showSchedulesForDate(todayKey)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewScheduleActivity, "Gagal mengambil jadwal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Fetch hari libur untuk tahun ini
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        fetchHolidaysForYear(currentYear)
    }
    
    private fun fetchHolidaysForYear(year: Int) {
        holidaysRef.child(year.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                holidays.clear()
                if (snapshot.exists()) {
                    for (holidaySnapshot in snapshot.children) {
                        val date = holidaySnapshot.key
                        val name = holidaySnapshot.getValue(String::class.java)
                        if (date != null && name != null) {
                            holidays[date] = name
                        }
                    }
                }
                // Setelah data libur ter-load, update kalender lagi
                updateCalendarDecorators()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewScheduleActivity, "Gagal mengambil data hari libur.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCalendarDecorators() {
        binding.calendarView.removeDecorators()

        // 1. Decorator untuk Hari Libur
        val holidayDates = mutableSetOf<CalendarDay>()
        holidays.keys.forEach { dateStr ->
             try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                date?.let { holidayDates.add(CalendarDay.from(it)) }
            } catch (e: Exception) { }
        }
        if (holidayDates.isNotEmpty()) {
            binding.calendarView.addDecorator(HolidayDecorator(holidayDates))
        }

        // 2. Decorator untuk Jadwal User
        val redDates = mutableSetOf<CalendarDay>()
        val yellowDates = mutableSetOf<CalendarDay>()
        val greenDates = mutableSetOf<CalendarDay>()
        val blueDates = mutableSetOf<CalendarDay>()

        allSchedules.values.flatten().forEach { schedule ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(schedule.date)
                date?.let {
                    val calendarDay = CalendarDay.from(it)
                    // Jangan timpa dekorasi hari libur dengan titik jadwal
                    if (!holidayDates.contains(calendarDay)) {
                         val priority = schedule.priority.toLowerCase(Locale.ROOT)
                        when {
                            priority.contains("high") -> redDates.add(calendarDay)
                            priority.contains("medium") -> yellowDates.add(calendarDay)
                            priority.contains("low") -> greenDates.add(calendarDay)
                            else -> blueDates.add(calendarDay)
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        
        if (redDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FF3B30"), redDates))
        if (yellowDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#FFCC00"), yellowDates))
        if (greenDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#34C759"), greenDates))
        if (blueDates.isNotEmpty()) binding.calendarView.addDecorator(EventDecorator(Color.parseColor("#007AFF"), blueDates))
    }

    private fun showSchedulesForDate(dateKey: String) {
        binding.eventList.removeAllViews()

        // Judul header
        try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
            val formattedTitle = SimpleDateFormat("EEEE, d MMMM", Locale("id", "ID")).format(parsedDate)
            binding.tvSelectedDate.text = "Acara pada $formattedTitle"
        } catch (e: Exception) {
            binding.tvSelectedDate.text = "Acara pada $dateKey"
        }
        
        // Cek dan tampilkan jika hari ini libur
        if (holidays.containsKey(dateKey)) {
            val holidayName = holidays[dateKey]
            val holidayView = TextView(this).apply {
                text = "Hari Libur: $holidayName"
                setTextColor(Color.RED)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 16, 0, 16)
            }
            binding.eventList.addView(holidayView)
        }

        val schedulesForDate = allSchedules[dateKey]

        if (schedulesForDate.isNullOrEmpty()) {
             if (!holidays.containsKey(dateKey)) { // Hanya tampilkan jika bukan hari libur
                val noEventView = layoutInflater.inflate(R.layout.item_no_schedule, binding.eventList, false)
                binding.eventList.addView(noEventView)
            }
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
                    priority.contains("high") -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_red)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_high_bg))
                    }
                    priority.contains("medium") -> {
                        priorityDot.setImageResource(R.drawable.priority_dot_yellow)
                        bgDrawable?.setColor(ContextCompat.getColor(this, R.color.priority_medium_bg))
                    }
                    priority.contains("low") -> {
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
                    category.contains("work") -> R.drawable.ic_work
                    category.contains("gym") -> R.drawable.ic_gym
                    category.contains("doctor") -> R.drawable.ic_doctor
                    category.contains("study") -> R.drawable.ic_file
                    category.contains("home") -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar
                }
                ivCategoryIcon.setImageResource(iconRes)

                binding.eventList.addView(itemView)
            }
        }
    }
}
