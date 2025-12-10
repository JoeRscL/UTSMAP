package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityScheduleBinding
import com.google.firebase.auth.FirebaseAuth
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

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var dayCircles: MutableList<LinearLayout>
    private lateinit var days: List<String>
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var tempCal: Calendar
    private var todayIndex: Int = 0
    private lateinit var footerHelper: FooterNavHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        // Inisialisasi Footer Helper
        footerHelper = FooterNavHelper(this)

        // Setup Header
        setupHeader()

        // --- KALENDER MINGGUAN REALTIME ---
        setupWeeklyCalendar()

        // --- LOAD JADWAL DARI FIREBASE ---
        loadSchedules()

        // Setup Footer Navigation menggunakan Helper
        footerHelper.setupFooterNavigation()
        
        // Klik icon profil untuk ke settings
        binding.ivProfileSmall.setOnClickListener { // ID diperbaiki kembali ke ivProfileSmall
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupHeader() {
        // 1. Set Tanggal Hari Ini
        val headerFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        binding.tvDate.text = headerFormat.format(Date())

        // 2. Ambil Nama User dari Firebase
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("nama").getValue(String::class.java)
                    val displayName = if (!name.isNullOrEmpty()) {
                        name
                    } else {
                        val email = currentUser.email
                        if (email != null) {
                            email.substringBefore("@")
                        } else {
                            "User"
                        }
                    }
                    binding.tvUserName.text = "Hi, $displayName"
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.tvUserName.text = "Hi, User"
                }
            })
        } else {
            binding.tvUserName.text = "Hi, Guest"
        }
    }

    private fun setupWeeklyCalendar() {
        dayCircles = mutableListOf()
        val dayInitials = listOf("S", "M", "T", "W", "T", "F", "S")
        
        val dateOnlyFormat = SimpleDateFormat("d", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        tempCal = Calendar.getInstance()
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val currentCal = Calendar.getInstance()
        todayIndex = currentCal.get(Calendar.DAY_OF_WEEK) - 1 

        for (i in 0..6) {
            val dayWrapper = LinearLayout(this)
            val wrapperParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            dayWrapper.layoutParams = wrapperParams
            dayWrapper.gravity = Gravity.CENTER
            dayWrapper.orientation = LinearLayout.VERTICAL

            val circle = LinearLayout(this)
            val circleParams = LinearLayout.LayoutParams(50.dp, 80.dp)
            circle.layoutParams = circleParams
            circle.orientation = LinearLayout.VERTICAL
            circle.gravity = Gravity.CENTER
            
            val isToday = (i == todayIndex)

            circle.setBackgroundResource(
                if (isToday) R.drawable.bg_day_pill_selected else R.drawable.bg_day_pill_unselected
            )

            val tvDay = TextView(this)
            tvDay.text = dayInitials[i]
            tvDay.textSize = 14f
            tvDay.setTypeface(null, Typeface.BOLD)
            tvDay.gravity = Gravity.CENTER
            tvDay.setTextColor(
                if (isToday) Color.WHITE else Color.parseColor("#8E8E93")
            )

            val dateStr = "${dateOnlyFormat.format(tempCal.time)}\n${monthFormat.format(tempCal.time)}"
            val tvDate = TextView(this)
            tvDate.text = dateStr
            tvDate.textSize = 10f
            tvDate.gravity = Gravity.CENTER
            tvDate.setTextColor(
                if (isToday) Color.WHITE else Color.parseColor("#8E8E93")
            )
            tvDate.setLines(2)

            circle.addView(tvDay)
            circle.addView(tvDate)

            dayWrapper.addView(circle)
            binding.dayContainer.addView(dayWrapper)

            dayCircles.add(circle)

            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun loadSchedules() {
        val scheduleRef = FirebaseDatabase.getInstance().getReference("schedules")

        scheduleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.scheduleContainer.removeAllViews()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.let {
                            val view = layoutInflater.inflate(R.layout.item_schedule_home, binding.scheduleContainer, false)
                            
                            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                            val tvPriority = view.findViewById<TextView>(R.id.tvPriority)
                            val tvDate = view.findViewById<TextView>(R.id.tvDate)
                            val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
                            val tvTime = view.findViewById<TextView>(R.id.tvTime)

                            tvTitle.text = it.title
                            
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("EEEE d MMM yyyy", Locale.getDefault())
                                val dateObj = inputFormat.parse(it.date)
                                if (dateObj != null) {
                                    tvDate.text = outputFormat.format(dateObj).uppercase()
                                }
                            } catch (e: Exception) {
                                tvDate.text = it.date
                            }

                            tvTime.text = it.time
                            tvCategory.text = it.priority

                            if (it.title.contains("Penting", true) || it.title.contains("Urgent", true)) {
                                tvPriority.text = "HIGH"
                            } else {
                                tvPriority.text = "MEDIUM"
                            }

                            binding.scheduleContainer.addView(view)
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
    }

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
