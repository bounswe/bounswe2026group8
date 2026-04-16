package com.bounswe2026group8.emergencyhub.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.bounswe2026group8.emergencyhub.api.RetrofitClient

/**
 * Base Activity that listens for 401 Unauthorized broadcasts from the
 * OkHttp interceptor and redirects to [LandingActivity].
 *
 * All authenticated Activities should extend this instead of AppCompatActivity
 * so that expired-token handling is automatic.
 */
open class BaseActivity : AppCompatActivity() {

    private val unauthorizedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == RetrofitClient.ACTION_UNAUTHORIZED) {
                navigateToLanding()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(RetrofitClient.ACTION_UNAUTHORIZED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unauthorizedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unauthorizedReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(unauthorizedReceiver)
    }

    protected fun navigateToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
