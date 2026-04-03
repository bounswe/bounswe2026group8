package com.bounswe2026group8.emergencyhub.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.CreateCommentRequest
import com.bounswe2026group8.emergencyhub.api.HelpRequestComment
import com.bounswe2026group8.emergencyhub.api.HelpRequestDetail
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Displays full details of a help request including location map,
 * comments with expert badges, and a comment input form.
 *
 * Receives the request ID via [EXTRA_REQUEST_ID] intent extra.
 */
class HelpRequestDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
    }

    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: HelpRequestCommentAdapter

    // Detail views
    private lateinit var txtDetailTitle: TextView
    private lateinit var txtDetailCategory: TextView
    private lateinit var txtDetailUrgency: TextView
    private lateinit var txtDetailStatus: TextView
    private lateinit var txtDetailDescription: TextView
    private lateinit var txtDetailAuthor: TextView
    private lateinit var txtDetailTimeAgo: TextView

    // Map
    private lateinit var cardMap: MaterialCardView
    private lateinit var txtMapLocationText: TextView
    private lateinit var txtLocationOnly: TextView
    private var mapView: MapView? = null

    // Comments
    private lateinit var txtCommentsHeader: TextView
    private lateinit var recyclerComments: RecyclerView
    private lateinit var progressComments: ProgressBar
    private lateinit var txtNoComments: TextView
    private lateinit var inputComment: TextInputEditText
    private lateinit var btnSendComment: MaterialButton

    private var requestId: Int = -1

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid configuration — must be before setContentView
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_help_request_detail)

        tokenManager = TokenManager(this)
        if (!tokenManager.isLoggedIn()) {
            navigateToLanding()
            return
        }

        requestId = intent.getIntExtra(EXTRA_REQUEST_ID, -1)
        if (requestId == -1) {
            Toast.makeText(this, "Invalid request", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupRecyclerView()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        btnSendComment.setOnClickListener { submitComment() }

        fetchDetail()
        fetchComments()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDetach()
    }

    // ── View binding ─────────────────────────────────────────────────────

    private fun bindViews() {
        txtDetailTitle = findViewById(R.id.txtDetailTitle)
        txtDetailCategory = findViewById(R.id.txtDetailCategory)
        txtDetailUrgency = findViewById(R.id.txtDetailUrgency)
        txtDetailStatus = findViewById(R.id.txtDetailStatus)
        txtDetailDescription = findViewById(R.id.txtDetailDescription)
        txtDetailAuthor = findViewById(R.id.txtDetailAuthor)
        txtDetailTimeAgo = findViewById(R.id.txtDetailTimeAgo)

        cardMap = findViewById(R.id.cardMap)
        mapView = findViewById(R.id.mapView)
        txtMapLocationText = findViewById(R.id.txtMapLocationText)
        txtLocationOnly = findViewById(R.id.txtLocationOnly)

        txtCommentsHeader = findViewById(R.id.txtCommentsHeader)
        recyclerComments = findViewById(R.id.recyclerComments)
        progressComments = findViewById(R.id.progressComments)
        txtNoComments = findViewById(R.id.txtNoComments)
        inputComment = findViewById(R.id.inputComment)
        btnSendComment = findViewById(R.id.btnSendComment)
    }

    private fun setupRecyclerView() {
        commentAdapter = HelpRequestCommentAdapter(emptyList())
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.isNestedScrollingEnabled = false
        recyclerComments.adapter = commentAdapter
    }

    // ── Fetch detail ─────────────────────────────────────────────────────

    private fun fetchDetail() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .getHelpRequestDetail(requestId)

                if (response.isSuccessful) {
                    response.body()?.let { displayDetail(it) }
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    Toast.makeText(
                        this@HelpRequestDetailActivity,
                        "Failed to load details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayDetail(detail: HelpRequestDetail) {
        txtDetailTitle.text = detail.title
        txtDetailDescription.text = detail.description
        txtDetailAuthor.text = "Posted by ${detail.author.fullName}"
        txtDetailTimeAgo.text = formatTimeAgo(detail.createdAt)

        // Category badge
        txtDetailCategory.text = detail.category.lowercase().replaceFirstChar { it.uppercase() }
        val (catText, catBg) = categoryColors(detail.category)
        txtDetailCategory.setTextColor(ContextCompat.getColor(this, catText))
        txtDetailCategory.background.mutate().setTint(ContextCompat.getColor(this, catBg))

        // Urgency badge
        txtDetailUrgency.text = detail.urgency.lowercase().replaceFirstChar { it.uppercase() }
        val (urgText, urgBg) = urgencyColors(detail.urgency)
        txtDetailUrgency.setTextColor(ContextCompat.getColor(this, urgText))
        txtDetailUrgency.background.mutate().setTint(ContextCompat.getColor(this, urgBg))

        // Status badge — highlight EXPERT_RESPONDING
        when (detail.status) {
            "EXPERT_RESPONDING" -> {
                txtDetailStatus.text = getString(R.string.status_expert_responding)
                txtDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_secondary))
                txtDetailStatus.background.mutate()
                    .setTint(ContextCompat.getColor(this, R.color.badge_accent_bg))
            }
            "RESOLVED" -> {
                txtDetailStatus.text = getString(R.string.status_resolved)
                txtDetailStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                txtDetailStatus.background.mutate()
                    .setTint(ContextCompat.getColor(this, R.color.urgency_low_bg))
            }
            else -> {
                txtDetailStatus.text = getString(R.string.status_open)
            }
        }

        // Map / location
        val lat = detail.latitude?.toDoubleOrNull()
        val lng = detail.longitude?.toDoubleOrNull()

        if (lat != null && lng != null) {
            cardMap.visibility = View.VISIBLE
            txtLocationOnly.visibility = View.GONE
            setupMap(lat, lng)
            if (!detail.locationText.isNullOrBlank()) {
                txtMapLocationText.text = "📍 ${detail.locationText}"
                txtMapLocationText.visibility = View.VISIBLE
            }
        } else if (!detail.locationText.isNullOrBlank()) {
            cardMap.visibility = View.GONE
            txtLocationOnly.text = "📍 ${detail.locationText}"
            txtLocationOnly.visibility = View.VISIBLE
        } else {
            cardMap.visibility = View.GONE
            txtLocationOnly.visibility = View.GONE
        }

        // Comments header count
        txtCommentsHeader.text = getString(R.string.comments_count_label, detail.commentCount)
    }

    private fun setupMap(lat: Double, lng: Double) {
        mapView?.let { map ->
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)

            val point = GeoPoint(lat, lng)
            map.controller.setZoom(15.0)
            map.controller.setCenter(point)

            val marker = Marker(map)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Request location"
            map.overlays.add(marker)
        }
    }

    // ── Fetch comments ───────────────────────────────────────────────────

    private fun fetchComments() {
        progressComments.visibility = View.VISIBLE
        recyclerComments.visibility = View.GONE
        txtNoComments.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .getHelpRequestComments(requestId)

                if (response.isSuccessful) {
                    val comments = response.body() ?: emptyList()
                    if (comments.isEmpty()) {
                        txtNoComments.visibility = View.VISIBLE
                    } else {
                        commentAdapter.updateItems(comments)
                        recyclerComments.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@HelpRequestDetailActivity,
                        "Failed to load comments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressComments.visibility = View.GONE
            }
        }
    }

    // ── Submit comment ───────────────────────────────────────────────────

    private fun submitComment() {
        val content = inputComment.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSendComment.isEnabled = false
        btnSendComment.text = getString(R.string.sending)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .createHelpRequestComment(requestId, CreateCommentRequest(content))

                if (response.isSuccessful) {
                    val comment = response.body()!!
                    inputComment.text?.clear()

                    // Show the new comment immediately
                    txtNoComments.visibility = View.GONE
                    recyclerComments.visibility = View.VISIBLE
                    commentAdapter.addComment(comment)

                    // Scroll to bottom to show the new comment
                    recyclerComments.post {
                        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    }

                    // Re-fetch detail — status may have changed to EXPERT_RESPONDING
                    fetchDetail()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    val errorText = response.errorBody()?.string() ?: "Failed to post comment."
                    Toast.makeText(
                        this@HelpRequestDetailActivity,
                        errorText,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSendComment.isEnabled = true
                btnSendComment.text = getString(R.string.send)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun categoryColors(category: String): Pair<Int, Int> = when (category) {
        "MEDICAL"   -> R.color.category_medical   to R.color.category_medical_bg
        "FOOD"      -> R.color.category_food       to R.color.category_food_bg
        "SHELTER"   -> R.color.category_shelter    to R.color.category_shelter_bg
        "TRANSPORT" -> R.color.category_transport  to R.color.category_transport_bg
        else        -> R.color.text_secondary      to R.color.badge_muted_bg
    }

    private fun urgencyColors(urgency: String): Pair<Int, Int> = when (urgency) {
        "HIGH"   -> R.color.urgency_high   to R.color.urgency_high_bg
        "MEDIUM" -> R.color.urgency_medium to R.color.urgency_medium_bg
        else     -> R.color.urgency_low    to R.color.urgency_low_bg
    }

    private fun formatTimeAgo(iso: String): String {
        return try {
            val trimmed = iso.substringBefore(".").substringBefore("Z").substringBefore("+")
            val millis = dateFormat.parse(trimmed)?.time ?: return iso
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (_: Exception) {
            iso
        }
    }

    private fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
