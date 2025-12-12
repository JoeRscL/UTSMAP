package com.example.mindnestapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityAddTaskBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    // Pastikan path database SAMA dengan ScheduleActivity ("schedules")
    private val database = FirebaseDatabase.getInstance().getReference("schedules")

    // Variabel untuk menyimpan tanggal dan waktu
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    // --- TAMBAHAN UNTUK FITUR EDIT ---
    private var isEditMode = false
    private var taskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. CEK DATA DARI INTENT (APAKAH INI EDIT MODE?)
        checkEditMode()

        // --- Setup Listeners ---
        setupClickListeners()
        setupFooterNavigation()
    }

    private fun checkEditMode() {
        // Mengecek apakah ada data "isEditMode" yang dikirim dari ScheduleActivity
        if (intent.getBooleanExtra("isEditMode", false)) {
            isEditMode = true
            taskId = intent.getStringExtra("taskId")

            // Ambil data lama
            val oldTitle = intent.getStringExtra("title")
            val oldCategory = intent.getStringExtra("category")
            val oldDate = intent.getStringExtra("date")
            val oldTime = intent.getStringExtra("time")
            val oldNotes = intent.getStringExtra("notes")

            // Isi Form dengan Data Lama
            binding.etTaskTitle.setText(oldTitle)
            binding.etCategory.setText(oldCategory)
            binding.etNotes.setText(oldNotes)

            // Set variabel tanggal/waktu agar tidak kosong saat disimpan
            selectedDate = oldDate ?: ""
            selectedTime = oldTime ?: ""

            // Tampilkan di UI
            binding.tvSetDate.text = selectedDate
            binding.tvSetTime.text = selectedTime

            // Ubah teks tombol (Opsional)
            // binding.btnSave.text = "Update Task"
        }
    }

    private fun setupClickListeners() {
        binding.fabSave.setOnClickListener {
            saveTaskToFirebase()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.layoutSetDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.layoutSetTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(selectedCalendar.time)

            binding.tvSetDate.text = selectedDate
            binding.tvSetDate.setTextColor(resources.getColor(android.R.color.black, theme))
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

            binding.tvSetTime.text = selectedTime
            binding.tvSetTime.setTextColor(resources.getColor(android.R.color.black, theme))
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun saveTaskToFirebase() {
        val title = binding.etTaskTitle.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Judul tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Silakan atur tanggal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Silakan atur waktu terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Menyimpan...", Toast.LENGTH_SHORT).show()

        val taskData = mapOf(
            "title" to title,
            "description" to notes,
            "priority" to if (category.isNotEmpty()) category else "Sedang", // Menggunakan field 'priority' untuk kategori sesuai kode lama Anda
            "date" to selectedDate,
            "time" to selectedTime,
            "isCompleted" to false // Pastikan status reset ke false atau tetap false
        )

        // --- LOGIKA PENYIMPANAN (BARU VS EDIT) ---
        if (isEditMode && taskId != null) {
            // JIKA EDIT: Update ID yang lama, JANGAN buat baru
            database.child(taskId!!).updateChildren(taskData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Jadwal Berhasil Diubah!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    finishAffinity()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal Update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // JIKA BARU: Buat ID baru (Logic Anda yang lama)
            database.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var newScheduleIdNumber = 1
                    if (snapshot.exists()) {
                        val lastScheduleKey = snapshot.children.first().key
                        lastScheduleKey?.let {
                            val lastNumber = it.substringAfter("schedule_").toIntOrNull() ?: 0
                            newScheduleIdNumber = lastNumber + 1
                        }
                    }

                    val newScheduleId = "schedule_${String.format("%03d", newScheduleIdNumber)}"

                    database.child(newScheduleId).setValue(taskData)
                        .addOnSuccessListener {
                            Toast.makeText(this@AddTaskActivity, "Tugas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AddTaskActivity, ScheduleActivity::class.java))
                            finishAffinity()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AddTaskActivity, "Gagal menyimpan: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddTaskActivity, "Error DB: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun setupFooterNavigation() {
        binding.footerNav.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.footerNav.navCalendar.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
            finish()
        }
        binding.footerNav.navAdd.setOnClickListener {
            // Do nothing
        }
        binding.footerNav.navSettings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}