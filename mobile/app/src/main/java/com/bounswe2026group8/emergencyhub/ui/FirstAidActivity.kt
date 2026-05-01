package com.bounswe2026group8.emergencyhub.offline.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bounswe2026group8.emergencyhub.R

class FirstAidActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_aid)

        findViewById<CardView>(R.id.card_displacement).setOnClickListener {
            startDetail(
                getString(R.string.first_aid_displacement_title),
                getString(R.string.first_aid_displacement_detail),
                R.drawable.rautek_maneuver,
                R.drawable.blanket_pull
            )
        }

        findViewById<CardView>(R.id.card_checking).setOnClickListener {
            startDetail(
                getString(R.string.first_aid_checking_title),
                getString(R.string.first_aid_checking_detail),
                R.drawable.carotidian_pulse,
                R.drawable.checking_respiration
            )
        }

        findViewById<CardView>(R.id.card_cpr).setOnClickListener {
            startDetail(
                getString(R.string.first_aid_cpr_title),
                getString(R.string.first_aid_cpr_detail)
            )
        }

        findViewById<CardView>(R.id.card_abc).setOnClickListener {
            startDetail(
                getString(R.string.first_aid_abc_title),
                getString(R.string.first_aid_abc_detail)
            )
        }
    }

    private fun startDetail(title: String, text: String, image1: Int = 0, image2: Int = 0) {
        val intent = Intent(this, FirstAidDetailActivity::class.java)
        intent.putExtra("EXTRA_TITLE", title)
        intent.putExtra("EXTRA_TEXT", text)
        intent.putExtra("EXTRA_IMAGE_ID_1", image1)
        intent.putExtra("EXTRA_IMAGE_ID_2", image2)
        startActivity(intent)
    }
}
