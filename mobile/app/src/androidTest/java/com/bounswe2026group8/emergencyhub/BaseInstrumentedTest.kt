package com.bounswe2026group8.emergencyhub

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
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(ctx).clear()

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
