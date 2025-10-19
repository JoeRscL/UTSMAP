package com.example.mindnestapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
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
    private val database = FirebaseDatabase.getInstance().getReference("schedules")

    // Variabel untuk menyimpan tanggal dan waktu yang dipilih pengguna
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Setup Listeners ---
        setupClickListeners()
        setupFooterNavigation()
    }

    private fun setupClickListeners() {
        // Tombol simpan (FAB)
        binding.fabSave.setOnClickListener {
            saveTaskToFirebase()
        }

        // Tombol cancel di header
        binding.btnCancel.setOnClickListener {
            finish() // Menutup activity
        }

        // Layout untuk memilih tanggal
        binding.layoutSetDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Layout untuk memilih waktu
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
            // Format tanggal yang dipilih menjadi "yyyy-MM-dd"
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(selectedCalendar.time)

            // Tampilkan tanggal yang dipilih di UI
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
            // Format waktu yang dipilih menjadi "HH:mm"
            selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

            // Tampilkan waktu yang dipilih di UI
            binding.tvSetTime.text = selectedTime
            binding.tvSetTime.setTextColor(resources.getColor(android.R.color.black, theme))
        }, hour, minute, true) // 'true' untuk format 24 jam

        timePickerDialog.show()
    }

    private fun saveTaskToFirebase() {
        // Ambil data dari semua field di layout
        val title = binding.etTaskTitle.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        // Validasi: Pastikan field judul, tanggal, dan waktu tidak kosong
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

        Toast.makeText(this, "Menyimpan tugas...", Toast.LENGTH_SHORT).show()

        // Dapatkan ID jadwal terakhir untuk membuat ID baru
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

                // Buat objek data schedule dengan data yang sudah dipilih pengguna
                val taskData = mapOf(
                    "title" to title,
                    "description" to notes,
                    "priority" to if (category.isNotEmpty()) category else "Sedang",
                    "date" to selectedDate, // Gunakan tanggal yang dipilih pengguna
                    "time" to selectedTime  // Gunakan waktu yang dipilih pengguna
                )

                // Simpan data ke Firebase dengan ID baru
                database.child(newScheduleId).setValue(taskData)
                    .addOnSuccessListener {
                        Toast.makeText(this@AddTaskActivity, "Tugas '$title' berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@AddTaskActivity, ScheduleActivity::class.java))
                        finishAffinity()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@AddTaskActivity, "Gagal menyimpan: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddTaskActivity, "Gagal mengakses database: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
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
            // Sudah di halaman ini, tidak perlu melakukan apa-apa
        }
        binding.footerNav.navFile.setOnClickListener {
            Toast.makeText(this, "Fitur File belum tersedia", Toast.LENGTH_SHORT).show()
        }
        binding.footerNav.navSettings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
