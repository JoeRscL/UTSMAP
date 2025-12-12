package com.example.mindnestapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    private var imageUri: Uri? = null
    private val genderOptions = arrayOf("Select your gender", "Male", "Female", "Other")
    private lateinit var userProfileListener: ValueEventListener

    // Launcher untuk galeri
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                binding.ivProfile.setImageURI(it)
                uploadImageToFirebase()
            }
        }
    }

    // Launcher untuk kamera
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri?.let {
                binding.ivProfile.setImageURI(it)
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
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        setupSpinner()
        loadUserProfile()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hindari memory leak dengan melepaskan listener saat activity dihancurkan
        if (this::userProfileListener.isInitialized) {
            database.removeEventListener(userProfileListener)
        }
    }

    private fun setupSpinner() {
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = genderAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnUpdateProfile.setOnClickListener {
            updateUserProfileData()
        }

        binding.ivProfile.setOnClickListener { showImagePickerDialog() }

        binding.layoutDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        // Daftarkan EditText nomor telepon ke CCP di sini agar tidak berulang kali
        binding.ccp.registerCarrierNumberEditText(binding.etPhoneNumber)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.tvDateOfBirth.text = selectedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE
        userProfileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDestroyed || isFinishing) return

                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java)
                    val lastName = snapshot.child("lastName").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    val fullPhoneNumber = snapshot.child("phoneNumber").getValue(String::class.java)
                    val gender = snapshot.child("gender").getValue(String::class.java)
                    val dateOfBirth = snapshot.child("dateOfBirth").getValue(String::class.java)

                    binding.tvProfileName.text = "$firstName $lastName"
                    binding.tvProfileEmail.text = email ?: "Email tidak ditemukan"
                    binding.etFirstName.setText(firstName)
                    binding.etLastName.setText(lastName)

                    if (!profileImageUrl.isNullOrEmpty()) {
                        // PERBAIKAN: Nonaktifkan cache Glide agar gambar profil terbaru selalu dimuat
                        Glide.with(this@ProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(binding.ivProfile)
                    }

                    if (!fullPhoneNumber.isNullOrEmpty()) {
                        binding.ccp.fullNumber = fullPhoneNumber
                        binding.etPhoneNumber.setText(fullPhoneNumber.replace(binding.ccp.selectedCountryCodeWithPlus, ""))
                    }

                    if (!gender.isNullOrEmpty()) {
                        val genderPosition = genderOptions.indexOf(gender)
                        if (genderPosition >= 0) {
                            binding.spinnerGender.setSelection(genderPosition)
                        }
                    }

                    if (!dateOfBirth.isNullOrEmpty()) {
                        binding.tvDateOfBirth.text = dateOfBirth
                    }
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isDestroyed && !isFinishing) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ProfileActivity, "Gagal memuat profil: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        database.addValueEventListener(userProfileListener)
    }

    private fun updateUserProfileData() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()

        val phoneNumber = binding.ccp.fullNumberWithPlus.trim()
        val gender = if (binding.spinnerGender.selectedItemPosition > 0) {
            binding.spinnerGender.selectedItem.toString()
        } else {
            ""
        }
        val dateOfBirth = binding.tvDateOfBirth.text.toString()

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Nama depan dan belakang tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "gender" to gender,
            "dateOfBirth" to dateOfBirth
        )

        binding.progressBar.visibility = View.VISIBLE
        database.updateChildren(userUpdates).addOnCompleteListener {
            binding.progressBar.visibility = View.GONE
            if (it.isSuccessful) {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal memperbarui profil: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Buka Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Ganti Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

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
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun uploadImageToFirebase() {
        imageUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
    
            val userId = auth.currentUser?.uid
            if (userId == null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Sesi pengguna tidak valid, silakan login ulang.", Toast.LENGTH_SHORT).show()
                return
            }
    
            val storageRef = FirebaseStorage.getInstance().getReference("profile_images/$userId.jpg")
    
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // Setelah upload berhasil, dapatkan URL download
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updateProfileImageUrl(downloadUrl.toString())
                    }.addOnFailureListener {
                        // Gagal mendapatkan URL, bisa jadi karena aturan read
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Upload berhasil, tapi gagal mendapatkan URL gambar.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    // Pesan error yang lebih deskriptif untuk membantu debugging
                    Toast.makeText(this, "Upload gagal. Periksa aturan keamanan Firebase Storage Anda. Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun updateProfileImageUrl(url: String) {
        database.child("profileImageUrl").setValue(url)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memperbarui URL database.", Toast.LENGTH_SHORT).show()
            }
    }
}
