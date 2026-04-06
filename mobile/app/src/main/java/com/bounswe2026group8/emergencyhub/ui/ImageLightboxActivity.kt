package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bumptech.glide.Glide

class ImageLightboxActivity : AppCompatActivity() {

    private lateinit var imgLightbox: ImageView
    private lateinit var btnPrev: TextView
    private lateinit var btnNext: TextView
    private lateinit var txtCounter: TextView

    private var imageUrls: List<String> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_lightbox)

        imgLightbox = findViewById(R.id.imgLightbox)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtCounter = findViewById(R.id.txtCounter)

        imageUrls = intent.getStringArrayListExtra("image_urls") ?: emptyList()
        currentIndex = intent.getIntExtra("start_index", 0)

        if (imageUrls.isEmpty()) { finish(); return }

        if (imageUrls.size > 1) {
            btnPrev.visibility = View.VISIBLE
            btnNext.visibility = View.VISIBLE
            txtCounter.visibility = View.VISIBLE
        }

        btnPrev.setOnClickListener {
            currentIndex = (currentIndex - 1 + imageUrls.size) % imageUrls.size
            displayImage()
        }

        btnNext.setOnClickListener {
            currentIndex = (currentIndex + 1) % imageUrls.size
            displayImage()
        }

        findViewById<TextView>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<View>(android.R.id.content).setOnClickListener { finish() }
        imgLightbox.setOnClickListener { /* prevent closing when tapping image */ }

        displayImage()
    }

    private fun displayImage() {
        Glide.with(this)
            .load(RetrofitClient.resolveImageUrl(imageUrls[currentIndex]))
            .into(imgLightbox)

        if (imageUrls.size > 1) {
            txtCounter.text = "${currentIndex + 1} / ${imageUrls.size}"
        }
    }
}
