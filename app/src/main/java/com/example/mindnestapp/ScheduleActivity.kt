package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox // [BARU] Tambahkan import ini
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var tempCal: Calendar
    private var todayIndex: Int = 0
    private lateinit var footerHelper: FooterNavHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("schedules")

        // 2. Setup Helper & UI
        footerHelper = FooterNavHelper(this)
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
        binding.tvDate.text = headerFormat.format(Date())

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

    // --- LOGIKA UTAMA: MEMUAT JADWAL & CHECKBOX ---
    private fun loadSchedules() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Reset view agar tidak duplikat saat data berubah
                binding.scheduleContainer.removeAllViews()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(Schedule::class.java)
                        schedule?.id = child.key

                        // FILTER: Hanya tampilkan yang BELUM selesai (!isCompleted)
                        if (schedule != null && !schedule.isCompleted) {

                            // Inflate layout item
                            val view = layoutInflater.inflate(R.layout.item_schedule_home, binding.scheduleContainer, false)

                            // Binding ID dari XML item
                            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                            val tvPriority = view.findViewById<TextView>(R.id.tvPriority)
                            val tvDate = view.findViewById<TextView>(R.id.tvDate)
                            val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
                            val tvTime = view.findViewById<TextView>(R.id.tvTime)

                            // [PERUBAHAN UTAMA] Mengambil ID CheckBox
                            val cbComplete = view.findViewById<CheckBox>(R.id.cbComplete)

                            val btnEdit = view.findViewById<LinearLayout>(R.id.btnEdit)
//                            val btnSnooze = view.findViewById<LinearLayout>(R.id.btnSnooze)

                            // Tampilkan Data ke Layar
                            tvTitle.text = schedule.title
                            tvTime.text = schedule.time
                            tvCategory.text = schedule.category.ifEmpty { "General" }

                            // Format Tanggal
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("EEEE d MMM yyyy", Locale.getDefault())
                                val dateObj = inputFormat.parse(schedule.date)
                                if (dateObj != null) {
                                    tvDate.text = outputFormat.format(dateObj).uppercase()
                                } else {
                                    tvDate.text = schedule.date
                                }
                            } catch (_: Exception) {
                                tvDate.text = schedule.date
                            }

                            // Set Label Priority
                            if (schedule.priority.equals("Tinggi", true) || schedule.priority.equals("High", true)) {
                                tvPriority.text = "HIGH"
                            } else {
                                tvPriority.text = "MEDIUM"
                            }

                            // --- LOGIKA CHECKBOX COMPLETE ---
                            // Ketika checkbox diklik/dicentang
                            cbComplete.setOnClickListener {
                                if (cbComplete.isChecked) {
                                    if (schedule.id != null) {
                                        // Update Firebase: set isCompleted = true
                                        database.child(schedule.id!!).child("isCompleted").setValue(true)
                                            .addOnSuccessListener {
                                                Toast.makeText(this@ScheduleActivity, "Tugas Selesai!", Toast.LENGTH_SHORT).show()

                                                // Hapus view dari layar agar terlihat menghilang
                                                binding.scheduleContainer.removeView(view)
                                            }
                                            .addOnFailureListener {
                                                // Jika gagal (karena internet dsb), kembalikan checkbox jadi tidak tercentang
                                                cbComplete.isChecked = false
                                                Toast.makeText(this@ScheduleActivity, "Gagal mengupdate status", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }

                            // Tombol Edit
                            btnEdit.setOnClickListener {
                                val intent = Intent(this@ScheduleActivity, AddTaskActivity::class.java)
                                intent.putExtra("taskId", schedule.id)
                                intent.putExtra("title", schedule.title)
                                intent.putExtra("category", schedule.category)
                                intent.putExtra("date", schedule.date)
                                intent.putExtra("time", schedule.time)
                                intent.putExtra("notes", schedule.description)
                                intent.putExtra("isEditMode", true)
                                startActivity(intent)
                            }

//                            // Tombol Snooze
//                            btnSnooze.setOnClickListener {
//                                Toast.makeText(this@ScheduleActivity, "Snooze clicked", Toast.LENGTH_SHORT).show()
//                            }

                            // Masukkan kartu ke dalam container
                            binding.scheduleContainer.addView(view)
                        }
                    }
                } else {
                    Toast.makeText(this@ScheduleActivity, "Tidak ada jadwal aktif", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScheduleActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

        binding.dayContainer.removeAllViews()

        for (i in 0..6) {
            val dayWrapper = LinearLayout(this)
            dayWrapper.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            dayWrapper.gravity = Gravity.CENTER
            dayWrapper.orientation = LinearLayout.VERTICAL

            val circle = LinearLayout(this)
            circle.layoutParams = LinearLayout.LayoutParams(50.dp, 80.dp)
            circle.orientation = LinearLayout.VERTICAL
            circle.gravity = Gravity.CENTER
            val isToday = (i == todayIndex)

            circle.setBackgroundResource(if (isToday) R.drawable.bg_day_pill_selected else R.drawable.bg_day_pill_unselected)

            val tvDay = TextView(this)
            tvDay.text = dayInitials[i]
            tvDay.textSize = 14f
            tvDay.setTypeface(null, Typeface.BOLD)
            tvDay.gravity = Gravity.CENTER
            tvDay.setTextColor(if (isToday) Color.WHITE else "#8E8E93".toColorInt())

            val dateText = dateOnlyFormat.format(tempCal.time)
            val monthText = monthFormat.format(tempCal.time)

            val tvDate = TextView(this)
            val fullDateText = """
            $dateText
            $monthText
            """.trimIndent()
            tvDate.text = fullDateText
            tvDate.textSize = 10f
            tvDate.gravity = Gravity.CENTER
            tvDate.setTextColor(if (isToday) Color.WHITE else "#8E8E93".toColorInt())
            tvDate.setLines(2)

            circle.addView(tvDay)
            circle.addView(tvDate)
            dayWrapper.addView(circle)
            binding.dayContainer.addView(dayWrapper)
            dayCircles.add(circle)
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}