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
import kotlinx.coroutines.launch

class HubSelectorHelper(
    private val activity: AppCompatActivity,
    private val spinner: Spinner,
    private val onHubSelected: ((Hub?) -> Unit)? = null
) {
    private val hubManager = HubManager(activity)
    private var hubs: List<Hub> = emptyList()

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

        val savedHub = hubManager.getSelectedHub()
        val savedIdx = if (savedHub != null) hubs.indexOfFirst { it.id == savedHub.id } else -1

        if (savedIdx >= 0) {
            spinner.setSelection(savedIdx)
        } else {
            val istanbulIdx = hubs.indexOfFirst { it.slug == "istanbul" }
            if (istanbulIdx >= 0) spinner.setSelection(istanbulIdx)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
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
