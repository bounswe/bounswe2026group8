package com.bounswe2026group8.emergencyhub.offline.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bounswe2026group8.emergencyhub.R

class FirstAidDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_aid_detail)

        // 1. Find the views
        val card1 = findViewById<CardView>(R.id.card_image_1)
        val imgView1 = findViewById<ImageView>(R.id.detail_image_1)

        val card2 = findViewById<CardView>(R.id.card_image_2)
        val imgView2 = findViewById<ImageView>(R.id.detail_image_2)

        val titleView = findViewById<TextView>(R.id.detail_title)
        val textView = findViewById<TextView>(R.id.detail_text)

        // 2. Get the data passed from the previous screen
        val passedTitle = intent.getStringExtra("EXTRA_TITLE")
        val passedText = intent.getStringExtra("EXTRA_TEXT")

        // Use DIFFERENT keys for each image
        val passedImage1 = intent.getIntExtra("EXTRA_IMAGE_ID_1", 0)
        val passedImage2 = intent.getIntExtra("EXTRA_IMAGE_ID_2", 0)

        // 3. Set the text
        titleView.text = passedTitle
        textView.text = passedText

        // 4. Handle Image 1
        if (passedImage1 != 0) {
            imgView1.setImageResource(passedImage1)
            card1.visibility = View.VISIBLE
        } else {
            card1.visibility = View.GONE // Hide completely if no image is sent
        }

        // 5. Handle Image 2
        if (passedImage2 != 0) {
            imgView2.setImageResource(passedImage2)
            card2.visibility = View.VISIBLE
        } else {
            card2.visibility = View.GONE // Hide completely if no image is sent
        }
    }
}