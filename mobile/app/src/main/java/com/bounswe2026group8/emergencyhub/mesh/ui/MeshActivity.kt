package com.bounswe2026group8.emergencyhub.mesh.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
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
import com.bounswe2026group8.emergencyhub.offline.data.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeshActivity : AppCompatActivity() {

    private var syncManager: MeshSyncManager? = null
    private lateinit var adapter: MeshMessageAdapter
    private var meshRunning = false
    private var serviceBound = false

    private var pendingStartAction: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val meshBinder = binder as MeshForegroundService.MeshBinder
            syncManager = meshBinder.getSyncManager()
            syncManager?.onMessagesUpdated = { runOnUiThread { loadPosts() } }
            syncManager?.onPeersUpdated = { runOnUiThread { renderPeers() } }
            runOnUiThread { renderPeers() }
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            syncManager = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            pendingStartAction?.invoke()
            pendingStartAction = null
        } else {
            Toast.makeText(this, getString(R.string.mesh_permissions_needed), Toast.LENGTH_LONG).show()
        }
    }

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (btManager.adapter?.isEnabled == true) {
            pendingStartAction?.invoke()
            pendingStartAction = null
        } else {
            Toast.makeText(this, getString(R.string.mesh_bluetooth_needed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesh)

        // --- RecyclerView ---
        adapter = MeshMessageAdapter()
        adapter.setOnPostClick { post ->
            val intent = Intent(this, MeshPostDetailActivity::class.java).apply {
                putExtra(MeshPostDetailActivity.EXTRA_POST_ID, post.id)
            }
            startActivity(intent)
        }
        val recycler = findViewById<RecyclerView>(R.id.recyclerMessages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // --- Back ---
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // --- Display name ---
        val editName = findViewById<TextInputEditText>(R.id.editDisplayName)

        // --- Start/Stop toggle ---
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleMesh)
        val btnNewPost = findViewById<MaterialButton>(R.id.btnNewPost)
        val txtStatus = findViewById<View>(R.id.txtStatus) as TextView
        val statusDot = findViewById<View>(R.id.statusDot)

        btnNewPost.isEnabled = false
        btnNewPost.setOnClickListener {
            if (syncManager == null) {
                Toast.makeText(this, getString(R.string.mesh_start_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, MeshCreatePostActivity::class.java))
        }

        btnToggle.setOnClickListener {
            if (meshRunning) {
                // Stop the service
                val stopIntent = Intent(this, MeshForegroundService::class.java).apply {
                    action = MeshForegroundService.ACTION_STOP
                }
                startService(stopIntent)
                if (serviceBound) {
                    unbindService(serviceConnection)
                    serviceBound = false
                }
                syncManager = null
                meshRunning = false

                btnToggle.text = getString(R.string.mesh_start)
                txtStatus.text = getString(R.string.mesh_status_off)
                statusDot.setBackgroundColor(getColor(R.color.text_muted))
                findViewById<TextView>(R.id.txtPeers).text =
                    getString(R.string.mesh_peers_none)
                editName.isEnabled = true
                btnNewPost.isEnabled = false
            } else {
                val startAction = {
                    val name = editName.text?.toString()?.trim()?.ifEmpty { null }

                    // Start the foreground service
                    val serviceIntent = Intent(this, MeshForegroundService::class.java).apply {
                        putExtra(MeshForegroundService.EXTRA_DISPLAY_NAME, name)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

                    // Bind to get access to the SyncManager
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

                    meshRunning = true
                    val deviceId = MeshSyncManager.getDeviceId(this)
                    btnToggle.text = getString(R.string.mesh_stop)
                    txtStatus.text = "BLE Mesh active as $deviceId"
                    statusDot.setBackgroundColor(getColor(R.color.success))
                    editName.isEnabled = false
                    btnNewPost.isEnabled = true
                }

                startWithBlePermissions(startAction)
            }
        }

        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        // Reclaim the SyncManager's onMessagesUpdated callback after returning from
        // detail/create — otherwise it still points at the destroyed detail activity.
        syncManager?.onMessagesUpdated = { runOnUiThread { loadPosts() } }
        loadPosts()
        // Server upload is intentionally NOT triggered here — this screen is
        // accessible pre-login and would have no auth token. Upload is wired in
        // DashboardActivity.onResume (fires after login) and MeshArchiveActivity
        // (fires on archive open).
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        // Note: service keeps running even if activity is destroyed
    }

    private fun startWithBlePermissions(onReady: () -> Unit) {
        val needed = getBlePermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            pendingStartAction = { startWithBlePermissions(onReady) }
            permissionLauncher.launch(needed.toTypedArray())
            return
        }

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (btManager.adapter?.isEnabled != true) {
            pendingStartAction = onReady
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        onReady()
    }

    private fun getBlePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun loadPosts() {
        val dao = AppDatabase.getDatabase(this).meshMessageDao()
        CoroutineScope(Dispatchers.IO).launch {
            val posts = dao.getAllPosts()
            val counts = posts.associate { it.id to dao.getCommentCount(it.id) }
            withContext(Dispatchers.Main) {
                adapter.submitList(posts, counts)
                val recycler = findViewById<RecyclerView>(R.id.recyclerMessages)
                val txtEmpty = findViewById<View>(R.id.txtEmpty)
                if (posts.isEmpty()) {
                    recycler.visibility = View.GONE
                    txtEmpty.visibility = View.VISIBLE
                } else {
                    recycler.visibility = View.VISIBLE
                    txtEmpty.visibility = View.GONE
                    recycler.scrollToPosition(0)
                }
            }
        }
    }

    private fun renderPeers() {
        val txtPeers = findViewById<TextView>(R.id.txtPeers)
        val peers = syncManager?.getConnectedPeers().orEmpty()
        if (peers.isEmpty()) {
            txtPeers.text = getString(R.string.mesh_peers_none)
        } else {
            val names = peers.joinToString(", ") {
                it.displayName?.takeIf { n -> n.isNotBlank() } ?: "device-${it.deviceId}"
            }
            txtPeers.text = getString(R.string.mesh_peers_format, peers.size, names)
        }
    }
}
