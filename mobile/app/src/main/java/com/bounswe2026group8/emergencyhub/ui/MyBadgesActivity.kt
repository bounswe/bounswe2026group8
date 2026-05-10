package com.bounswe2026group8.emergencyhub.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UserBadgeItem
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * Displays the current user's badge progress.
 * Mirrors the web MyBadgesPage behavior:
 * - Calls GET /api/badges/my-badges/
 * - For each badge shows icon, name + level, description, progress bar, progress text
 */
class MyBadgesActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var progressBadges: ProgressBar
    private lateinit var txtNoBadges: TextView
    private lateinit var txtBadgesError: TextView
    private lateinit var badgesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_badges)

        tokenManager = TokenManager(this)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        progressBadges = findViewById(R.id.progressBadges)
        txtNoBadges = findViewById(R.id.txtNoBadges)
        txtBadgesError = findViewById(R.id.txtBadgesError)
        badgesList = findViewById(R.id.badgesList)

        loadBadges()
    }

    private fun loadBadges() {
        progressBadges.visibility = View.VISIBLE
        txtNoBadges.visibility = View.GONE
        txtBadgesError.visibility = View.GONE
        badgesList.removeAllViews()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@MyBadgesActivity)
                    .getMyBadges()

                if (response.isSuccessful) {
                    val badges = response.body() ?: emptyList()
                    if (badges.isEmpty()) {
                        txtNoBadges.visibility = View.VISIBLE
                    } else {
                        displayBadges(badges)
                    }
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    finish()
                } else {
                    txtBadgesError.text = getString(R.string.badges_load_failed)
                    txtBadgesError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                txtBadgesError.text = getString(R.string.network_error_with_message, e.localizedMessage ?: "")
                txtBadgesError.visibility = View.VISIBLE
            } finally {
                progressBadges.visibility = View.GONE
            }
        }
    }

    private fun displayBadges(badges: List<UserBadgeItem>) {
        val density = resources.displayMetrics.density

        for (badge in badges) {
            // Card container
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (12 * density).toInt() }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_surface))
                radius = 14 * density
                strokeColor = ContextCompat.getColor(context, R.color.border)
                strokeWidth = (1 * density).toInt()
                cardElevation = 0f
            }

            val cardContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (16 * density).toInt(), (16 * density).toInt(),
                    (16 * density).toInt(), (16 * density).toInt()
                )
            }

            // Top row: icon + name/description
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Badge icon
            val iconView = TextView(this).apply {
                text = badge.badgeIcon
                textSize = 28f
                setPadding(0, 0, (12 * density).toInt(), 0)
            }

            // Name + description column
            val infoColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val displayTitle = if (badge.currentLevel > 0) {
                "${badge.badgeName} ${badge.currentLevel}"
            } else {
                badge.badgeName
            }

            val nameView = TextView(this).apply {
                text = displayTitle
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val descView = TextView(this).apply {
                text = badge.badgeDescription
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 13f
                setPadding(0, (2 * density).toInt(), 0, 0)
            }

            infoColumn.addView(nameView)
            infoColumn.addView(descView)
            topRow.addView(iconView)
            topRow.addView(infoColumn)
            cardContent.addView(topRow)

            // Progress bar background
            val progressPercent = if (badge.isMaxLevel) {
                100f
            } else if (badge.nextLevelGoal > 0) {
                (badge.currentProgress.toFloat() / badge.nextLevelGoal * 100).coerceAtMost(100f)
            } else {
                0f
            }

            val progressBarBg = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * density).toInt()
                ).apply { topMargin = (14 * density).toInt() }
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
            }

            // We need a FrameLayout for the progress bar overlay
            val progressContainer = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * density).toInt()
                ).apply { topMargin = (14 * density).toInt() }
            }

            val progressBg = View(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_input))
            }

            val progressFg = View(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    0, // width will be set after layout
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                val bgColor = if (badge.isMaxLevel) {
                    0xFFFFD700.toInt() // Gold for max level
                } else {
                    ContextCompat.getColor(context, R.color.accent)
                }
                setBackgroundColor(bgColor)
            }

            progressContainer.addView(progressBg)
            progressContainer.addView(progressFg)
            cardContent.addView(progressContainer)

            // Set the width after layout is computed
            progressContainer.post {
                val totalWidth = progressContainer.width
                val fgWidth = (totalWidth * progressPercent / 100f).toInt()
                progressFg.layoutParams = progressFg.layoutParams.apply {
                    width = fgWidth
                }
                progressFg.requestLayout()
            }

            // Progress text
            val progressText = TextView(this).apply {
                text = if (badge.isMaxLevel) {
                    getString(R.string.badges_max_level)
                } else {
                    "${badge.currentProgress} / ${badge.nextLevelGoal}"
                }
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.END
                setPadding(0, (6 * density).toInt(), 0, 0)
            }
            cardContent.addView(progressText)

            card.addView(cardContent)
            badgesList.addView(card)
        }
    }
}
