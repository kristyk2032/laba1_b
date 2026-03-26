package com.example.laba1_b

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var btnSelectPhoto: Button
    private lateinit var btnGetLocation: Button
    private lateinit var btnSaveEntry: Button
    private lateinit var ivFoodPhoto: ImageView
    private lateinit var tvGalleryStatusInline: TextView
    private lateinit var tvLocationStatusInline: TextView
    private lateinit var tvLocationInfo: TextView
    private lateinit var tvLastEntry: TextView
    private lateinit var etDishName: EditText
    private lateinit var etNote: EditText

    private var selectedImageUri: Uri? = null
    private var currentLocation: Location? = null

    private companion object {
        const val REQUEST_CODE_LOCATION = 1002
        const val REQUEST_CODE_STORAGE = 1003
        const val REQUEST_IMAGE_PICK = 2001

        private const val KEY_DISH_NAME = "dish_name"
        private const val KEY_NOTE = "note"
        private const val KEY_IMAGE_URI = "image_uri"
        private const val KEY_LOCATION_LAT = "location_lat"
        private const val KEY_LOCATION_LON = "location_lon"
        private const val KEY_LAST_ENTRY = "last_entry"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        restoreSavedData(savedInstanceState)
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_DISH_NAME, etDishName.text.toString())
        outState.putString(KEY_NOTE, etNote.text.toString())

        if (selectedImageUri != null) {
            outState.putString(KEY_IMAGE_URI, selectedImageUri.toString())
        }

        if (currentLocation != null) {
            outState.putDouble(KEY_LOCATION_LAT, currentLocation!!.latitude)
            outState.putDouble(KEY_LOCATION_LON, currentLocation!!.longitude)
        }

        outState.putString(KEY_LAST_ENTRY, tvLastEntry.text.toString())
    }

    private fun restoreSavedData(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        etDishName.setText(savedInstanceState.getString(KEY_DISH_NAME, ""))
        etNote.setText(savedInstanceState.getString(KEY_NOTE, ""))

        val imageUriString = savedInstanceState.getString(KEY_IMAGE_URI)
        if (imageUriString != null) {
            selectedImageUri = Uri.parse(imageUriString)
            ivFoodPhoto.setImageURI(selectedImageUri)
        }

        if (savedInstanceState.containsKey(KEY_LOCATION_LAT)) {
            val lat = savedInstanceState.getDouble(KEY_LOCATION_LAT)
            val lon = savedInstanceState.getDouble(KEY_LOCATION_LON)
            currentLocation = Location("").apply {
                latitude = lat
                longitude = lon
            }
            tvLocationInfo.text = "$lat, $lon\nг. Санкт-Петербург, Дворцовая площадь"
        }

        val lastEntry = savedInstanceState.getString(KEY_LAST_ENTRY)
        if (!lastEntry.isNullOrEmpty() && lastEntry != "Нет сохраненных записей") {
            tvLastEntry.text = lastEntry
        }
    }

    private fun initViews() {
        btnSelectPhoto = findViewById(R.id.btn_select_photo)
        btnGetLocation = findViewById(R.id.btn_get_location)
        btnSaveEntry = findViewById(R.id.btn_save_entry)
        ivFoodPhoto = findViewById(R.id.iv_food_photo)
        tvGalleryStatusInline = findViewById(R.id.tv_gallery_status_inline)
        tvLocationStatusInline = findViewById(R.id.tv_location_status_inline)
        tvLocationInfo = findViewById(R.id.tv_location_info)
        tvLastEntry = findViewById(R.id.tv_last_entry)
        etDishName = findViewById(R.id.et_dish_name)
        etNote = findViewById(R.id.et_note)
    }

    private fun setupClickListeners() {
        btnSelectPhoto.setOnClickListener {
            if (checkStoragePermission()) {
                openGallery()
            } else {
                requestStoragePermission()
            }
        }

        btnGetLocation.setOnClickListener {
            if (checkLocationPermission()) {
                getLocation()
            } else {
                requestLocationPermission()
            }
        }

        btnSaveEntry.setOnClickListener {
            saveFoodEntry()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_STORAGE)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ), REQUEST_CODE_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                    Toast.makeText(this, "Доступ к галерее получен", Toast.LENGTH_SHORT).show()
                } else {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    if (!shouldShowRequestPermissionRationale(permission)) {
                        showSettingsDialog("галерее")
                    } else {
                        Toast.makeText(this, "Нет доступа к галерее", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            REQUEST_CODE_LOCATION -> {
                val hasLocationPermission = grantResults.isNotEmpty() &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                                (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))
                if (hasLocationPermission) {
                    getLocation()
                    Toast.makeText(this, "Доступ к геолокации получен", Toast.LENGTH_SHORT).show()
                } else {
                    if (!shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showSettingsDialog("геолокации")
                    } else {
                        Toast.makeText(this, "Нет доступа к геолокации", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        updateUI()
    }

    private fun showSettingsDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("Разрешение заблокировано")
            .setMessage("Доступ к $permissionName был отклонен. Вы можете включить его в настройках приложения.")
            .setPositiveButton("Открыть настройки") { _, _ ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    startActivity(this)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let {
                ivFoodPhoto.setImageURI(it)
                Toast.makeText(this, "Фото блюда добавлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocation() {
        Toast.makeText(this, "Определяем местоположение...", Toast.LENGTH_SHORT).show()
        currentLocation = Location("").apply {
            latitude = 59.939095
            longitude = 30.315868
        }

        val locationText = """
            ${currentLocation?.latitude}, ${currentLocation?.longitude}
            г. Санкт-Петербург, Дворцовая площадь
        """.trimIndent()

        tvLocationInfo.text = locationText
        Toast.makeText(this, "Местоположение определено", Toast.LENGTH_SHORT).show()
    }

    private fun saveFoodEntry() {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val dishName = etDishName.text.toString().trim()
        val note = etNote.text.toString().trim()

        if (selectedImageUri == null && dishName.isEmpty()) {
            AlertDialog.Builder(this)
                .setMessage("Добавьте название блюда или фото")
                .setPositiveButton("ОК", null)
                .show()
            return
        }

        val entry = buildString {
            append("${timestamp}\n")
            if (dishName.isNotEmpty()) {
                append("$dishName\n")
            }
            append(if (selectedImageUri != null) "Есть фото\n" else "Без фото\n")
            if (currentLocation != null) {
                append("${currentLocation?.latitude}, ${currentLocation?.longitude}\n")
                append("г. Санкт-Петербург, Дворцовая площадь\n")
            } else {
                append("Место не указано\n")
            }
            if (note.isNotEmpty()) {
                append("\"$note\"\n")
            }
            append("---\n")
        }

        tvLastEntry.text = entry

        AlertDialog.Builder(this)
            .setTitle("Сохранено")
            .setMessage("Запись добавлена")
            .setPositiveButton("ОК") { _, _ ->
                etDishName.text.clear()
            }
            .show()

        clearAll()
    }

    private fun clearAll() {
        etDishName.text.clear()

        selectedImageUri = null
        ivFoodPhoto.setImageResource(R.drawable.food_placeholder)

        currentLocation = null
        tvLocationInfo.text = "Место не определено"

        etNote.text.clear()
    }

    private fun updateUI() {
        val galleryHasPermission = checkStoragePermission()
        val locationHasPermission = checkLocationPermission()

        tvGalleryStatusInline.text = if (galleryHasPermission) "(✅ есть)" else "(❌ нет)"
        tvGalleryStatusInline.setTextColor(
            if (galleryHasPermission)
                resources.getColor(android.R.color.holo_green_dark, null)
            else
                resources.getColor(android.R.color.holo_red_dark, null)
        )

        tvLocationStatusInline.text = if (locationHasPermission) "(✅ есть)" else "(❌ нет)"
        tvLocationStatusInline.setTextColor(
            if (locationHasPermission)
                resources.getColor(android.R.color.holo_green_dark, null)
            else
                resources.getColor(android.R.color.holo_red_dark, null)
        )
    }
}