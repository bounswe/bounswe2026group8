package com.bounswe2026group8.emergencyhub.mesh.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.MeshForegroundService
import com.bounswe2026group8.emergencyhub.mesh.MeshSyncManager
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.bounswe2026group8.emergencyhub.mesh.voice.MeshVoiceBinding
import com.bounswe2026group8.emergencyhub.offline.data.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MeshPostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "post_id"
    }

    private var syncManager: MeshSyncManager? = null
    private var serviceBound = false

    private lateinit var commentAdapter: MeshCommentAdapter
    private lateinit var postId: String
    private var post: MeshMessage? = null
    private var commentVoice: MeshVoiceBinding? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val meshBinder = binder as MeshForegroundService.MeshBinder
            syncManager = meshBinder.getSyncManager()
            // Refresh comments when peers gossip new ones to us.
            syncManager?.onMessagesUpdated = { runOnUiThread { loadComments() } }
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            syncManager = null
            serviceBound = false
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val switch = findViewById<SwitchMaterial>(R.id.switchShareLocation)
        if (granted) {
            MeshSyncManager.setLocationSharingEnabled(this, true)
            switch.isChecked = true
        } else {
            MeshSyncManager.setLocationSharingEnabled(this, false)
            switch.isChecked = false
            Toast.makeText(
                this, getString(R.string.mesh_location_permission_needed), Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesh_post_detail)

        postId = intent.getStringExtra(EXTRA_POST_ID).orEmpty()
        if (postId.isEmpty()) {
            Toast.makeText(this, getString(R.string.mesh_post_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Bind to the running service for sendComment
        val intent = Intent(this, MeshForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        commentAdapter = MeshCommentAdapter()
        val recycler = findViewById<RecyclerView>(R.id.recyclerComments)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = commentAdapter

        findViewById<View>(R.id.linkBack).setOnClickListener { finish() }

        // Share location toggle (sticky default)
        val switchShareLocation = findViewById<SwitchMaterial>(R.id.switchShareLocation)
        switchShareLocation.isChecked = MeshSyncManager.isLocationSharingEnabled(this)
        switchShareLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val granted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    MeshSyncManager.setLocationSharingEnabled(this, true)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                MeshSyncManager.setLocationSharingEnabled(this, false)
            }
        }

        val inputComment = findViewById<TextInputEditText>(R.id.inputComment)
        commentVoice = MeshVoiceBinding.bind(
            activity = this,
            editText = inputComment,
            micButton = findViewById<ImageButton>(R.id.btnVoiceComment),
        )
        findViewById<MaterialButton>(R.id.btnPostComment).setOnClickListener {
            val body = inputComment.text?.toString()?.trim().orEmpty()
            if (body.isEmpty()) return@setOnClickListener
            val mgr = syncManager
            if (mgr == null) {
                Toast.makeText(this, getString(R.string.mesh_start_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendCommentWithOptionalLocation(mgr, body, switchShareLocation.isChecked)
            inputComment.text?.clear()
        }

        loadPost()
        loadComments()
    }

    override fun onResume() {
        super.onResume()
        commentVoice?.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun loadPost() {
        val dao = AppDatabase.getDatabase(this).meshMessageDao()
        CoroutineScope(Dispatchers.IO).launch {
            val loaded = dao.getPostById(postId)
            withContext(Dispatchers.Main) {
                if (loaded == null || loaded.parentPostId != null) {
                    Toast.makeText(
                        this@MeshPostDetailActivity,
                        getString(R.string.mesh_post_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@withContext
                }
                post = loaded
                renderPost(loaded)
            }
        }
    }

    private fun loadComments() {
        val dao = AppDatabase.getDatabase(this).meshMessageDao()
        CoroutineScope(Dispatchers.IO).launch {
            val comments = dao.getCommentsForPost(postId)
            withContext(Dispatchers.Main) {
                commentAdapter.submitList(comments)
                findViewById<TextView>(R.id.txtCommentsHeading).text =
                    getString(R.string.mesh_comments_heading, comments.size)
                val recycler = findViewById<RecyclerView>(R.id.recyclerComments)
                val txtNone = findViewById<TextView>(R.id.txtNoComments)
                if (comments.isEmpty()) {
                    recycler.visibility = View.GONE
                    txtNone.visibility = View.VISIBLE
                } else {
                    recycler.visibility = View.VISIBLE
                    txtNone.visibility = View.GONE
                }
            }
        }
    }

    private fun renderPost(p: MeshMessage) {
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
            ?: getString(R.string.mesh_device_fallback_format, p.authorDeviceId)
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

    private fun sendCommentWithOptionalLocation(
        mgr: MeshSyncManager,
        body: String,
        shareLocation: Boolean
    ) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!shareLocation || !granted) {
            mgr.sendComment(parentPostId = postId, body = body)
            Toast.makeText(this, getString(R.string.mesh_comment_created), Toast.LENGTH_SHORT).show()
            return
        }

        // Cold-cache phones need an active poll — fetcher handles the fallback.
        MeshLocationFetcher.fetch(this) { location ->
            if (location != null) {
                mgr.sendComment(
                    parentPostId = postId,
                    body = body,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                    locCapturedAt = location.time
                )
            } else {
                Toast.makeText(
                    this, getString(R.string.mesh_location_unavailable), Toast.LENGTH_SHORT
                ).show()
                mgr.sendComment(parentPostId = postId, body = body)
            }
            Toast.makeText(this, getString(R.string.mesh_comment_created), Toast.LENGTH_SHORT).show()
        }
    }

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
            parts += formatFixAge(this, ageSec)
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
