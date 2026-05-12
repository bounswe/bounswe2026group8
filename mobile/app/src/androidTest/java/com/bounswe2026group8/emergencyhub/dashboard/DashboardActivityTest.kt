package com.bounswe2026group8.emergencyhub.dashboard

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.DashboardActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [DashboardActivity].
 *
 * Covers: dashboard renders core navigation elements and the hub display text.
 *
 * [injectToken] simulates a logged-in user. A [Dispatcher] routes all
 * async calls (fetchMe, hub load, FCM token, mesh sync) to MockWebServer.
 * POST_NOTIFICATIONS is pre-granted in BaseInstrumentedTest.setUp() for API 33+.
 */
@RunWith(AndroidJUnit4::class)
class DashboardActivityTest : BaseInstrumentedTest() {

    // ── Dashboard renders navigation cards ────────────────────────────────────────

    @Test
    fun dashboard_rendersNavigationCards() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/me"    -> MockResponse().setResponseCode(200).setBody(USER_JSON)
                request.path == "/hubs/" -> MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else                     -> MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(DashboardActivity::class.java).use {
            onView(withId(R.id.txtWelcome)).check(matches(isDisplayed()))
            onView(withId(R.id.cardForum)).check(matches(isDisplayed()))
            onView(withId(R.id.cardHelpRequests)).check(matches(isDisplayed()))
            onView(withId(R.id.cardProfile)).check(matches(isDisplayed()))
            onView(withId(R.id.cardOfflineInfo)).check(matches(isDisplayed()))
            onView(withId(R.id.textHubDisplay)).check(matches(isDisplayed()))
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val USER_JSON =
            """{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null}"""
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
    }
}
