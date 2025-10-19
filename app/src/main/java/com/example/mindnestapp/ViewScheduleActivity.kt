package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
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

data class ScheduleItem(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val priority: String = ""
)

class EventDecorator(private val color: Int, dates: Collection<CalendarDay>) : DayViewDecorator {
    private val dates: HashSet<CalendarDay> = HashSet(dates)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(7f, color))
    }
}

class ViewScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewScheduleBinding
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("schedules")
    private val allSchedules = mutableMapOf<String, MutableList<ScheduleItem>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar()
        setupListeners()
        fetchSchedulesFromFirebase()
        setupFooterNavigation()
    }

    private fun setupCalendar() {
        binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            .format(binding.calendarView.currentDate.date)
        binding.calendarView.selectedDate = CalendarDay.today()
    }

    private fun setupListeners() {
        binding.calendarView.setOnMonthChangedListener { _, date ->
            binding.tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                .format(date.date)
        }

        binding.btnPrevMonth.setOnClickListener { binding.calendarView.goToPrevious() }
        binding.btnNextMonth.setOnClickListener { binding.calendarView.goToNext() }

        binding.calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                val selectedDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.date)
                // INI BAGIAN PENTING: Memanggil fungsi untuk menampilkan data sesuai tanggal
                showSchedulesForDate(selectedDateKey)
            }
        }
    }

    private fun fetchSchedulesFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSchedules.clear()
                for (scheduleSnapshot in snapshot.children) {
                    val schedule = scheduleSnapshot.getValue(ScheduleItem::class.java)
                    schedule?.let {
                        if (it.date.isNotBlank()) { // Pastikan data tanggal tidak kosong
                            if (!allSchedules.containsKey(it.date)) {
                                allSchedules[it.date] = mutableListOf()
                            }
                            allSchedules[it.date]?.add(it)
                        }
                    }
                }
                updateCalendarDecorators()
                // Setelah data terambil, langsung update tampilan untuk tanggal yang sedang terpilih
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
        val eventDates = allSchedules.keys.mapNotNull { dateString ->
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.let { CalendarDay.from(it) }
            } catch (e: Exception) {
                null
            }
        }
        binding.calendarView.removeDecorators() // Hapus decorator lama
        binding.calendarView.addDecorator(EventDecorator(Color.BLUE, eventDates)) // Tambahkan yang baru
    }

    private fun showSchedulesForDate(dateKey: String) {
        // Gunakan binding secara konsisten dan benar
        binding.eventList.removeAllViews()
        binding.tvSelectedDate.text = "Kegiatan pada $dateKey:"

        // Mengambil data dari map HANYA untuk tanggal yang dipilih
        val schedulesForDate = allSchedules[dateKey]

        if (schedulesForDate.isNullOrEmpty()) {
            // Tampilkan pesan jika tidak ada jadwal
            val noEventView = layoutInflater.inflate(R.layout.item_no_schedule, binding.eventList, false)
            binding.eventList.addView(noEventView)
        } else {
            // Loop dan tampilkan HANYA jadwal untuk tanggal yang dipilih
            schedulesForDate.forEach { schedule ->
                val itemView = layoutInflater.inflate(R.layout.item_schedule, binding.eventList, false)

                // Menghubungkan ke ID yang ada di 'item_schedule.xml'
                val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
                val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
                val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
                val tvPriority = itemView.findViewById<TextView>(R.id.tvPriority)
                val tvCategory = itemView.findViewById<TextView>(R.id.tvCategory)

                // Mengisi data ke view
                tvTitle.text = schedule.title
                tvDate.text = schedule.date
                tvTime.text = schedule.time
                tvPriority.text = schedule.priority
                tvCategory.text = schedule.description

                binding.eventList.addView(itemView)
            }
        }
    }

    private fun setupFooterNavigation() {
        binding.footerNav.navHome.setOnClickListener {
            // Arahkan ke MainActivity jika ada, atau beri pesan
            // startActivity(Intent(this, MainActivity::class.java))
            Toast.makeText(this, "Fitur Home belum tersedia", Toast.LENGTH_SHORT).show()
        }
        binding.footerNav.navCalendar.setOnClickListener { /* Sudah di halaman ini */ }
        binding.footerNav.navAdd.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
        binding.footerNav.navFile.setOnClickListener {
            Toast.makeText(this, "Fitur File belum tersedia", Toast.LENGTH_SHORT).show()
        }
        binding.footerNav.navSettings.setOnClickListener {
            // Arahkan ke ProfileActivity
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
