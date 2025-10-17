package com.example.mindnestapp

import android.app.Activity
import android.widget.LinearLayout
import android.widget.Toast

class FooterNavHelper(private val activity: Activity) {

    fun setupFooterNavigation() {
        val navHome = activity.findViewById<LinearLayout>(R.id.navHome)
        val navCalendar = activity.findViewById<LinearLayout>(R.id.navCalendar)
        val navAdd = activity.findViewById<LinearLayout>(R.id.navAdd)
        val navFile = activity.findViewById<LinearLayout>(R.id.navFile)
        val navSettings = activity.findViewById<LinearLayout>(R.id.navSettings)

        navHome.setOnClickListener { Toast.makeText(activity, "Home clicked", Toast.LENGTH_SHORT).show() }
        navCalendar.setOnClickListener { Toast.makeText(activity, "Calendar clicked", Toast.LENGTH_SHORT).show() }
        navAdd.setOnClickListener { Toast.makeText(activity, "Add clicked", Toast.LENGTH_SHORT).show() }
        navFile.setOnClickListener { Toast.makeText(activity, "Files clicked", Toast.LENGTH_SHORT).show() }
        navSettings.setOnClickListener { Toast.makeText(activity, "Settings clicked", Toast.LENGTH_SHORT).show() }
    }
}
