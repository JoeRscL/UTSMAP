package com.example.mindnestapp

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mindnestapp.databinding.ActivityAddTaskBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var footerHelper: FooterNavHelper

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedCategory: String = ""
    private var selectedLevel: String = ""

    private var isEditMode = false
    private var taskId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Izin notifikasi diberikan. Silakan simpan tugas lagi.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Pengingat tidak akan berfungsi tanpa izin notifikasi.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        database = FirebaseDatabase.getInstance().getReference("schedules").child(currentUser.uid)

        checkEditMode()
        setupClickListeners()

        footerHelper = FooterNavHelper(this)
        footerHelper.setupFooterNavigation()
    }

    private fun checkEditMode() {
        if (intent.getBooleanExtra("isEditMode", false)) {
            isEditMode = true
            taskId = intent.getStringExtra("taskId")

            binding.tvHeaderTitle.text = "Edit Task"
            binding.fabSave.setImageResource(R.drawable.ic_check_white)
            binding.fabDelete.visibility = View.VISIBLE

            if (taskId != null) {
                database.child(taskId!!).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) populateFormFromSnapshot(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) { 
                        Toast.makeText(this@AddTaskActivity, "Gagal memuat data.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
    
    private fun populateFormFromSnapshot(snapshot: DataSnapshot) {
        val task = snapshot.getValue(ScheduleActivity.Schedule::class.java)
        task?.let {
            binding.etTaskTitle.setText(it.title)
            binding.etNotes.setText(it.description)

            selectedCategory = it.category
            if (selectedCategory.isNotEmpty()) {
                binding.tvCategorySelect.text = selectedCategory
                binding.tvCategorySelect.setTextColor(Color.BLACK)
            } else {
                 binding.tvCategorySelect.text = "Please select category"
                 binding.tvCategorySelect.setTextColor(Color.parseColor("#8E8E93"))
            }
            binding.etCategory.setText(selectedCategory)

            selectedLevel = it.priority
            if (selectedLevel.isNotEmpty()) {
                binding.tvLevelSelect.text = selectedLevel
                binding.tvLevelSelect.setTextColor(Color.BLACK)
            } else {
                binding.tvLevelSelect.text = "Select urgency level"
                binding.tvLevelSelect.setTextColor(Color.parseColor("#8E8E93"))
            }

            selectedDate = it.date
            if (selectedDate.isNotEmpty()) {
                binding.tvSetDate.text = selectedDate
                binding.tvSetDate.setTextColor(Color.BLACK)
            }

            selectedTime = it.time
            if (selectedTime.isNotEmpty()) {
                binding.tvSetTime.text = selectedTime
                binding.tvSetTime.setTextColor(Color.BLACK)
            }

            binding.switchReminder.isChecked = snapshot.child("reminderEnabled").getValue(Boolean::class.java) ?: false
        }
    }

    private fun setupClickListeners() {
        binding.fabSave.setOnClickListener { saveTaskToFirebase() }
        binding.fabDelete.setOnClickListener { showDeleteConfirmationDialog() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.tvCategorySelect.setOnClickListener { showCategorySelectionDialog() }
        binding.tvLevelSelect.setOnClickListener { showLevelSelectionDialog() }
        binding.layoutSetDate.setOnClickListener { showDatePickerDialog() }
        binding.layoutSetTime.setOnClickListener { showTimePickerDialog() }
    }

    private fun saveTaskToFirebase() {
        try {
            val title = binding.etTaskTitle.text.toString().trim()
            val isReminderOn = binding.switchReminder.isChecked

            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Judul, tanggal, dan waktu harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (isReminderOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }

            val taskData = mapOf(
                "title" to title,
                "description" to binding.etNotes.text.toString().trim(),
                "category" to selectedCategory,
                "priority" to (if (selectedLevel.isEmpty()) "Medium" else selectedLevel),
                "date" to selectedDate,
                "time" to selectedTime,
                "isCompleted" to false,
                "reminderEnabled" to isReminderOn
            )
            
            val finalTaskId = if (isEditMode) taskId!! else database.push().key!!

            database.child(finalTaskId).setValue(taskData).addOnSuccessListener {
                if (isReminderOn) {
                    scheduleAlarm(finalTaskId, title)
                } else {
                    if(isEditMode) cancelAlarm(finalTaskId)
                }
                Toast.makeText(this, if (isEditMode) "Task Updated!" else "Task Saved!", Toast.LENGTH_SHORT).show()
                
                // **PERBAIKAN KRUSIAL**: Mengganti finishAffinity() dengan finish()
                finish() 

            }.addOnFailureListener { 
                Toast.makeText(this, "Gagal menyimpan: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("AddTaskActivity", "Error in saveTaskToFirebase", e)
            Toast.makeText(this, "Terjadi error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getStableRequestCode(taskId: String): Int {
        return abs(taskId.takeLast(6).hashCode())
    }

    private fun scheduleAlarm(taskId: String, taskTitle: String) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("TASK_ID", taskId)
                putExtra("TASK_TITLE", taskTitle)
            }
            
            val requestCode = getStableRequestCode(taskId)
            val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

            val calendar = Calendar.getInstance().apply {
                val dateParts = selectedDate.split("-")
                val timeParts = selectedTime.split(":")
                set(Calendar.YEAR, dateParts[0].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } 
        } catch (e: Exception) {
            Log.e("AddTaskActivity", "Error scheduling alarm", e)
            Toast.makeText(this, "Gagal menjadwalkan alarm: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelAlarm(taskId: String) {
       try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            val requestCode = getStableRequestCode(taskId)
            val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
       } catch (e: Exception) {
            Log.e("AddTaskActivity", "Error canceling alarm", e)
            Toast.makeText(this, "Gagal membatalkan alarm: ${e.message}", Toast.LENGTH_LONG).show()
       }
    }

    private fun showDeleteConfirmationDialog() { 
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { dialog, _ ->
                taskId?.let { cancelAlarm(it) }
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
                    finish() // Cukup finish, tidak perlu ke home
                }
        }
    }

    private fun showCategorySelectionDialog() {
        val categories = arrayOf("Work", "Gym", "Doctor", "Study", "Home", "Other")
        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setItems(categories) { dialog, which ->
                selectedCategory = categories[which]
                binding.tvCategorySelect.text = selectedCategory
                binding.tvCategorySelect.setTextColor(Color.BLACK)
                binding.etCategory.setText(selectedCategory)
                dialog.dismiss()
            }
            .show()
    }

    private fun showLevelSelectionDialog() {
        val levels = arrayOf("High", "Medium", "Low")
        AlertDialog.Builder(this)
            .setTitle("Select Urgency Level")
            .setItems(levels) { dialog, which ->
                selectedLevel = levels[which]
                binding.tvLevelSelect.text = selectedLevel
                binding.tvLevelSelect.setTextColor(Color.BLACK)
                dialog.dismiss()
            }
            .show()
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
}
