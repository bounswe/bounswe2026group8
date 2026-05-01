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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.mesh.MeshForegroundService
import com.bounswe2026group8.emergencyhub.mesh.MeshSyncManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class MeshCreatePostActivity : AppCompatActivity() {

    private var syncManager: MeshSyncManager? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val meshBinder = binder as MeshForegroundService.MeshBinder
            syncManager = meshBinder.getSyncManager()
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
        setContentView(R.layout.activity_mesh_create_post)

        // Bind to the running mesh service so we can call sendPost on its SyncManager.
        // The user can only reach this screen when the mesh is on (the parent activity
        // gates the "+ New Post" button), so the service should already be running.
        val intent = Intent(this, MeshForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        val toggle = findViewById<MaterialButtonToggleGroup>(R.id.postTypeToggle)
        val inputTitle = findViewById<TextInputEditText>(R.id.inputTitle)
        val inputContent = findViewById<TextInputEditText>(R.id.inputContent)
        val switchShareLocation = findViewById<SwitchMaterial>(R.id.switchShareLocation)
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreate)
        val txtError = findViewById<TextView>(R.id.txtError)

        // Persisted toggle is the user's default (sticky across compose sessions).
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

        findViewById<View>(R.id.linkBack).setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            txtError.visibility = View.GONE

            val type = when (toggle.checkedButtonId) {
                R.id.btnTypeNeed -> "NEED_HELP"
                R.id.btnTypeOffer -> "OFFER_HELP"
                else -> null
            }
            if (type == null) {
                showError(txtError, R.string.mesh_type_required)
                return@setOnClickListener
            }

            val title = inputTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                showError(txtError, R.string.mesh_title_required)
                return@setOnClickListener
            }

            val content = inputContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) {
                showError(txtError, R.string.mesh_content_required)
                return@setOnClickListener
            }

            val mgr = syncManager
            if (mgr == null) {
                Toast.makeText(this, getString(R.string.mesh_start_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postWithOptionalLocation(mgr, title, content, type, switchShareLocation.isChecked)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun showError(txtError: TextView, resId: Int) {
        txtError.text = getString(resId)
        txtError.visibility = View.VISIBLE
    }

    private fun postWithOptionalLocation(
        mgr: MeshSyncManager,
        title: String,
        content: String,
        type: String,
        shareLocation: Boolean
    ) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!shareLocation || !granted) {
            mgr.sendPost(title = title, body = content, postType = type)
            finishWithSuccess()
            return
        }

        // Cold-cache phones (no app polled location recently) need an active poll —
        // MeshLocationFetcher tries lastLocation first then falls back to a fresh fix.
        MeshLocationFetcher.fetch(this) { location ->
            if (location != null) {
                mgr.sendPost(
                    title = title,
                    body = content,
                    postType = type,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                    locCapturedAt = location.time
                )
            } else {
                Toast.makeText(
                    this, getString(R.string.mesh_location_unavailable), Toast.LENGTH_SHORT
                ).show()
                mgr.sendPost(title = title, body = content, postType = type)
            }
            finishWithSuccess()
        }
    }

    private fun finishWithSuccess() {
        Toast.makeText(this, getString(R.string.mesh_post_created), Toast.LENGTH_SHORT).show()
        finish()
    }
}
