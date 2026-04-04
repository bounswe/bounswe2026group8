package com.bounswe2026group8.emergencyhub.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateHelpRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Form screen for creating a new help request.
 *
 * Collects title, description, category, urgency, optional location
 * (text and/or GPS coordinates), and optional images. Images are uploaded
 * to POST /help-requests/upload/ before the form is submitted; the returned
 * URLs are attached to the request payload.
 */
class CreateHelpRequestActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var inputTitle: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var inputLocationText: TextInputEditText
    private lateinit var dropdownCategory: AutoCompleteTextView
    private lateinit var dropdownUrgency: AutoCompleteTextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnUseLocation: MaterialButton
    private lateinit var txtLocationStatus: TextView

    // Image upload views
    private lateinit var btnUploadImages: MaterialButton
    private lateinit var btnCaptureImage: MaterialButton
    private lateinit var txtUploadStatus: TextView
    private lateinit var imagePreviewScroll: HorizontalScrollView
    private lateinit var imagePreviewContainer: LinearLayout

    /** GPS coordinates captured via "Use My Location". */
    private var capturedLat: Double? = null
    private var capturedLng: Double? = null

    /** URLs returned by the upload endpoint, to be sent with the create request. */
    private val uploadedImageUrls = mutableListOf<String>()

    /** URI where the camera photo will be saved (needed to retrieve it after capture). */
    private var cameraImageUri: Uri? = null

    /** Launcher for the location permission request. */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchCurrentLocation()
        else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    /** Launcher for gallery image picker (multi-select). */
    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            val singleUri = result.data?.data
            val uris = mutableListOf<Uri>()
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) uris.add(clipData.getItemAt(i).uri)
            } else if (singleUri != null) {
                uris.add(singleUri)
            }
            if (uris.isNotEmpty()) uploadImages(uris)
        }
    }

    /** Launcher for camera capture. */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uploadImages(listOf(it)) }
        }
    }

    /** Launcher for camera permission request. */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    /** Launcher for storage permission (Android ≤ 12). */
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchGallery()
        else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_help_request)

        tokenManager = TokenManager(this)

        // Auth guard
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        // Bind views
        inputTitle = findViewById(R.id.inputTitle)
        inputDescription = findViewById(R.id.inputDescription)
        inputLocationText = findViewById(R.id.inputLocationText)
        dropdownCategory = findViewById(R.id.dropdownCategory)
        dropdownUrgency = findViewById(R.id.dropdownUrgency)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnUseLocation = findViewById(R.id.btnUseLocation)
        txtLocationStatus = findViewById(R.id.txtLocationStatus)
        btnUploadImages = findViewById(R.id.btnUploadImages)
        btnCaptureImage = findViewById(R.id.btnCaptureImage)
        txtUploadStatus = findViewById(R.id.txtUploadStatus)
        imagePreviewScroll = findViewById(R.id.imagePreviewScroll)
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer)

        // Back button
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        // Category dropdown
        val categoryAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line,
            arrayOf("Medical", "Food", "Shelter", "Transport")
        )
        dropdownCategory.setAdapter(categoryAdapter)

        // Urgency dropdown
        val urgencyAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line,
            arrayOf("Low", "Medium", "High")
        )
        dropdownUrgency.setAdapter(urgencyAdapter)

        // GPS location
        btnUseLocation.setOnClickListener { requestLocation() }

        // Image buttons
        btnUploadImages.setOnClickListener { requestGallery() }
        btnCaptureImage.setOnClickListener { requestCamera() }

        // Submit
        btnSubmit.setOnClickListener { attemptSubmit() }
    }

    // ── Location ─────────────────────────────────────────────────────────

    /** Checks permission and then fetches the device's current location. */
    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Retrieves the current GPS coordinates using the Fused Location Provider.
     * Requires ACCESS_FINE_LOCATION to already be granted.
     */
    @Suppress("MissingPermission")
    private fun fetchCurrentLocation() {
        btnUseLocation.isEnabled = false
        btnUseLocation.text = "Locating…"

        val client = LocationServices.getFusedLocationProviderClient(this)
        val cancellation = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location ->
                btnUseLocation.isEnabled = true
                btnUseLocation.text = getString(R.string.use_my_location)

                if (location != null) {
                    capturedLat = location.latitude
                    capturedLng = location.longitude
                    txtLocationStatus.text = getString(R.string.location_acquired)
                    txtLocationStatus.visibility = TextView.VISIBLE
                } else {
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                btnUseLocation.isEnabled = true
                btnUseLocation.text = getString(R.string.use_my_location)
                Toast.makeText(this, "Location error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Image upload ─────────────────────────────────────────────────────

    /** Requests storage permission (≤ API 32) or goes straight to the picker (API 33+). */
    private fun requestGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // READ_MEDIA_IMAGES granted at install on Android 13+; no runtime prompt needed
            launchGallery()
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                launchGallery()
            } else {
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryPickerLauncher.launch(Intent.createChooser(intent, "Select Images"))
    }

    /** Requests CAMERA permission and then launches the camera. */
    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        // Create a MediaStore entry so the photo lands in the shared Pictures folder.
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EmergencyHub")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show()
            return
        }
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    /**
     * Uploads the given URIs to POST /help-requests/upload/ and appends the
     * returned URLs to [uploadedImageUrls].
     */
    private fun uploadImages(uris: List<Uri>) {
        btnUploadImages.isEnabled = false
        btnCaptureImage.isEnabled = false
        txtUploadStatus.text = "Uploading ${uris.size} image(s)…"
        txtUploadStatus.visibility = View.VISIBLE

        val parts = mutableListOf<MultipartBody.Part>()
        for (uri in uris) {
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: continue
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = getFileName(uri) ?: "image.jpg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())
            parts.add(MultipartBody.Part.createFormData("images", fileName, requestBody))
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreateHelpRequestActivity)
                    .uploadHelpRequestImages(parts)

                if (response.isSuccessful) {
                    val urls = response.body()?.urls ?: emptyList()
                    uploadedImageUrls.addAll(urls)
                    refreshImagePreviews()
                    txtUploadStatus.text = "${uploadedImageUrls.size} image(s) attached"
                } else {
                    txtUploadStatus.text = "Upload failed"
                }
            } catch (e: Exception) {
                txtUploadStatus.text = "Upload error: ${e.message}"
            } finally {
                btnUploadImages.isEnabled = true
                btnCaptureImage.isEnabled = true
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name
    }

    /** Rebuilds the horizontal image preview strip from [uploadedImageUrls]. */
    private fun refreshImagePreviews() {
        imagePreviewContainer.removeAllViews()
        if (uploadedImageUrls.isEmpty()) {
            imagePreviewScroll.visibility = View.GONE
            return
        }
        imagePreviewScroll.visibility = View.VISIBLE

        for ((index, url) in uploadedImageUrls.withIndex()) {
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_image_preview, imagePreviewContainer, false)

            Glide.with(this)
                .load(RetrofitClient.resolveImageUrl(url))
                .centerCrop()
                .into(itemView.findViewById<ImageView>(R.id.imgPreview))

            itemView.findViewById<TextView>(R.id.btnRemoveImage).setOnClickListener {
                uploadedImageUrls.removeAt(index)
                refreshImagePreviews()
                if (uploadedImageUrls.isEmpty()) {
                    txtUploadStatus.visibility = View.GONE
                } else {
                    txtUploadStatus.text = "${uploadedImageUrls.size} image(s) attached"
                }
            }

            imagePreviewContainer.addView(itemView)
        }
    }

    // ── Form submission ──────────────────────────────────────────────────

    private fun attemptSubmit() {
        val title = inputTitle.text.toString().trim()
        val description = inputDescription.text.toString().trim()
        val locationText = inputLocationText.text.toString().trim().ifEmpty { null }

        // Client-side validation
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (dropdownCategory.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }
        if (dropdownUrgency.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select an urgency level.", Toast.LENGTH_SHORT).show()
            return
        }

        val category = when (dropdownCategory.text.toString()) {
            "Medical"   -> "MEDICAL"
            "Food"      -> "FOOD"
            "Shelter"   -> "SHELTER"
            "Transport" -> "TRANSPORT"
            else        -> "MEDICAL"
        }

        val urgency = when (dropdownUrgency.text.toString()) {
            "Low"    -> "LOW"
            "Medium" -> "MEDIUM"
            "High"   -> "HIGH"
            else     -> "LOW"
        }

        // Disable button while submitting
        btnSubmit.isEnabled = false
        btnSubmit.text = getString(R.string.submitting)

        val request = CreateHelpRequest(
            category = category,
            urgency = urgency,
            title = title,
            description = description,
            imageUrls = uploadedImageUrls.toList(),
            latitude = capturedLat?.let { "%.6f".format(it) },
            longitude = capturedLng?.let { "%.6f".format(it) },
            locationText = locationText
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreateHelpRequestActivity)
                    .createHelpRequest(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreateHelpRequestActivity,
                        getString(R.string.help_request_created),
                        Toast.LENGTH_SHORT
                    ).show()
                    val detail = response.body()
                    if (detail != null) {
                        val detailIntent = Intent(
                            this@CreateHelpRequestActivity,
                            HelpRequestDetailActivity::class.java
                        )
                        detailIntent.putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, detail.id)
                        startActivity(detailIntent)
                    }
                    finish()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    val errorText = response.errorBody()?.string() ?: "Submission failed."
                    Toast.makeText(
                        this@CreateHelpRequestActivity,
                        errorText,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateHelpRequestActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = getString(R.string.submit)
            }
        }
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
