package com.bounswe2026group8.emergencyhub

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.bounswe2026group8.emergencyhub.api.RetrofitClient
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

/**
 * Base class for all instrumented tests.
 *
 * Sets up a [MockWebServer] and redirects [RetrofitClient] to it so tests never
 * hit the real backend. Auth state is cleared before each test so tests start
 * from a logged-out state by default. Call [injectToken] to simulate a logged-in user.
 */
abstract class BaseInstrumentedTest {

    protected lateinit var mockWebServer: MockWebServer

    @Before
    open fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        TokenManager(ctx).clear()

        // Delete the Room DB so stale schemas from previous installs never cause
        // "no such table" crashes on any developer's machine or CI runner.
        ctx.deleteDatabase("offline_contacts_database")

        // POST_NOTIFICATIONS is a runtime permission only on API 33+. Grant it via
        // shell before any activity launches so the system dialog never steals
        // Espresso's window focus. On API 30 the permission doesn't exist, so skip.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation
                .executeShellCommand("pm grant ${ctx.packageName} android.permission.POST_NOTIFICATIONS")
                .close()
        }

        // Revoke location permissions so DashboardActivity.preCacheOfflineData()
        // takes the no-permission branch and returns immediately. Without this,
        // getCurrentLocation() on an emulator without mock GPS configured registers
        // a callback that never fires, causing Espresso to wait indefinitely.
        instrumentation.uiAutomation
            .executeShellCommand("pm revoke ${ctx.packageName} android.permission.ACCESS_FINE_LOCATION")
            .close()
        instrumentation.uiAutomation
            .executeShellCommand("pm revoke ${ctx.packageName} android.permission.ACCESS_COARSE_LOCATION")
            .close()

        mockWebServer = MockWebServer()
        mockWebServer.start()
        RetrofitClient.testBaseUrl = mockWebServer.url("/").toString()
        RetrofitClient.reset()
    }

    @After
    open fun tearDown() {
        mockWebServer.shutdown()
        RetrofitClient.testBaseUrl = null
        RetrofitClient.reset()
    }

    /** Injects a fake auth token so requests include an Authorization header. */
    protected fun injectToken(token: String) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(ctx).saveToken(token)
    }
}
