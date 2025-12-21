package com.example.mindnestapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.mindnestapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
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

    // Launcher Galeri
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                imageUri = it
                binding.ivProfile.setImageURI(it)
                uploadImageAsBase64()
            }
        }
    }

    // Launcher Kamera
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri?.let {
                binding.ivProfile.setImageURI(it)
                uploadImageAsBase64()
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
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        setupSpinner()

        // 1. Jalankan deteksi negara (Prioritas Indonesia untuk Emulator)
        autoDetectCountry()

        // 2. Load data user (Data di DB akan menimpa deteksi jika sudah ada nomornya)
        loadUserProfile()

        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::userProfileListener.isInitialized) {
            database.removeEventListener(userProfileListener)
        }
    }

    private fun setupSpinner() {
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = genderAdapter
    }

    // --- MODIFIKASI KHUSUS AGAR TERDETEKSI INDONESIA ---
    private fun autoDetectCountry() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var countryIso = telephonyManager.networkCountryIso

            if (countryIso.isNullOrEmpty()) {
                countryIso = Locale.getDefault().country
            }

            // [FIX] Trik Khusus Emulator:
            // Karena Emulator defaultnya "us", kita paksa ubah ke "id" jika terdeteksi "us".
            // Hapus blok 'if' ini jika nanti sudah rilis ke PlayStore/HP Asli.
            if (countryIso.equals("us", ignoreCase = true)) {
                countryIso = "id"
            }

            if (!countryIso.isNullOrEmpty()) {
                binding.ccp.setDefaultCountryUsingNameCode(countryIso)
                binding.ccp.resetToDefaultCountry() // Reset agar tampilan berubah
            } else {
                // Fallback jika gagal deteksi sama sekali
                binding.ccp.setDefaultCountryUsingNameCode("ID")
                binding.ccp.resetToDefaultCountry()
            }
        } catch (e: Exception) {
            // Jika error, default ke Indonesia
            binding.ccp.setDefaultCountryUsingNameCode("ID")
            binding.ccp.resetToDefaultCountry()
        }

        binding.ccp.registerCarrierNumberEditText(binding.etPhoneNumber)
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE

        userProfileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDestroyed || isFinishing) return

                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""

                    binding.tvProfileName.text = "$firstName $lastName"
                    binding.tvProfileEmail.text = snapshot.child("email").getValue(String::class.java)
                    binding.etFirstName.setText(firstName)
                    binding.etLastName.setText(lastName)

                    // Load Image Base64
                    val base64Image = snapshot.child("profileImageBase64").getValue(String::class.java)
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binding.ivProfile.setImageBitmap(decodedImage)
                        } catch (e: Exception) {
                            binding.ivProfile.setImageResource(R.drawable.ic_default_profile)
                        }
                    } else {
                        binding.ivProfile.setImageResource(R.drawable.ic_default_profile)
                    }

                    // [PENTING] Load Nomor HP
                    val fullPhoneNumber = snapshot.child("phoneNumber").getValue(String::class.java)

                    if (!fullPhoneNumber.isNullOrEmpty()) {
                        // Jika di database sudah ada nomor (misal +1...), dia akan MENIMPA deteksi otomatis.
                        // CCP akan menyesuaikan bendera dengan nomor yang tersimpan.
                        binding.ccp.fullNumber = fullPhoneNumber
                    }
                    // Jika database kosong, dia akan tetap menggunakan hasil autoDetectCountry() (Indonesia)

                    val gender = snapshot.child("gender").getValue(String::class.java)
                    if (!gender.isNullOrEmpty()) {
                        val genderPosition = genderOptions.indexOf(gender)
                        if (genderPosition >= 0) binding.spinnerGender.setSelection(genderPosition)
                    }

                    val dateOfBirth = snapshot.child("dateOfBirth").getValue(String::class.java)
                    if (!dateOfBirth.isNullOrEmpty()) binding.tvDateOfBirth.text = dateOfBirth
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isDestroyed && !isFinishing) binding.progressBar.visibility = View.GONE
            }
        }
        database.addValueEventListener(userProfileListener)
    }

    // ... (Sisa fungsi sama: uploadImage, updateProfile, dll) ...

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUpdateProfile.setOnClickListener { updateUserProfileData() }
        binding.ivProfile.setOnClickListener { showImagePickerDialog() }
        binding.layoutDateOfBirth.setOnClickListener { showDatePickerDialog() }
        binding.btnLogout.setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun uploadImageAsBase64() {
        val uri = imageUri ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdateProfile.isEnabled = false

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            database.child("profileImageBase64").setValue(base64Image)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUpdateProfile.isEnabled = true
                    Toast.makeText(this, "Foto berhasil diupdate!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnUpdateProfile.isEnabled = true
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnUpdateProfile.isEnabled = true
        }
    }

    private fun updateUserProfileData() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val phoneNumber = binding.ccp.fullNumberWithPlus // Ambil nomor lengkap dengan kode negara
        val gender = if (binding.spinnerGender.selectedItemPosition > 0) binding.spinnerGender.selectedItem.toString() else ""
        val dateOfBirth = binding.tvDateOfBirth.text.toString()

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            binding.tvDateOfBirth.text = String.format("%04d-%02d-%02d", y, m + 1, d)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(this).setTitle("Ganti Foto").setItems(arrayOf("Kamera", "Galeri")) { _, w ->
            if (w == 0) openCamera() else openGallery()
        }.show()
    }

    private fun openGallery() {
        galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
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
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        } catch (e: IOException) { null }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this).setTitle("Logout").setMessage("Yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                auth.signOut()
                val intent = Intent(this, login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.setNegativeButton("Batal", null).show()
    }
}