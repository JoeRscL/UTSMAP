package com.example.mindnestapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

// --- 1. MODEL DATA (Disatukan disini agar tidak error) ---
// Sesuaikan nama field ini dengan database Firebase Anda
data class Task(
    var id: String? = null,
    var title: String? = null,
    var date: String? = null, // Format: yyyy-MM-dd
    var time: String? = null, // Format: HH:mm
    var isCompleted: Boolean = false
)

// --- 2. ADAPTER ---
class TaskAdapter(
    private val taskList: ArrayList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        // Pastikan di item_task.xml, CardView terluar ID-nya adalah @+id/cardContainer
        val cardContainer: CardView = itemView.findViewById(R.id.cardContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        val context = holder.itemView.context

        // Set Data Text
        holder.tvTitle.text = task.title
        holder.tvDate.text = task.date
        holder.tvTime.text = task.time

        // --- LOGIKA TAMPILAN (WARNA & CORET) ---

        // A. Reset tampilan ke default (Wajib untuk RecyclerView)
        holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() // Hapus coretan
        holder.tvTitle.setTextColor(Color.BLACK)
        holder.tvTime.setTextColor(Color.parseColor("#007AFF")) // Biru Default
        holder.cardContainer.setCardBackgroundColor(Color.WHITE) // Putih Default

        // B. Cek Status
        if (task.isCompleted) {
            // KONDISI: SELESAI
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Coret
            holder.tvTitle.setTextColor(Color.GRAY)
            holder.tvTime.setTextColor(Color.GRAY)
            holder.cardContainer.setCardBackgroundColor(Color.parseColor("#F2F2F7")) // Abu-abu pudar
        } else {
            // KONDISI: BELUM SELESAI (Cek Telat)
            val dateString = "${task.date} ${task.time}"
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            try {
                val dateJadwal = format.parse(dateString)
                val waktuSekarang = System.currentTimeMillis()

                if (dateJadwal != null && dateJadwal.time < waktuSekarang) {
                    // TELAT
                    holder.tvTime.setTextColor(Color.RED)
                    holder.tvTime.text = "${task.time} (Terlewat)"
                    holder.cardContainer.setCardBackgroundColor(Color.parseColor("#FFF0F0")) // Merah pudar
                }
            } catch (e: Exception) {
                // Ignore error parsing
            }
        }

        // --- KLIK UNTUK EDIT ---
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AddTaskActivity::class.java)
            intent.putExtra("isEditMode", true)
            intent.putExtra("taskId", task.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }
}