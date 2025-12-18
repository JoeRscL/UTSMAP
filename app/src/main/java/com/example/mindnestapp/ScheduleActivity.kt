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

    // MODEL DATA
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

        // 1. Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)

        // 2. Setup Helper & UI
        footerHelper = FooterNavHelper(this)
        
        // Default selected date = Today
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())

        setupHeader()
        setupWeeklyCalendar()

        // 3. Load Data
        loadSchedules()

        footerHelper.setupFooterNavigation()

        binding.ivProfileSmall.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupHeader() {
        val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        // Tampilkan tanggal yang dipilih di header, bukan selalu hari ini
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
                        val name = snapshot.child("firstName").getValue(String::class.java)
                        val displayName = name.takeIf { !it.isNullOrEmpty() } ?: "User"
                        binding.tvUserName.text = "Hi, $displayName"
                    }
                    override fun onCancelled(error: DatabaseError) {
                        binding.tvUserName.text = "Hi, User"
                    }
                })
        }
    }

    // --- LOGIKA UTAMA: MEMUAT JADWAL ---
    private fun loadSchedules() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSchedules.clear() // Reset local list

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.id = child.key
                        if (schedule != null) {
                            allSchedules.add(schedule)
                        }
                    }
                }
                // Filter dan tampilkan berdasarkan tanggal yang dipilih
                displaySchedulesForDate(selectedDate)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fungsi baru untuk memfilter tampilan
    private fun displaySchedulesForDate(dateStr: String) {
        binding.scheduleContainer.removeAllViews()

        val filteredSchedules = allSchedules.filter { 
            it.date == dateStr && !it.isCompleted 
        }

        if (filteredSchedules.isNotEmpty()) {
            for (schedule in filteredSchedules) {
                val view = layoutInflater.inflate(R.layout.item_schedule_home, binding.scheduleContainer, false)

                // Binding ID dari XML item
                val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                val ivPriority = view.findViewById<ImageView>(R.id.ivPriority)
                val ivCategory = view.findViewById<ImageView>(R.id.ivCategory)
                val tvTime = view.findViewById<TextView>(R.id.tvTime)
                val container = view as LinearLayout

                // Tampilkan Data ke Layar
                tvTitle.text = schedule.title
                tvTime.text = schedule.time

                // Set Category Icon
                val categoryIcon = when (schedule.category.lowercase(Locale.getDefault())) {
                    "work", "pekerjaan", "kantor" -> R.drawable.ic_work
                    "gym", "olahraga", "workout", "sehat" -> R.drawable.ic_gym
                    "doctor", "dokter", "health", "kesehatan", "medis" -> R.drawable.ic_doctor
                    "study", "belajar", "kuliah", "sekolah" -> R.drawable.ic_file
                    "home", "rumah", "keluarga" -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar // Default icon
                }
                ivCategory.setImageResource(categoryIcon)

                // Set Priority Colors and Icon
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

                // Add Click Listener to Edit
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

                // Masukkan kartu ke dalam container
                binding.scheduleContainer.addView(view)
            }
        } else {
            // Tampilkan pesan kosong jika tidak ada jadwal di tanggal ini
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
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Start week from Sunday
        
        val todayCal = Calendar.getInstance()
        val todayStr = fullDateFormat.format(todayCal.time)
        
        // Cari index hari ini (0-6)
        selectedIndex = -1 // Reset
        
        binding.dayContainer.removeAllViews()

        for (i in 0..6) {
            val currentDate = tempCal.time
            calendarDates.add(currentDate)
            val currentDateStr = fullDateFormat.format(currentDate)
            
            // Set initial selected index to today if matches
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
            
            // Initial styling will be updated in updateDaySelection()
            
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
            
            // Click Listener untuk Ganti Tanggal
            val index = i
            circle.setOnClickListener {
                selectedIndex = index
                val newDate = calendarDates[index]
                selectedDate = fullDateFormat.format(newDate)
                
                // Update UI Header Tanggal
                val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
                binding.tvDate.text = headerFormat.format(newDate)
                
                // Update UI Calendar Selection
                updateDaySelection()
                
                // Filter List Jadwal
                displaySchedulesForDate(selectedDate)
            }

            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Jika hari ini tidak ada dalam rentang minggu ini (kasus jarang jika logic benar), default ke index 0
        if (selectedIndex == -1) selectedIndex = 0
        
        updateDaySelection()
    }
    
    private fun updateDaySelection() {
        for (i in dayCircles.indices) {
            val circle = dayCircles[i]
            val tvDay = circle.getChildAt(0) as TextView
            val tvDate = circle.getChildAt(1) as TextView
            
            if (i == selectedIndex) {
                // Selected Style
                circle.setBackgroundResource(R.drawable.bg_day_pill_selected)
                tvDay.setTextColor(Color.WHITE)
                tvDate.setTextColor(Color.WHITE)
            } else {
                // Unselected Style
                circle.setBackgroundResource(R.drawable.bg_day_pill_unselected)
                tvDay.setTextColor("#8E8E93".toColorInt())
                tvDate.setTextColor("#8E8E93".toColorInt())
            }
        }
    }

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}