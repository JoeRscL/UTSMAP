package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.mindnestapp.databinding.ActivityScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    // ... (property lainnya tetap sama)
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

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var dayCircles: MutableList<LinearLayout>
    private lateinit var calendarDates: MutableList<Date> // Simpan tanggal objek untuk filter
    private var selectedDate: String = "" // Simpan tanggal yang dipilih user (yyyy-MM-dd)
    private var selectedIndex: Int = -1 // Indeks hari yang dipilih
    private lateinit var footerHelper: FooterNavHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var allSchedules = mutableListOf<Schedule>() // Simpan semua jadwal untuk filtering lokal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }
        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)

        footerHelper = FooterNavHelper(this)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())

        // Panggil method yang tidak perlu di-refresh di sini
        setupWeeklyCalendar()
        loadSchedules()
        footerHelper.setupFooterNavigation()

        binding.ivProfileSmall.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Panggil setupHeader() di sini agar selalu refresh saat kembali ke halaman ini
        setupHeader()
    }

    private fun setupHeader() {
        val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        try {
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            binding.tvDate.text = headerFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            binding.tvDate.text = headerFormat.format(Date())
        }

        val user = auth.currentUser
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java)
                        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                        val displayName = if (!firstName.isNullOrBlank()) {
                            "$firstName $lastName".trim()
                        } else {
                            "User"
                        }
                        binding.tvUserName.text = "Hi, $displayName"
                    }
                    override fun onCancelled(error: DatabaseError) {
                        binding.tvUserName.text = "Hi, User"
                    }
                })
        }
    }

    // ... (Sisa fungsi lainnya: loadSchedules, displaySchedulesForDate, setupWeeklyCalendar, dll, tetap sama)
    private fun loadSchedules() {
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
                displaySchedulesForDate(selectedDate)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displaySchedulesForDate(dateStr: String) {
        binding.scheduleContainer.removeAllViews()

        val filteredSchedules = allSchedules.filter { it.date == dateStr && !it.isCompleted }

        if (filteredSchedules.isNotEmpty()) {
            for (schedule in filteredSchedules) {
                val view = layoutInflater.inflate(R.layout.item_schedule_home, binding.scheduleContainer, false)

                val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                val ivPriority = view.findViewById<ImageView>(R.id.ivPriority)
                val ivCategory = view.findViewById<ImageView>(R.id.ivCategory)
                val tvTime = view.findViewById<TextView>(R.id.tvTime)
                val container = view as LinearLayout

                tvTitle.text = schedule.title
                tvTime.text = schedule.time

                val categoryIcon = when (schedule.category.lowercase(Locale.getDefault())) {
                    "work", "pekerjaan", "kantor" -> R.drawable.ic_work
                    "gym", "olahraga", "workout", "sehat" -> R.drawable.ic_gym
                    "doctor", "dokter", "health", "kesehatan", "medis" -> R.drawable.ic_doctor
                    "study", "belajar", "kuliah", "sekolah" -> R.drawable.ic_file
                    "home", "rumah", "keluarga" -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar
                }
                ivCategory.setImageResource(categoryIcon)

                val bgDrawable = ContextCompat.getDrawable(this@ScheduleActivity, R.drawable.bg_card)?.mutate() as? GradientDrawable
                
                when (schedule.priority.lowercase(Locale.getDefault())) {
                    "tinggi", "high" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_red)
                        bgDrawable?.setColor(ContextCompat.getColor(this@ScheduleActivity, R.color.priority_high_bg))
                    }
                    "sedang", "medium" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_yellow)
                        bgDrawable?.setColor(ContextCompat.getColor(this@ScheduleActivity, R.color.priority_medium_bg))
                    }
                    "rendah", "low" -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_green)
                        bgDrawable?.setColor(ContextCompat.getColor(this@ScheduleActivity, R.color.priority_low_bg))
                    }
                    else -> {
                        ivPriority.setImageResource(R.drawable.priority_dot_blue)
                        bgDrawable?.setColor(ContextCompat.getColor(this@ScheduleActivity, R.color.priority_default_bg))
                    }
                }
                container.background = bgDrawable

                view.setOnClickListener {
                    val intent = Intent(this@ScheduleActivity, AddTaskActivity::class.java)
                    intent.putExtra("taskId", schedule.id)
                    intent.putExtra("title", schedule.title)
                    intent.putExtra("category", schedule.category)
                    intent.putExtra("priority", schedule.priority)
                    intent.putExtra("date", schedule.date)
                    intent.putExtra("time", schedule.time)
                    intent.putExtra("notes", schedule.description)
                    intent.putExtra("isEditMode", true)
                    startActivity(intent)
                }

                binding.scheduleContainer.addView(view)
            }
        } else {
             val emptyView = layoutInflater.inflate(R.layout.item_no_schedule, binding.scheduleContainer, false)
             binding.scheduleContainer.addView(emptyView)
        }
    }

    private fun setupWeeklyCalendar() {
        dayCircles = mutableListOf()
        calendarDates = mutableListOf()
        
        val dayInitials = listOf("S", "M", "T", "W", "T", "F", "S")
        val dateOnlyFormat = SimpleDateFormat("d", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        
        val todayCal = Calendar.getInstance()
        val todayStr = fullDateFormat.format(todayCal.time)
        
        selectedIndex = -1
        
        binding.dayContainer.removeAllViews()

        for (i in 0..6) {
            val currentDate = tempCal.time
            calendarDates.add(currentDate)
            val currentDateStr = fullDateFormat.format(currentDate)
            
            if (currentDateStr == todayStr && selectedIndex == -1) {
                selectedIndex = i
            }

            val dayWrapper = LinearLayout(this)
            dayWrapper.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            dayWrapper.gravity = Gravity.CENTER
            dayWrapper.orientation = LinearLayout.VERTICAL

            val circle = LinearLayout(this)
            circle.layoutParams = LinearLayout.LayoutParams(50.dp, 80.dp)
            circle.orientation = LinearLayout.VERTICAL
            circle.gravity = Gravity.CENTER
            
            val tvDay = TextView(this)
            tvDay.text = dayInitials[i]
            tvDay.textSize = 14f
            tvDay.setTypeface(null, Typeface.BOLD)
            tvDay.gravity = Gravity.CENTER
            
            val dateText = dateOnlyFormat.format(currentDate)
            val monthText = monthFormat.format(currentDate)

            val tvDate = TextView(this)
            val fullDateText = """
            $dateText
            $monthText
            """.trimIndent()
            tvDate.text = fullDateText
            tvDate.textSize = 10f
            tvDate.gravity = Gravity.CENTER
            tvDate.setLines(2)

            circle.addView(tvDay)
            circle.addView(tvDate)
            dayWrapper.addView(circle)
            binding.dayContainer.addView(dayWrapper)
            dayCircles.add(circle)
            
            val index = i
            circle.setOnClickListener {
                selectedIndex = index
                val newDate = calendarDates[index]
                selectedDate = fullDateFormat.format(newDate)
                
                val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
                binding.tvDate.text = headerFormat.format(newDate)
                
                updateDaySelection()
                
                displaySchedulesForDate(selectedDate)
            }

            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        if (selectedIndex == -1) selectedIndex = 0
        
        updateDaySelection()
    }
    
    private fun updateDaySelection() {
        for (i in dayCircles.indices) {
            val circle = dayCircles[i]
            val tvDay = circle.getChildAt(0) as TextView
            val tvDate = circle.getChildAt(1) as TextView
            
            if (i == selectedIndex) {
                circle.setBackgroundResource(R.drawable.bg_day_pill_selected)
                tvDay.setTextColor(Color.WHITE)
                tvDate.setTextColor(Color.WHITE)
            } else {
                circle.setBackgroundResource(R.drawable.bg_day_pill_unselected)
                tvDay.setTextColor("#8E8E93".toColorInt())
                tvDate.setTextColor("#8E8E93".toColorInt())
            }
        }
    }

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}