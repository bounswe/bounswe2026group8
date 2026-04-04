package com.bounswe2026group8.emergencyhub.ui

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.HubManager
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

class HubSelectorHelper(
    private val activity: AppCompatActivity,
    private val spinner: Spinner,
    private val onHubSelected: ((Hub?) -> Unit)? = null
) {
    private val hubManager = HubManager(activity)
    private var hubs: List<Hub> = emptyList()
    private var initialSetupDone = false

    fun load() {
        activity.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(activity).getHubs()
                if (response.isSuccessful) {
                    hubs = response.body() ?: emptyList()
                    setupSpinner()
                }
            } catch (_: Exception) { }
        }
    }

    fun getSelectedHub(): Hub? {
        val pos = spinner.selectedItemPosition
        return if (pos in hubs.indices) hubs[pos] else null
    }

    private fun setupSpinner() {
        val hubNames = hubs.map { it.name }
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, hubNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Priority: manually saved hub > user's registered hub > istanbul fallback
        val savedHub = hubManager.getSelectedHub()
        val userHub = TokenManager(activity).getUser()?.hub
        val resolvedHub = savedHub ?: userHub
        val resolvedIdx = if (resolvedHub != null) hubs.indexOfFirst { it.id == resolvedHub.id } else -1

        val selectedIdx = if (resolvedIdx >= 0) {
            resolvedIdx
        } else {
            val istanbulIdx = hubs.indexOfFirst { it.slug == "istanbul" }
            if (istanbulIdx >= 0) istanbulIdx else 0
        }

        spinner.setSelection(selectedIdx)

        // Notify the callback with the initial hub (without saving — it's already persisted)
        if (selectedIdx in hubs.indices) {
            onHubSelected?.invoke(hubs[selectedIdx])
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (!initialSetupDone) {
                    initialSetupDone = true
                    return
                }
                if (pos in hubs.indices) {
                    val hub = hubs[pos]
                    hubManager.saveSelectedHub(hub)
                    onHubSelected?.invoke(hub)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                onHubSelected?.invoke(null)
            }
        }
    }
}
