package com.bounswe2026group8.emergencyhub.mesh.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.MeshMessageDto
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.mesh.MeshServerSyncManager
import com.bounswe2026group8.emergencyhub.mesh.db.MeshMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Offline Messages" archive — read-only view of server-synced mesh inventory
 * from all users. On entry: pushes any local unsynced messages to the server,
 * then fetches the global post list and renders it with the same adapter the
 * offline forum uses.
 */
class MeshArchiveActivity : AppCompatActivity() {

    private lateinit var adapter: MeshMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesh_archive)

        adapter = MeshMessageAdapter()
        adapter.setOnPostClick { post ->
            // Pass the post DTO via Intent — avoids an extra round-trip to fetch
            // the same data we already have. Comments fetch happens in detail.
            val dto = post.toDto()
            val intent = Intent(this, MeshArchivePostDetailActivity::class.java).apply {
                putExtra(MeshArchivePostDetailActivity.EXTRA_POST_JSON, Gson().toJson(dto))
            }
            startActivity(intent)
        }
        val recycler = findViewById<RecyclerView>(R.id.recyclerArchive)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        loadFlow()
    }

    override fun onResume() {
        super.onResume()
        loadFlow()
    }

    private fun loadFlow() {
        val status = findViewById<TextView>(R.id.txtStatus)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val empty = findViewById<TextView>(R.id.txtEmpty)
        val recycler = findViewById<RecyclerView>(R.id.recyclerArchive)

        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        status.text = getString(R.string.mesh_archive_status_uploading)

        lifecycleScope.launch {
            // 1. Push our local inventory up first (no-op if no internet / nothing new).
            MeshServerSyncManager.uploadIfOnline(this@MeshArchiveActivity)

            // 2. Fetch the global feed.
            status.text = getString(R.string.mesh_archive_status_loading)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val api = RetrofitClient.getService(this@MeshArchiveActivity)
                    val response = api.getMeshPosts()
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP ${response.code()}")
                    }
                    response.body().orEmpty()
                }
            }

            progress.visibility = View.GONE
            result
                .onSuccess { posts ->
                    val entities = posts.map { it.toEntity() }
                    // Comment counts aren't included in the list endpoint — show 0
                    // here; the detail view will fetch and display the real list.
                    adapter.submitList(entities, emptyMap())
                    if (entities.isEmpty()) {
                        recycler.visibility = View.GONE
                        empty.visibility = View.VISIBLE
                    } else {
                        recycler.visibility = View.VISIBLE
                        empty.visibility = View.GONE
                    }
                    status.text = getString(R.string.mesh_archive_status_loaded, entities.size)
                }
                .onFailure { e ->
                    recycler.visibility = View.GONE
                    empty.visibility = View.GONE
                    status.text = getString(
                        R.string.mesh_archive_status_error,
                        e.message ?: getString(R.string.mesh_error_unknown)
                    )
                }
        }
    }

    private fun MeshMessage.toDto(): MeshMessageDto = MeshMessageDto(
        id = id,
        authorDeviceId = authorDeviceId,
        authorDisplayName = authorDisplayName,
        body = body,
        createdAt = createdAt,
        receivedAt = receivedAt,
        ttlHours = ttlHours,
        hopCount = hopCount,
        latitude = latitude,
        longitude = longitude,
        locAccuracyMeters = locAccuracyMeters,
        locCapturedAt = locCapturedAt,
        title = title,
        postType = postType,
        parentPostId = parentPostId
    )
}

/**
 * Convert a server-fetched DTO into the same Room entity shape the existing
 * adapters consume. We never persist these — the archive feed is fetched fresh
 * each time — but reusing the entity type avoids duplicating render code.
 */
internal fun MeshMessageDto.toEntity(): MeshMessage = MeshMessage(
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
