package com.example.mindnestapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
            finish()
            return
        }
        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)

        footerHelper = FooterNavHelper(this)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())

        setupWeeklyCalendar()
        loadSchedules()
        footerHelper.setupFooterNavigation()

        binding.ivProfileSmall.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        setupHeader()
    }

    private fun setupHeader() {
        val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        try {
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            binding.tvDate.text = headerFormat.format(dateObj ?: Date())
        } catch (e: Exception) { /* Default */ }

        val user = auth.currentUser ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java)
                val displayName = if (!firstName.isNullOrBlank()) firstName else "User"
                binding.tvUserName.text = "Hi, $displayName"

                val base64Image = snapshot.child("profileImageBase64").getValue(String::class.java)
                if (!base64Image.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.ivProfileSmall.setImageBitmap(decodedImage)
                    } catch (e: Exception) {
                        binding.ivProfileSmall.setImageResource(R.drawable.ic_default_profile)
                    }
                } else {
                    binding.ivProfileSmall.setImageResource(R.drawable.ic_default_profile)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvUserName.text = "Hi, User"
                binding.ivProfileSmall.setImageResource(R.drawable.ic_default_profile)
            }
        })
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
                displaySchedulesForDate(selectedDate)
            }
            override fun onCancelled(error: DatabaseError) { /*...*/ }
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
                    "work" -> R.drawable.ic_work
                    "gym" -> R.drawable.ic_gym
                    "doctor" -> R.drawable.ic_doctor
                    "study" -> R.drawable.ic_file
                    "home" -> R.drawable.ic_home
                    else -> R.drawable.ic_calendar
                }
                ivCategory.setImageResource(categoryIcon)

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

                view.setOnClickListener { 
                    val intent = Intent(this, AddTaskActivity::class.java)
                    intent.putExtra("taskId", schedule.id)
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
        val dateWithMonthFormat = SimpleDateFormat("d MMM", Locale.getDefault()) // Format baru: 2 Oct

        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        
        val todayCal = Calendar.getInstance()
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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

            val dayWrapper = layoutInflater.inflate(R.layout.item_day_of_week, binding.dayContainer, false)
            val circle = dayWrapper.findViewById<LinearLayout>(R.id.day_pill)
            val tvDay = dayWrapper.findViewById<TextView>(R.id.tv_day_initial)
            val tvDate = dayWrapper.findViewById<TextView>(R.id.tv_day_number)
            
            tvDay.text = dayInitials[i]
            tvDate.text = dateWithMonthFormat.format(currentDate) // Gunakan format baru

            binding.dayContainer.addView(dayWrapper)
            dayCircles.add(circle)
            
            val index = i
            dayWrapper.setOnClickListener {
                selectedIndex = index
                val newDate = calendarDates[index]
                selectedDate = fullDateFormat.format(newDate)
                
                setupHeader()
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
            val dayWrapper = circle.parent as View
            val tvDay = dayWrapper.findViewById<TextView>(R.id.tv_day_initial)
            val tvDate = dayWrapper.findViewById<TextView>(R.id.tv_day_number)

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
}
