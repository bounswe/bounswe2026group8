package com.bounswe2026group8.emergencyhub.helprequest

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.HelpRequestListActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [HelpRequestListActivity].
 *
 * Covers: help request list renders mocked requests, and tapping a request
 * opens [com.bounswe2026group8.emergencyhub.ui.HelpRequestDetailActivity].
 *
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class HelpRequestListActivityTest : BaseInstrumentedTest() {

    // ── Help request list renders mocked items ─────────────────────────────────────

    @Test
    fun helpRequestList_rendersMockedRequests() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path?.startsWith("/help-requests/") == true ->
                    MockResponse().setResponseCode(200).setBody(HELP_REQUESTS_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("[]")
            }
        }

        ActivityScenario.launch(HelpRequestListActivity::class.java).use {
            onView(withText("Need insulin")).check(matches(isDisplayed()))
        }
    }

    // ── Tapping a request opens HelpRequestDetailActivity ─────────────────────────

    @Test
    fun requestClick_opensDetailActivity() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                // Most specific first: comments for detail
                request.path?.startsWith("/help-requests/") == true &&
                        request.path?.contains("/comments/") == true ->
                    MockResponse().setResponseCode(200).setBody("[]")
                // Detail endpoint (exact path for id=1)
                request.path == "/help-requests/1/" ->
                    MockResponse().setResponseCode(200).setBody(HELP_REQUEST_DETAIL_JSON)
                // List endpoint (catches /help-requests/?... query params)
                request.path?.startsWith("/help-requests/") == true ->
                    MockResponse().setResponseCode(200).setBody(HELP_REQUESTS_JSON)
                // Return 404 for hubs so HubSelectorHelper skips setupSpinner().
                // If hubs load successfully, setupSpinner() calls onHubSelected which
                // triggers a second fetchHelpRequests() → showLoading() hides the
                // RecyclerView → Espresso's click dispatches to (0,0) and navigation
                // never fires. A 404 prevents that second call entirely.
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(404).setBody("{}")
                else ->
                    MockResponse().setResponseCode(200).setBody("[]")
            }
        }

        ActivityScenario.launch(HelpRequestListActivity::class.java).use {
            // Wait for the list to load, then click the card (click listener is on
            // the card container, not the title text view)
            onView(withText("Need insulin")).check(matches(isDisplayed()))
            onView(withId(R.id.cardHelpRequest)).perform(click())

            // HelpRequestDetailActivity is now in the foreground
            onView(withId(R.id.txtDetailTitle)).check(matches(isDisplayed()))
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val HELP_REQUESTS_JSON =
            """[{"id":1,"hub":null,"hub_name":null,"category":"MEDICAL","urgency":"HIGH","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null},"title":"Need insulin","status":"OPEN","comment_count":0,"created_at":"2026-01-01T00:00:00Z"}]"""
        private const val HELP_REQUEST_DETAIL_JSON =
            """{"id":1,"hub":null,"hub_name":null,"category":"MEDICAL","urgency":"HIGH","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null},"title":"Need insulin","description":"I need insulin urgently","image_urls":[],"latitude":null,"longitude":null,"location_text":null,"status":"OPEN","comment_count":0,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}"""
    }
}
