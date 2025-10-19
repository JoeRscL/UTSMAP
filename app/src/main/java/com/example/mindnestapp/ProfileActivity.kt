package com.example.mindnestapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.mindnestapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var imageUri: Uri? = null // Untuk menyimpan URI gambar yang akan diupload

    // Launcher untuk memilih gambar dari galeri
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                binding.ivProfile.setImageURI(it) // Tampilkan preview
                uploadImageToFirebase()
            }
        }
    }

    // Launcher untuk mengambil gambar dari kamera
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Gambar dari kamera sudah disimpan di `imageUri` yang kita buat sebelumnya.
            // Langsung tampilkan preview dan mulai proses upload.
            imageUri?.let {
                binding.ivProfile.setImageURI(it) // Tampilkan preview
                uploadImageToFirebase()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Jika tidak ada user yang login, kembali ke halaman login
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        // Referensi ke data user spesifik di Firebase
        database = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        loadUserProfile()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.ivProfile.setOnClickListener { showImagePickerDialog() }
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE
        // Menggunakan addValueEventListener agar jika ada perubahan data (seperti URL foto),
        // tampilan langsung diperbarui.
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Ambil data dari Firebase
                    val name = snapshot.child("nama").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    // Tampilkan data ke UI
                    binding.tvProfileName.text = name ?: "Nama tidak ditemukan"
                    binding.tvProfileEmail.text = email ?: "Email tidak ditemukan"

                    // Muat gambar profil menggunakan Glide
                    // Glide akan menangani caching (penyimpanan sementara di local storage) secara otomatis
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile) // Tampilkan ini saat gambar sedang dimuat
                            .into(binding.ivProfile)
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Data profil tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ProfileActivity, "Gagal memuat profil: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Buka Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Ganti Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // Panggil fungsi kamera
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // ============================================
    //  FUNGSI UNTUK MEMBUKA KAMERA (SUDAH BENAR)
    // ============================================
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        createImageUri()?.let { uri ->
            imageUri = uri
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            cameraLauncher.launch(intent)
        }
    }

    private fun createImageUri(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            // Dapatkan URI yang aman melalui FileProvider
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider", // Otoritas harus cocok dengan di Manifest
                file
            )
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // =============================================================
    //  FUNGSI UNTUK UPLOAD KE STORAGE & UPDATE KE REALTIME DATABASE
    // =============================================================
    private fun uploadImageToFirebase() {
        imageUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE // Tampilkan loading
            // Buat path unik untuk setiap pengguna di Firebase Storage
            val storageRef = FirebaseStorage.getInstance().getReference("profile_images/${auth.currentUser?.uid}")

            // 1. Upload file ke Firebase Storage
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // 2. Jika upload berhasil, dapatkan URL download-nya
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // 3. Panggil fungsi untuk menyimpan URL ini ke Realtime Database
                        updateProfileImageUrl(downloadUrl.toString())
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Upload gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProfileImageUrl(url: String) {
        // 4. Simpan URL ke dalam node 'profileImageUrl' di Realtime Database
        database.child("profileImageUrl").setValue(url)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memperbarui URL foto di database.", Toast.LENGTH_SHORT).show()
            }
    }
}
