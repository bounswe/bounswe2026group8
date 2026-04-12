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
    private var useBle = true
    private var serviceBound = false

    private var pendingStartAction: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val meshBinder = binder as MeshForegroundService.MeshBinder
            syncManager = meshBinder.getSyncManager()
            syncManager?.onMessagesUpdated = { runOnUiThread { loadMessages() } }
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
        val recycler = findViewById<RecyclerView>(R.id.recyclerMessages)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // --- Back ---
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // --- Display name ---
        val editName = findViewById<TextInputEditText>(R.id.editDisplayName)

        // --- Mode selector: BLE vs Mock TCP ---
        val btnBle = findViewById<MaterialButton>(R.id.btnModeAdvertise)
        val btnTcp = findViewById<MaterialButton>(R.id.btnModeDiscover)
        btnBle.text = getString(R.string.mesh_mode_ble)
        btnTcp.text = getString(R.string.mesh_mode_tcp)
        updateModeButtons(btnBle, btnTcp)

        btnBle.setOnClickListener {
            useBle = true
            updateModeButtons(btnBle, btnTcp)
        }
        btnTcp.setOnClickListener {
            useBle = false
            updateModeButtons(btnBle, btnTcp)
        }

        // --- Start/Stop toggle ---
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleMesh)
        val txtStatus = findViewById<View>(R.id.txtStatus) as android.widget.TextView
        val statusDot = findViewById<View>(R.id.statusDot)

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
                btnBle.isEnabled = true
                btnTcp.isEnabled = true
                editName.isEnabled = true
            } else {
                val startAction = {
                    val name = editName.text?.toString()?.trim()?.ifEmpty { null }

                    // Start the foreground service
                    val serviceIntent = Intent(this, MeshForegroundService::class.java).apply {
                        putExtra(MeshForegroundService.EXTRA_USE_BLE, useBle)
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
                    val modeLabel = if (useBle) "BLE Mesh" else "TCP Mock"
                    txtStatus.text = "$modeLabel active as $deviceId"
                    statusDot.setBackgroundColor(getColor(R.color.success))
                    btnBle.isEnabled = false
                    btnTcp.isEnabled = false
                    editName.isEnabled = false
                }

                if (useBle) {
                    startWithBlePermissions(startAction)
                } else {
                    startAction()
                }
            }
        }

        // --- Send ---
        val editMessage = findViewById<TextInputEditText>(R.id.editMessage)
        findViewById<MaterialButton>(R.id.btnSend).setOnClickListener {
            val body = editMessage.text?.toString()?.trim() ?: return@setOnClickListener
            if (body.isEmpty()) return@setOnClickListener
            val mgr = syncManager
            if (mgr == null) {
                Toast.makeText(this, getString(R.string.mesh_start_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mgr.sendMessage(body)
            editMessage.text?.clear()
        }

        loadMessages()
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

    private fun loadMessages() {
        val dao = AppDatabase.getDatabase(this).meshMessageDao()
        CoroutineScope(Dispatchers.IO).launch {
            val messages = dao.getAllMessages()
            withContext(Dispatchers.Main) {
                adapter.submitList(messages)
                val recycler = findViewById<RecyclerView>(R.id.recyclerMessages)
                val txtEmpty = findViewById<View>(R.id.txtEmpty)
                if (messages.isEmpty()) {
                    recycler.visibility = View.GONE
                    txtEmpty.visibility = View.VISIBLE
                } else {
                    recycler.visibility = View.VISIBLE
                    txtEmpty.visibility = View.GONE
                }
            }
        }
    }

    private fun updateModeButtons(btnBle: MaterialButton, btnTcp: MaterialButton) {
        if (useBle) {
            btnBle.setBackgroundColor(getColor(R.color.accent))
            btnBle.setTextColor(getColor(R.color.white))
            btnTcp.setBackgroundColor(getColor(R.color.bg_input))
            btnTcp.setTextColor(getColor(R.color.text_secondary))
        } else {
            btnTcp.setBackgroundColor(getColor(R.color.accent))
            btnTcp.setTextColor(getColor(R.color.white))
            btnBle.setBackgroundColor(getColor(R.color.bg_input))
            btnBle.setTextColor(getColor(R.color.text_secondary))
        }
    }
}
