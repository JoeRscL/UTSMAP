package com.example.mindnestapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.*

class ViewScheduleActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var eventList: LinearLayout
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView

    private val calendar = Calendar.getInstance()
    private val events = mutableMapOf<String, List<String>>() // contoh data kegiatan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_schedule)

        // === Inisialisasi View ===
        calendarView = findViewById(R.id.calendarView)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        eventList = findViewById(R.id.eventList)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        // ✅ Tambahan: atur warna calendarView (ganti atribut XML yang error)
        val blue = ContextCompat.getColor(this, R.color.blue)
        val black = ContextCompat.getColor(this, R.color.black)

        calendarView.setSelectionColor(blue)
        calendarView.setArrowColor(black)

        // contoh data kegiatan
        events["2025-10-18"] = listOf("Meeting tim", "Review desain app")
        events["2025-10-19"] = listOf("Presentasi proyek", "Ngopi bareng")

        updateMonthTitle()

        // navigasi bulan
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            calendarView.goToPrevious()
            updateMonthTitle()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            calendarView.goToNext()
            updateMonthTitle()
        }

        // saat pilih tanggal
        calendarView.setOnDateChangedListener { _, date, _ ->
            val key = "${date.year}-${date.month + 1}-${date.day}"
            showEventsForDate(key)
        }

        // tampilkan event hari ini
        val today = CalendarDay.today()
        calendarView.setDateSelected(today, true)
        val todayKey = "${today.year}-${today.month + 1}-${today.day}"
        showEventsForDate(todayKey)
    }

    private fun updateMonthTitle() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvMonthYear.text = dateFormat.format(calendar.time)
    }

    private fun showEventsForDate(key: String) {
        eventList.removeAllViews()
        tvSelectedDate.text = "Kegiatan pada ${key.replace("-", " ")}:"
        val activities = events[key] ?: listOf("Tidak ada kegiatan")
        activities.forEach {
            val textView = TextView(this)
            textView.text = "• $it"
            textView.textSize = 16f
            eventList.addView(textView)
        }
    }
}
