package com.example.mindnestapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
    private lateinit var footerHelper: FooterNavHelper

    // Variabel untuk menyimpan tanggal dan waktu
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedCategory: String = ""
    private var selectedLevel: String = ""

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

        // --- Setup Footer ---
        footerHelper = FooterNavHelper(this)
        footerHelper.setupFooterNavigation()
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
            val oldPriority = intent.getStringExtra("priority")

            // Isi Form dengan Data Lama
            binding.etTaskTitle.setText(oldTitle)
            binding.etNotes.setText(oldNotes)

            // Set Category
            if (!oldCategory.isNullOrEmpty()) {
                selectedCategory = oldCategory
                binding.tvCategorySelect.text = selectedCategory
                binding.tvCategorySelect.setTextColor(Color.BLACK)
                binding.etCategory.setText(selectedCategory)
            }
            
            // Set Level (Priority)
            if (!oldPriority.isNullOrEmpty()) {
                selectedLevel = oldPriority
                binding.tvLevelSelect.text = selectedLevel
                binding.tvLevelSelect.setTextColor(Color.BLACK)
            }

            // Set variabel tanggal/waktu agar tidak kosong saat disimpan
            selectedDate = oldDate ?: ""
            selectedTime = oldTime ?: ""

            // Tampilkan di UI
            if (selectedDate.isNotEmpty()) {
                binding.tvSetDate.text = selectedDate
                binding.tvSetDate.setTextColor(Color.BLACK)
            }
            if (selectedTime.isNotEmpty()) {
                binding.tvSetTime.text = selectedTime
                binding.tvSetTime.setTextColor(Color.BLACK)
            }

            // --- UI ADJUSTMENTS FOR EDIT MODE ---
            binding.tvHeaderTitle.text = "Edit Task"
            binding.fabSave.setImageResource(R.drawable.ic_check_white) // Checkmark icon for Accept
            
            // Show Delete Button
            binding.fabDelete.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        // Accept Change / Save Button
        binding.fabSave.setOnClickListener {
            saveTaskToFirebase()
        }

        // Delete Button (Only visible in edit mode)
        binding.fabDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.tvCategorySelect.setOnClickListener {
            showCategorySelectionDialog()
        }
        
        binding.tvLevelSelect.setOnClickListener {
            showLevelSelectionDialog()
        }

        binding.layoutSetDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.layoutSetTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteTask()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTask() {
        if (taskId != null) {
            database.child(taskId!!).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Task Deleted!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    finishAffinity()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showCategorySelectionDialog() {
        // List of categories (based on ScheduleActivity icons)
        val categories = arrayOf("Work", "Gym", "Doctor", "Study", "Home", "Other")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Category")
        builder.setItems(categories) { _, which ->
            selectedCategory = categories[which]
            binding.tvCategorySelect.text = selectedCategory
            binding.tvCategorySelect.setTextColor(Color.BLACK) // Change text color to black indicating selection
            binding.etCategory.setText(selectedCategory)
        }
        builder.show()
    }
    
    private fun showLevelSelectionDialog() {
        val levels = arrayOf("High", "Medium", "Low")
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Urgency Level")
        builder.setItems(levels) { _, which ->
            selectedLevel = levels[which]
            binding.tvLevelSelect.text = selectedLevel
            binding.tvLevelSelect.setTextColor(Color.BLACK)
        }
        builder.show()
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
            binding.tvSetDate.setTextColor(Color.BLACK)
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
            binding.tvSetTime.setTextColor(Color.BLACK)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun saveTaskToFirebase() {
        val title = binding.etTaskTitle.text.toString().trim()
        val category = binding.etCategory.text.toString().trim() // Or use selectedCategory directly
        val notes = binding.etNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Default level if not selected
        if (selectedLevel.isEmpty()) {
            selectedLevel = "Medium"
        }

        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()

        val taskData = mapOf(
            "title" to title,
            "description" to notes,
            "category" to category, 
            "priority" to selectedLevel, // Use selected level
            "date" to selectedDate,
            "time" to selectedTime,
            "isCompleted" to false
        )

        // --- LOGIKA PENYIMPANAN (BARU VS EDIT) ---
        if (isEditMode && taskId != null) {
            // JIKA EDIT: Update ID yang lama, JANGAN buat baru
            database.child(taskId!!).updateChildren(taskData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Task Updated!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ScheduleActivity::class.java))
                    finishAffinity()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@AddTaskActivity, "Task Saved!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AddTaskActivity, ScheduleActivity::class.java))
                            finishAffinity()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AddTaskActivity, "Failed to save: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddTaskActivity, "DB Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}