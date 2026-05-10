package com.bounswe2026group8.emergencyhub.dashboard

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import org.hamcrest.Matchers.anything
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Espresso instrumented tests for [DashboardActivity].
 *
 * Covers: dashboard renders core navigation elements, and hub selector
 * is populated and triggers a backend update on selection.
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
            onView(withId(R.id.spinnerHubSelector)).check(matches(isDisplayed()))
        }
    }

    // ── Hub selector is populated and selection triggers backend update ────────────

    @Test
    fun hubSelector_populatedAndSelectionTriggersUpdate() {
        injectToken("fake-jwt-token")

        // CountDownLatch lets us block until the PATCH /me coroutine fires
        val patchLatch = CountDownLatch(1)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/me" && request.method == "GET" ->
                    MockResponse().setResponseCode(200).setBody(USER_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(TWO_HUBS_JSON)
                request.path == "/me" && request.method == "PATCH" -> {
                    patchLatch.countDown()
                    MockResponse().setResponseCode(200).setBody(USER_ANKARA_JSON)
                }
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(DashboardActivity::class.java).use {
            // Hub spinner must be visible and populated by HubSelectorHelper.load()
            onView(withId(R.id.spinnerHubSelector)).check(matches(isDisplayed()))

            // Select the second hub (Ankara, index 1) — triggers PATCH /me
            onView(withId(R.id.spinnerHubSelector)).perform(click())
            onData(anything()).atPosition(1).perform(click())

            // Verify the PATCH reached MockWebServer within 5 seconds
            assertTrue(
                "Expected PATCH /me to be called after hub selection",
                patchLatch.await(5, TimeUnit.SECONDS)
            )
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val USER_JSON =
            """{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null}"""
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val TWO_HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"},{"id":2,"name":"Ankara","slug":"ankara"}]"""
        private const val USER_ANKARA_JSON =
            """{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":{"id":2,"name":"Ankara","slug":"ankara"},"neighborhood_address":null,"expertise_field":null}"""
    }
}
