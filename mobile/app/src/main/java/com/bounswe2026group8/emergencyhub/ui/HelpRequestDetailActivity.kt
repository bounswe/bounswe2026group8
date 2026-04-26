package com.bounswe2026group8.emergencyhub.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.bounswe2026group8.emergencyhub.api.HelpRequestDetail
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.api.UpdateHelpRequestStatusRequest
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.util.BadgeUtils
import com.bounswe2026group8.emergencyhub.util.TimeUtils
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HelpRequestDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
    }

    private lateinit var tokenManager: TokenManager
    private lateinit var commentAdapter: HelpRequestCommentAdapter

    private lateinit var txtDetailTitle: TextView
    private lateinit var txtDetailCategory: TextView
    private lateinit var txtDetailUrgency: TextView
    private lateinit var txtDetailStatus: TextView
    private lateinit var txtDetailDescription: TextView
    private lateinit var txtDetailAuthor: TextView
    private lateinit var txtDetailTimeAgo: TextView
    private lateinit var cardMap: MaterialCardView
    private lateinit var txtMapLocationText: TextView
    private lateinit var txtLocationOnly: TextView
    private var mapView: MapView? = null
    private lateinit var imageGalleryScroll: HorizontalScrollView
    private lateinit var imageGallery: LinearLayout
    private lateinit var txtCommentsHeader: TextView
    private lateinit var recyclerComments: RecyclerView
    private lateinit var progressComments: ProgressBar
    private lateinit var txtNoComments: TextView
    private lateinit var inputComment: TextInputEditText
    private lateinit var btnSendComment: MaterialButton

    private var requestId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Toast.makeText(this, getString(R.string.help_request_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupRecyclerView()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        btnSendComment.setOnClickListener { submitComment() }
        findViewById<MaterialButton>(R.id.btnDeleteRequest).setOnClickListener { confirmDeleteRequest() }
        findViewById<MaterialButton>(R.id.btnResolveRequest).setOnClickListener { resolveRequest() }

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
        imageGalleryScroll = findViewById(R.id.imageGalleryScroll)
        imageGallery = findViewById(R.id.imageGallery)
        txtCommentsHeader = findViewById(R.id.txtCommentsHeader)
        recyclerComments = findViewById(R.id.recyclerComments)
        progressComments = findViewById(R.id.progressComments)
        txtNoComments = findViewById(R.id.txtNoComments)
        inputComment = findViewById(R.id.inputComment)
        btnSendComment = findViewById(R.id.btnSendComment)
    }

    private fun setupRecyclerView() {
        commentAdapter = HelpRequestCommentAdapter(
            items = emptyList(),
            currentUserId = tokenManager.getUser()?.id,
            onDeleteClick = { comment -> confirmDeleteComment(comment.id) }
        )
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.isNestedScrollingEnabled = false
        recyclerComments.adapter = commentAdapter
    }

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
                        getString(R.string.help_request_detail_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayDetail(detail: HelpRequestDetail) {
        txtDetailTitle.text = detail.title
        txtDetailDescription.text = detail.description
        txtDetailAuthor.text = getString(R.string.help_request_author_format, detail.author.fullName)
        txtDetailTimeAgo.text = TimeUtils.timeAgo(detail.createdAt)

        txtDetailCategory.text = BadgeUtils.formatCategoryLabel(this, detail.category)
        val (catText, catBg) = BadgeUtils.categoryColors(detail.category)
        txtDetailCategory.setTextColor(ContextCompat.getColor(this, catText))
        txtDetailCategory.background.mutate().setTint(ContextCompat.getColor(this, catBg))

        txtDetailUrgency.text = BadgeUtils.formatUrgencyLabel(this, detail.urgency)
        val (urgText, urgBg) = BadgeUtils.urgencyColors(detail.urgency)
        txtDetailUrgency.setTextColor(ContextCompat.getColor(this, urgText))
        txtDetailUrgency.background.mutate().setTint(ContextCompat.getColor(this, urgBg))

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
            else -> txtDetailStatus.text = getString(R.string.status_open)
        }

        if (detail.imageUrls.isNotEmpty()) {
            imageGalleryScroll.visibility = View.VISIBLE
            imageGallery.removeAllViews()
            val density = resources.displayMetrics.density
            val sizePx = (140 * density).toInt()
            val marginPx = (8 * density).toInt()
            for (url in detail.imageUrls) {
                val imgView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also {
                        it.marginEnd = marginPx
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                Glide.with(this)
                    .load(RetrofitClient.resolveImageUrl(url))
                    .centerCrop()
                    .into(imgView)
                imageGallery.addView(imgView)
            }
        } else {
            imageGalleryScroll.visibility = View.GONE
        }

        val lat = detail.latitude?.toDoubleOrNull()
        val lng = detail.longitude?.toDoubleOrNull()

        if (lat != null && lng != null) {
            cardMap.visibility = View.VISIBLE
            txtLocationOnly.visibility = View.GONE
            setupMap(lat, lng)
            if (!detail.locationText.isNullOrBlank()) {
                txtMapLocationText.text = getString(R.string.location_pin_format, detail.locationText)
                txtMapLocationText.visibility = View.VISIBLE
            } else {
                txtMapLocationText.visibility = View.GONE
            }
        } else if (!detail.locationText.isNullOrBlank()) {
            cardMap.visibility = View.GONE
            txtLocationOnly.text = getString(R.string.location_pin_format, detail.locationText)
            txtLocationOnly.visibility = View.VISIBLE
        } else {
            cardMap.visibility = View.GONE
            txtLocationOnly.visibility = View.GONE
        }

        txtCommentsHeader.text = getString(R.string.comments_count_label, detail.commentCount)

        val currentUserId = tokenManager.getUser()?.id
        val isAuthor = currentUserId == detail.author.id
        findViewById<MaterialButton>(R.id.btnDeleteRequest).visibility =
            if (isAuthor) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnResolveRequest).visibility =
            if (isAuthor && detail.status != "RESOLVED") View.VISIBLE else View.GONE
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
            marker.title = getString(R.string.help_request_map_marker_title)
            map.overlays.add(marker)
        }
    }

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
                        getString(R.string.help_request_comments_load_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressComments.visibility = View.GONE
            }
        }
    }

    private fun resolveRequest() {
        val btnResolve = findViewById<MaterialButton>(R.id.btnResolveRequest)
        btnResolve.isEnabled = false
        btnResolve.text = getString(R.string.resolving)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .updateHelpRequestStatus(requestId, UpdateHelpRequestStatusRequest("RESOLVED"))
                if (response.isSuccessful) {
                    response.body()?.let { displayDetail(it) }
                    Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.help_request_resolved), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.help_request_status_update_failed), Toast.LENGTH_SHORT).show()
                    btnResolve.isEnabled = true
                    btnResolve.text = getString(R.string.mark_resolved)
                }
            } catch (_: Exception) {
                Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                btnResolve.isEnabled = true
                btnResolve.text = getString(R.string.mark_resolved)
            }
        }
    }

    private fun confirmDeleteRequest() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.help_request_delete_title))
            .setMessage(getString(R.string.help_request_delete_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteRequest() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteRequest() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .deleteHelpRequest(requestId)
                if (response.code() == 403) {
                    Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.help_request_delete_forbidden), Toast.LENGTH_SHORT).show()
                    return@launch
                }
            } catch (_: Exception) {
            }
            Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.help_request_deleted), Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun confirmDeleteComment(commentId: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.comment_delete_title))
            .setMessage(getString(R.string.comment_delete_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteComment(commentId) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteComment(commentId: Int) {
        commentAdapter.removeComment(commentId)
        if (commentAdapter.itemCount == 0) {
            recyclerComments.visibility = View.GONE
            txtNoComments.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(this@HelpRequestDetailActivity)
                    .deleteHelpRequestComment(commentId)
                if (response.isSuccessful || response.code() == 204) {
                    fetchDetail()
                } else {
                    fetchComments()
                    Toast.makeText(this@HelpRequestDetailActivity, getString(R.string.comment_delete_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                fetchComments()
            }
        }
    }

    private fun submitComment() {
        val content = inputComment.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.comment_empty_error), Toast.LENGTH_SHORT).show()
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
                    txtNoComments.visibility = View.GONE
                    recyclerComments.visibility = View.VISIBLE
                    commentAdapter.addComment(comment)
                    recyclerComments.post {
                        findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
                            .fullScroll(View.FOCUS_DOWN)
                    }
                    fetchDetail()
                } else if (response.code() == 401) {
                    tokenManager.clear()
                    navigateToLanding()
                } else {
                    val errorText = response.errorBody()?.string() ?: getString(R.string.comment_post_failed)
                    Toast.makeText(this@HelpRequestDetailActivity, errorText, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HelpRequestDetailActivity,
                    getString(R.string.network_error_with_message, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnSendComment.isEnabled = true
                btnSendComment.text = getString(R.string.send)
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
