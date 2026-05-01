package com.bounswe2026group8.emergencyhub.mesh.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.MeshMessageDto
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Read-only post detail for the "Offline Messages" archive. Renders the post
 * from the DTO passed in via Intent extras, then fetches the comment list
 * from the server.
 */
class MeshArchivePostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_JSON = "post_json"
    }

    private lateinit var commentAdapter: MeshCommentAdapter
    private lateinit var post: MeshMessageDto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesh_archive_post_detail)

        val json = intent.getStringExtra(EXTRA_POST_JSON)
        if (json.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.mesh_post_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        post = Gson().fromJson(json, MeshMessageDto::class.java)

        commentAdapter = MeshCommentAdapter()
        val recycler = findViewById<RecyclerView>(R.id.recyclerComments)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = commentAdapter

        findViewById<View>(R.id.linkBack).setOnClickListener { finish() }

        renderPost(post)
        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val api = RetrofitClient.getService(this@MeshArchivePostDetailActivity)
                    val response = api.getMeshComments(post.id)
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP ${response.code()}")
                    }
                    response.body().orEmpty()
                }
            }

            val txtHeading = findViewById<TextView>(R.id.txtCommentsHeading)
            val txtNone = findViewById<TextView>(R.id.txtNoComments)
            val recycler = findViewById<RecyclerView>(R.id.recyclerComments)

            result
                .onSuccess { dtos ->
                    val entities = dtos.map { it.toEntity() }
                    commentAdapter.submitList(entities)
                    txtHeading.text = getString(R.string.mesh_comments_heading, entities.size)
                    if (entities.isEmpty()) {
                        recycler.visibility = View.GONE
                        txtNone.visibility = View.VISIBLE
                    } else {
                        recycler.visibility = View.VISIBLE
                        txtNone.visibility = View.GONE
                    }
                }
                .onFailure { e ->
                    txtHeading.text = getString(R.string.mesh_comments_heading, 0)
                    txtNone.text = getString(R.string.mesh_archive_status_error, e.message ?: "unknown")
                    txtNone.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                }
        }
    }

    private fun renderPost(p: MeshMessageDto) {
        val txtType = findViewById<TextView>(R.id.txtPostType)
        when (p.postType) {
            "NEED_HELP" -> {
                txtType.text = getString(R.string.mesh_post_type_need)
                txtType.setTextColor(getColor(R.color.urgency_high))
                txtType.setBackgroundColor(getColor(R.color.urgency_high_bg))
                txtType.visibility = View.VISIBLE
            }
            "OFFER_HELP" -> {
                txtType.text = getString(R.string.mesh_post_type_offer)
                txtType.setTextColor(getColor(R.color.urgency_low))
                txtType.setBackgroundColor(getColor(R.color.urgency_low_bg))
                txtType.visibility = View.VISIBLE
            }
            else -> txtType.visibility = View.GONE
        }
        findViewById<TextView>(R.id.txtPostTitle).text = p.title.orEmpty()
        findViewById<TextView>(R.id.txtAuthor).text = p.authorDisplayName
            ?: "device-${p.authorDeviceId}"
        findViewById<TextView>(R.id.txtTime).text = formatTimestamp(p.createdAt)
        findViewById<TextView>(R.id.txtPostBody).text = p.body

        val txtLocation = findViewById<TextView>(R.id.txtPostLocation)
        val lat = p.latitude
        val lon = p.longitude
        if (lat != null && lon != null) {
            txtLocation.visibility = View.VISIBLE
            txtLocation.text = formatLocation(lat, lon, p.locAccuracyMeters, p.locCapturedAt)
        } else {
            txtLocation.visibility = View.GONE
        }
    }

    private fun MeshMessageDto.toEntity(): MeshMessage = MeshMessage(
        id = id,
        authorDeviceId = authorDeviceId,
        authorDisplayName = authorDisplayName,
        body = body,
        createdAt = createdAt,
        receivedAt = receivedAt,
        ttlHours = ttlHours,
        hopCount = hopCount,
        syncedToServer = true,
        latitude = latitude,
        longitude = longitude,
        locAccuracyMeters = locAccuracyMeters,
        locCapturedAt = locCapturedAt,
        title = title,
        postType = postType,
        parentPostId = parentPostId
    )

    private fun formatLocation(
        lat: Double,
        lon: Double,
        accuracyMeters: Float?,
        capturedAt: Long?
    ): String {
        val coords = String.format(Locale.US, "%.5f, %.5f", lat, lon)
        val parts = mutableListOf("📍 $coords")
        if (accuracyMeters != null) parts += "±${accuracyMeters.toInt()}m"
        if (capturedAt != null) {
            val ageSec = (System.currentTimeMillis() - capturedAt) / 1000
            val age = when {
                ageSec < 60 -> "${ageSec}s ago"
                ageSec < 3600 -> "${ageSec / 60}m ago"
                ageSec < 86400 -> "${ageSec / 3600}h ago"
                else -> "${ageSec / 86400}d ago"
            }
            parts += "fix $age"
        }
        return parts.joinToString(" · ")
    }

    private fun formatTimestamp(epochMillis: Long): String {
        val now = Calendar.getInstance()
        val msg = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val sameDay = now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)
        val pattern = if (sameDay) "HH:mm:ss" else "MMM d, HH:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMillis))
    }
}
