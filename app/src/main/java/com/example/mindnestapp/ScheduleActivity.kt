package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.mindnestapp.databinding.ActivityScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleActivity : AppCompatActivity() {

    // Model Data
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
    private lateinit var calendarDates: MutableList<Date>
    private var currentWeekCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: String = ""
    private var selectedIndex: Int = -1

    private lateinit var footerHelper: FooterNavHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var allSchedules = mutableListOf<Schedule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, login::class.java) // Pastikan nama class Login benar
            startActivity(intent)
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)

        footerHelper = FooterNavHelper(this)
        footerHelper.setupFooterNavigation()

        // Set tanggal awal ke hari ini
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        setupWeeklyCalendar()
        loadSchedules()
        setupHeader()

        binding.ivSettings.setOnClickListener { showSettingsMenu(it) }
        binding.btnPrevWeek.setOnClickListener { changeWeek(-1) }
        binding.btnNextWeek.setOnClickListener { changeWeek(1) }
    }

    override fun onResume() {
        super.onResume()
        setupHeader()
    }

    private fun changeWeek(amount: Int) {
        currentWeekCalendar.add(Calendar.WEEK_OF_YEAR, amount)
        selectedIndex = -1
        setupWeeklyCalendar()
    }

    private fun showSettingsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.settings_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_light_mode -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    true
                }
                R.id.action_dark_mode -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupHeader() {
        val user = auth.currentUser ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "User"
                binding.tvUserName.text = "Hi, $firstName"
                binding.ivSettings.setImageResource(R.drawable.ic_settings)
                binding.ivSettings.setColorFilter(ContextCompat.getColor(applicationContext, R.color.blue_700))
            }
            override fun onCancelled(error: DatabaseError) { binding.tvUserName.text = "Hi, User" }
        })

        val headerFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        try {
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            binding.tvDate.text = headerFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            binding.tvDate.text = headerFormat.format(Date())
        }
    }

    private fun loadSchedules() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSchedules.clear()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.id = child.key
                        if (schedule != null) allSchedules.add(schedule)
                    }
                }
                // Refresh tampilan setelah data masuk
                displaySchedulesForDate(selectedDate)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Gagal memuat jadwal.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // === MENAMPILKAN LIST ===
    private fun displaySchedulesForDate(dateStr: String) {
        // Bersihkan view lama
        binding.llActiveSchedule.removeAllViews()
        binding.llFinishedSchedule.removeAllViews()

        binding.tvFinishedHeader.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        // Filter Data
        val tasksForDate = allSchedules.filter { it.date == dateStr }
        val activeTasks = tasksForDate.filter { !it.isCompleted }.sortedBy { it.time }
        val completedTasks = tasksForDate.filter { it.isCompleted }.sortedBy { it.time }

        // Isi List Active (Atas)
        if (activeTasks.isNotEmpty()) {
            for (schedule in activeTasks) {
                val view = createScheduleItemView(schedule)
                binding.llActiveSchedule.addView(view)
            }
        }

        // Isi List Finished (Bawah)
        if (completedTasks.isNotEmpty()) {
            binding.tvFinishedHeader.visibility = View.VISIBLE
            for (schedule in completedTasks) {
                val view = createScheduleItemView(schedule)
                binding.llFinishedSchedule.addView(view)
            }
        }

        // Tampilkan State Kosong jika tidak ada tugas sama sekali
        if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
        }
    }

    // === HELPER MEMBUAT ITEM VIEW (PERBAIKAN UTAMA DISINI) ===
    private fun createScheduleItemView(schedule: Schedule): View {
        // 1. Inflate layout simpel
        val view = layoutInflater.inflate(R.layout.item_schedule_home, null, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val ivPriority = view.findViewById<ImageView>(R.id.ivPriority)
        val ivCategory = view.findViewById<ImageView>(R.id.ivCategory)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)

        // 2. ATUR LAYOUT PARAMS (MARGIN) SECARA PROGRAMATIK
        // Ini kuncinya agar tidak berdempetan
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        // Hitung pixel dari dp
        val marginSideInPx = (16 * resources.displayMetrics.density).toInt()
        val marginBottomInPx = (12 * resources.displayMetrics.density).toInt()

        // Set Margin (Kiri, Atas, Kanan, Bawah)
        params.setMargins(marginSideInPx, 0, marginSideInPx, marginBottomInPx)
        view.layoutParams = params

        // 3. ISI DATA
        tvTitle.text = schedule.title
        tvTime.text = schedule.time

        // Icon Kategori
        val categoryIcon = when (schedule.category.lowercase(Locale.getDefault())) {
            "work" -> R.drawable.ic_work
            "doctor" -> R.drawable.ic_doctor
            "study" -> R.drawable.ic_file
            "home" -> R.drawable.ic_home
            "sport" -> R.drawable.ic_gym
            else -> R.drawable.ic_calendar
        }
        ivCategory.setImageResource(categoryIcon)

        // 4. STYLE BACKGROUND (WARNA & ROUNDED CORNER)
        val bgDrawable = GradientDrawable()
        // Buat sudut melengkung (20dp)
        bgDrawable.cornerRadius = 20f * resources.displayMetrics.density

        if (schedule.isCompleted) {
            // STYLE SELESAI
            bgDrawable.setColor(Color.parseColor("#E0E0E0")) // Abu-abu
            tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            ivPriority.setImageResource(R.drawable.priority_dot_blue)
            view.alpha = 0.6f
        } else {
            // STYLE BELUM SELESAI
            tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            view.alpha = 1.0f

            // Warna Pastel
            when (schedule.priority.lowercase(Locale.getDefault())) {
                "high" -> {
                    ivPriority.setImageResource(R.drawable.priority_dot_red)
                    bgDrawable.setColor(Color.parseColor("#FFEBEE"))
                }
                "medium" -> {
                    ivPriority.setImageResource(R.drawable.priority_dot_yellow)
                    bgDrawable.setColor(Color.parseColor("#FFF8E1"))
                }
                "low" -> {
                    ivPriority.setImageResource(R.drawable.priority_dot_green)
                    bgDrawable.setColor(Color.parseColor("#E8F5E9"))
                }
                else -> {
                    ivPriority.setImageResource(R.drawable.priority_dot_blue)
                    bgDrawable.setColor(Color.parseColor("#E3F2FD"))
                }
            }
        }

        // Terapkan background langsung ke View Utama
        view.background = bgDrawable

        // Klik Edit
        view.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("taskId", schedule.id)
            intent.putExtra("isEditMode", true)
            startActivity(intent)
        }

        return view
    }

    // === KALENDER 7 HARI ===
    private fun setupWeeklyCalendar() {
        dayCircles = mutableListOf()
        calendarDates = mutableListOf()

        val dayInitials = listOf("M", "T", "W", "T", "F", "S", "S")
        val dateOnlyFormat = SimpleDateFormat("d", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val tempCal = currentWeekCalendar.clone() as Calendar
        tempCal.firstDayOfWeek = Calendar.MONDAY
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        binding.dayContainer.removeAllViews()

        for (i in 0 until 7) {
            val currentDate = tempCal.time
            calendarDates.add(currentDate)
            val currentDateStr = fullDateFormat.format(currentDate)

            if (currentDateStr == selectedDate) selectedIndex = i

            val dayWrapper = layoutInflater.inflate(R.layout.item_day_of_week, binding.dayContainer, false)
            val circle = dayWrapper.findViewById<LinearLayout>(R.id.day_pill)
            val tvDay = dayWrapper.findViewById<TextView>(R.id.tv_day_initial)
            val tvDate = dayWrapper.findViewById<TextView>(R.id.tv_day_number)

            tvDay.text = dayInitials[i]
            tvDate.text = dateOnlyFormat.format(currentDate)

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            params.setMargins(2, 0, 2, 0)
            dayWrapper.layoutParams = params

            binding.dayContainer.addView(dayWrapper)
            dayCircles.add(circle)

            val index = i
            dayWrapper.setOnClickListener {
                selectedIndex = index
                selectedDate = fullDateFormat.format(calendarDates[index])
                setupHeader()
                updateDaySelection()
                displaySchedulesForDate(selectedDate)
            }
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (selectedIndex != -1) updateDaySelection() else resetDaySelectionVisuals()
    }

    private fun updateDaySelection() {
        for (i in 0 until dayCircles.size) {
            val circle = dayCircles[i]
            val dayWrapper = circle.parent as View
            val tvDay = dayWrapper.findViewById<TextView>(R.id.tv_day_initial)
            val tvDate = dayWrapper.findViewById<TextView>(R.id.tv_day_number)

            if (i == selectedIndex) {
                circle.setBackgroundResource(R.drawable.bg_day_pill_selected)
                tvDay.setTextColor(Color.WHITE)
                tvDate.setTextColor(Color.WHITE)
            } else {
                circle.setBackgroundResource(R.drawable.bg_day_pill_unselected)
                tvDay.setTextColor(Color.parseColor("#8E8E93"))
                tvDate.setTextColor(Color.parseColor("#8E8E93"))
            }
        }
    }

    private fun resetDaySelectionVisuals() {
        for (circle in dayCircles) {
            val dayWrapper = circle.parent as View
            val tvDay = dayWrapper.findViewById<TextView>(R.id.tv_day_initial)
            val tvDate = dayWrapper.findViewById<TextView>(R.id.tv_day_number)
            circle.setBackgroundResource(R.drawable.bg_day_pill_unselected)
            tvDay.setTextColor(Color.parseColor("#8E8E93"))
            tvDate.setTextColor(Color.parseColor("#8E8E93"))
        }
    }
}