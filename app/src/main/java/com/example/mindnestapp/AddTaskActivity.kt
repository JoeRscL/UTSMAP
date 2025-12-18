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

    // Launcher untuk meminta izin notifikasi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Jika izin diberikan, beri tahu user untuk menyimpan lagi
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
                    override fun onCancelled(error: DatabaseError) { /* ... */ }
                })
            }
        }
    }
    
    private fun populateFormFromSnapshot(snapshot: DataSnapshot) {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
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
        val title = binding.etTaskTitle.text.toString().trim()
        val isReminderOn = binding.switchReminder.isChecked

        if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Judul, tanggal, dan waktu harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // **PERBAIKAN: Minta izin sebelum menyimpan**
        if (isReminderOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return // Hentikan proses simpan, tunggu user memberikan izin
            }
        }

        // --- Lanjutkan proses simpan jika izin sudah ada atau tidak diperlukan ---
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
                cancelAlarm(finalTaskId)
            }
            Toast.makeText(this, if (isEditMode) "Task Updated!" else "Task Saved!", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }.addOnFailureListener { 
            Toast.makeText(this, "Gagal menyimpan: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun scheduleAlarm(taskId: String, taskTitle: String) {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun cancelAlarm(taskId: String) {
       // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun showDeleteConfirmationDialog() { 
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }
    
    private fun deleteTask() {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun showCategorySelectionDialog() {
       // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun showLevelSelectionDialog() {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun showDatePickerDialog() {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }

    private fun showTimePickerDialog() {
        // ... (Fungsi ini tetap sama, tidak perlu diubah)
    }
}
