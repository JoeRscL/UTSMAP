package com.example.mindnestapp

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mindnestapp.databinding.ActivityAddTaskBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
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

    private var isTaskCompleted: Boolean = false
    private var isEditMode = false
    private var taskId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Pengingat tidak berfungsi tanpa izin.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
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
            binding.btnMarkDone.visibility = View.VISIBLE

            if (taskId != null) {
                database.child(taskId!!).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) populateFormFromSnapshot(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun populateFormFromSnapshot(snapshot: DataSnapshot) {
        val title = snapshot.child("title").getValue(String::class.java) ?: ""
        val description = snapshot.child("description").getValue(String::class.java) ?: ""
        val category = snapshot.child("category").getValue(String::class.java) ?: ""
        val priority = snapshot.child("priority").getValue(String::class.java) ?: ""
        val date = snapshot.child("date").getValue(String::class.java) ?: ""
        val time = snapshot.child("time").getValue(String::class.java) ?: ""
        val reminderEnabled = snapshot.child("reminderEnabled").getValue(Boolean::class.java) ?: false
        isTaskCompleted = snapshot.child("isCompleted").getValue(Boolean::class.java) ?: false

        binding.etTaskTitle.setText(title)
        binding.etNotes.setText(description)

        selectedCategory = category
        binding.tvCategorySelect.text = if (category.isNotEmpty()) category else "Select Category"
        binding.etCategory.setText(selectedCategory)

        selectedLevel = priority
        binding.tvLevelSelect.text = if (priority.isNotEmpty()) priority else "Select Level"

        selectedDate = date
        binding.tvSetDate.text = if (date.isNotEmpty()) date else "Set Date"

        selectedTime = time
        binding.tvSetTime.text = if (time.isNotEmpty()) time else "Set Time"

        binding.switchReminder.isChecked = reminderEnabled

        updateDoneButtonState()
    }

    private fun updateDoneButtonState() {
        if (isTaskCompleted) {
            binding.btnMarkDone.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            binding.etTaskTitle.paintFlags = binding.etTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            setFormEditable(false)
        } else {
            binding.btnMarkDone.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#34C759"))
            binding.etTaskTitle.paintFlags = binding.etTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            setFormEditable(true)
        }
    }

    private fun setFormEditable(isEditable: Boolean) {
        binding.etTaskTitle.isEnabled = isEditable
        binding.etNotes.isEnabled = isEditable
        binding.etCategory.isEnabled = isEditable
        binding.layoutSetDate.isEnabled = isEditable
        binding.layoutSetDate.isClickable = isEditable
        binding.layoutSetTime.isEnabled = isEditable
        binding.layoutSetTime.isClickable = isEditable
        binding.tvCategorySelect.isEnabled = isEditable
        binding.tvCategorySelect.isClickable = isEditable
        binding.tvLevelSelect.isEnabled = isEditable
        binding.tvLevelSelect.isClickable = isEditable
        binding.switchReminder.isEnabled = isEditable

        if (isEditable) binding.fabSave.show() else binding.fabSave.hide()
    }

    private fun setupClickListeners() {
        binding.fabSave.setOnClickListener { saveTaskToFirebase() }
        binding.fabDelete.setOnClickListener { showDeleteConfirmationDialog() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnMarkDone.setOnClickListener { toggleTaskCompletion() }

        binding.tvCategorySelect.setOnClickListener { showCategorySelectionDialog() }
        binding.tvLevelSelect.setOnClickListener { showLevelSelectionDialog() }
        binding.layoutSetDate.setOnClickListener { showDatePickerDialog() }
        binding.layoutSetTime.setOnClickListener { showTimePickerDialog() }
    }

    private fun toggleTaskCompletion() {
        if (taskId == null) return
        isTaskCompleted = !isTaskCompleted

        database.child(taskId!!).child("isCompleted").setValue(isTaskCompleted)
            .addOnSuccessListener {
                val msg = if (isTaskCompleted) "Tugas Selesai! 🎉" else "Tugas Dibuka Kembali"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                if (isTaskCompleted) cancelAlarm(taskId!!)
                updateDoneButtonState()
            }
    }

    // === BAGIAN UTAMA YANG DIUBAH ADA DI SINI ===
    private fun saveTaskToFirebase() {
        try {
            val title = binding.etTaskTitle.text.toString().trim()
            val isReminderOn = binding.switchReminder.isChecked

            if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Judul, tanggal, dan waktu harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            val taskData = mapOf(
                "title" to title,
                "description" to binding.etNotes.text.toString().trim(),
                "category" to selectedCategory,
                "priority" to (if (selectedLevel.isEmpty()) "Medium" else selectedLevel),
                "date" to selectedDate,
                "time" to selectedTime,
                "isCompleted" to isTaskCompleted,
                "reminderEnabled" to isReminderOn
            )

            val finalTaskId = if (isEditMode) taskId!! else database.push().key!!

            database.child(finalTaskId).setValue(taskData).addOnSuccessListener {
                // Atur Alarm
                if (isReminderOn && !isTaskCompleted) {
                    scheduleAlarm(finalTaskId, title)
                } else {
                    cancelAlarm(finalTaskId)
                }

                Toast.makeText(this, "Task Saved!", Toast.LENGTH_SHORT).show()

                // === PERUBAHAN: Kembali ke ScheduleActivity (Home) & Refresh ===
                val intent = Intent(this, ScheduleActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // Tutup activity AddTask
                // ==============================================================
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getStableRequestCode(taskId: String): Int = abs(taskId.takeLast(6).hashCode())

    private fun scheduleAlarm(taskId: String, taskTitle: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", taskTitle)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, getStableRequestCode(taskId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val cal = Calendar.getInstance()
        val d = selectedDate.split("-"); val t = selectedTime.split(":")
        cal.set(d[0].toInt(), d[1].toInt()-1, d[2].toInt(), t[0].toInt(), t[1].toInt(), 0)

        if(cal.timeInMillis > System.currentTimeMillis()) {
            try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent) }
            catch (e: SecurityException) { /* Handle permission */ }
        }
    }

    private fun cancelAlarm(taskId: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, getStableRequestCode(taskId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this).setTitle("Delete").setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                taskId?.let { cancelAlarm(it); database.child(it).removeValue() }

                // === Tambahkan navigasi ke Home saat hapus juga ===
                val intent = Intent(this, ScheduleActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }.setNegativeButton("No", null).show()
    }

    private fun showCategorySelectionDialog() {
        val cats = arrayOf("Work", "Sport", "Doctor", "Study", "Home", "Other")
        AlertDialog.Builder(this).setItems(cats) { _, w ->
            selectedCategory = cats[w]; binding.tvCategorySelect.text = selectedCategory
        }.show()
    }

    private fun showLevelSelectionDialog() {
        val levs = arrayOf("High", "Medium", "Low")
        AlertDialog.Builder(this).setItems(levs) { _, w ->
            selectedLevel = levs[w]; binding.tvLevelSelect.text = selectedLevel
        }.show()
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = String.format("%04d-%02d-%02d", y, m+1, d)
            binding.tvSetDate.text = selectedDate
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            selectedTime = String.format("%02d:%02d", h, m)
            binding.tvSetTime.text = selectedTime
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }
}