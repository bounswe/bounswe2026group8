package com.bounswe2026group8.emergencyhub.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreatePostRequest
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.bounswe2026group8.emergencyhub.util.ErrorUtils
import com.bounswe2026group8.emergencyhub.util.ImageUploadHelper
import com.bounswe2026group8.emergencyhub.util.VoiceInputManager

class CreatePostActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var forumTypeToggle: MaterialButtonToggleGroup
    private lateinit var inputTitle: TextInputEditText
    private lateinit var inputContent: TextInputEditText
    private lateinit var btnCreate: MaterialButton
    private lateinit var btnUploadImages: MaterialButton
    private lateinit var txtError: TextView
    private lateinit var txtUploadStatus: TextView
    private lateinit var txtPageTitle: TextView
    private lateinit var txtPageSubtitle: TextView
    private lateinit var imagePreviewScroll: HorizontalScrollView
    private lateinit var imagePreviewContainer: LinearLayout
    private lateinit var inputImageUrls: TextInputEditText
    private lateinit var voiceInputManager: VoiceInputManager

    private var selectedForumType = "GLOBAL"
    private val uploadedImageUrls = mutableListOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            val singleUri = result.data?.data
            val uris = mutableListOf<Uri>()

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } else if (singleUri != null) {
                uris.add(singleUri)
            }

            if (uris.isNotEmpty()) uploadImages(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        tokenManager = TokenManager(this)
        voiceInputManager = VoiceInputManager(this)

        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in to create posts", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        forumTypeToggle = findViewById(R.id.forumTypeToggle)
        inputTitle = findViewById(R.id.inputTitle)
        inputContent = findViewById(R.id.inputContent)
        btnCreate = findViewById(R.id.btnCreate)
        btnUploadImages = findViewById(R.id.btnUploadImages)
        txtError = findViewById(R.id.txtError)
        txtUploadStatus = findViewById(R.id.txtUploadStatus)
        txtPageTitle = findViewById(R.id.txtPageTitle)
        txtPageSubtitle = findViewById(R.id.txtPageSubtitle)
        imagePreviewScroll = findViewById(R.id.imagePreviewScroll)
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer)
        inputImageUrls = findViewById(R.id.inputImageUrls)
        voiceInputManager.bind(inputTitle, inputContent)

        selectedForumType = intent.getStringExtra("forum_type") ?: "GLOBAL"
        setInitialToggle()

        forumTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedForumType = when (checkedId) {
                    R.id.btnTypeStandard -> "STANDARD"
                    R.id.btnTypeUrgent -> "URGENT"
                    else -> "GLOBAL"
                }
                updateSubtitle()
            }
        }

        btnCreate.setOnClickListener { submitPost() }
        btnUploadImages.setOnClickListener { openImagePicker() }

        HubSelectorHelper(this, findViewById<TextView>(R.id.textHubDisplay)).load()

        findViewById<TextView>(R.id.linkBackToForum).setOnClickListener { finish() }

        updateSubtitle()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"))
    }

    private fun uploadImages(uris: List<Uri>) {
        btnUploadImages.isEnabled = false
        txtUploadStatus.text = "Uploading ${uris.size} image(s)…"
        txtUploadStatus.visibility = View.VISIBLE

        val parts = ImageUploadHelper.prepareParts(contentResolver, uris)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreatePostActivity)
                    .uploadImages(parts)

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
            }
        }
    }

    private fun refreshImagePreviews() {
        imagePreviewContainer.removeAllViews()
        if (uploadedImageUrls.isEmpty()) {
            imagePreviewScroll.visibility = View.GONE
            txtUploadStatus.visibility = View.GONE
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
            }

            imagePreviewContainer.addView(itemView)
        }
    }

    private fun setInitialToggle() {
        val checkedId = when (selectedForumType) {
            "STANDARD" -> R.id.btnTypeStandard
            "URGENT" -> R.id.btnTypeUrgent
            else -> R.id.btnTypeGlobal
        }
        forumTypeToggle.check(checkedId)
    }

    private fun updateSubtitle() {
        val typeLabel = when (selectedForumType) {
            "STANDARD" -> getString(R.string.forum_type_standard)
            "URGENT" -> getString(R.string.forum_type_urgent)
            else -> getString(R.string.forum_type_global)
        }
        txtPageTitle.text = getString(R.string.new_post_format, typeLabel)

        txtPageSubtitle.text = if (selectedForumType == "GLOBAL") {
            getString(R.string.create_post_subtitle_global)
        } else {
            getString(R.string.hub_scoped_post)
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = View.VISIBLE
    }

    private fun clearError() {
        txtError.visibility = View.GONE
    }

    private fun submitPost() {
        clearError()

        val title = inputTitle.text.toString().trim()
        val content = inputContent.text.toString().trim()

        if (title.isEmpty()) {
            showError(getString(R.string.title_required))
            return
        }
        if (content.isEmpty()) {
            showError(getString(R.string.content_required))
            return
        }

        btnCreate.isEnabled = false
        btnCreate.text = getString(R.string.creating_post)

        val hubId = if (selectedForumType != "GLOBAL") {
            tokenManager.getUser()?.hub?.id
        } else null

        val pastedUrls = inputImageUrls.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val allImageUrls = uploadedImageUrls + pastedUrls

        val request = CreatePostRequest(
            forumType = selectedForumType,
            title = title,
            content = content,
            imageUrls = allImageUrls,
            hub = hubId
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@CreatePostActivity)
                    .createPost(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreatePostActivity,
                        "Post created!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to create post."
                    showError(ErrorUtils.parseApiError(errorBody, "Failed to create post."))
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            } finally {
                btnCreate.isEnabled = true
                btnCreate.text = getString(R.string.create_post)
            }
        }
    }

}
