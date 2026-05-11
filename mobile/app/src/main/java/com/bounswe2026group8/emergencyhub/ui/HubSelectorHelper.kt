package com.bounswe2026group8.emergencyhub.ui

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.Hub
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import kotlinx.coroutines.launch

/**
 * Read-only top-right hub indicator. Refreshes the user payload from `/me`
 * so the label tracks any hub change the user just made in Settings.
 *
 * Callers may also pass [onHubSelected] to react to the resolved hub (used by
 * Forum / HelpRequests for filtering their lists by the user's hub).
 */
class HubSelectorHelper(
    private val activity: AppCompatActivity,
    private val display: TextView,
    private val onHubSelected: ((Hub?) -> Unit)? = null,
) {
    private val tokenManager = TokenManager(activity)
    private var currentHub: Hub? = null

    fun load() {
        // Show whatever is cached locally first so the UI doesn't flicker empty.
        val cachedHub = tokenManager.getUser()?.hub
        renderAndNotify(cachedHub)

        activity.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getService(activity).getMe()
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        tokenManager.saveUser(user)
                        renderAndNotify(user.hub)
                    }
                }
            } catch (_: Exception) {
                // Keep cached value on network failure.
            }
        }
    }

    fun getSelectedHub(): Hub? = currentHub

    private fun renderAndNotify(hub: Hub?) {
        currentHub = hub
        display.text = formatHubLabel(hub)
        onHubSelected?.invoke(hub)
    }

    private fun formatHubLabel(hub: Hub?): String {
        if (hub == null) return activity.getString(R.string.hub_none)
        val parts = listOfNotNull(hub.district, hub.city, hub.country).filter { it.isNotBlank() }
        return if (parts.isEmpty()) hub.name else parts.joinToString(", ")
    }
}
