package com.example.mindnestapp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddTaskActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navAdd: LinearLayout
    private lateinit var navFile: LinearLayout
    private lateinit var navSettings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task) // pastikan layout include footer

        // Bind view footer sesuai ID XML
        navHome = findViewById(R.id.navHome)
        navCalendar = findViewById(R.id.navCalendar)
        navAdd = findViewById(R.id.navAdd)
        navFile = findViewById(R.id.navFile)
        navSettings = findViewById(R.id.navSettings)

        // Click listeners
        navHome.setOnClickListener { Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show() }
        navCalendar.setOnClickListener { Toast.makeText(this, "Calendar clicked", Toast.LENGTH_SHORT).show() }
        navAdd.setOnClickListener { Toast.makeText(this, "Add clicked", Toast.LENGTH_SHORT).show() }
        navFile.setOnClickListener { Toast.makeText(this, "Files clicked", Toast.LENGTH_SHORT).show() }
        navSettings.setOnClickListener { Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show() }
    }
}