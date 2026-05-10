package com.bounswe2026group8.emergencyhub.helprequest

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.HelpRequestDetailActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [HelpRequestDetailActivity].
 *
 * Covers: help request detail screen renders title, category, status,
 * and description from a mocked backend response.
 *
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class HelpRequestDetailActivityTest : BaseInstrumentedTest() {

    // ── Help request detail renders all fields ─────────────────────────────────────

    @Test
    fun helpRequestDetail_rendersContent() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path?.startsWith("/help-requests/") == true &&
                        request.path?.contains("/comments/") == true ->
                    MockResponse().setResponseCode(200).setBody("[]")
                request.path == "/help-requests/1/" ->
                    MockResponse().setResponseCode(200).setBody(HELP_REQUEST_DETAIL_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, HelpRequestDetailActivity::class.java)
            .putExtra(HelpRequestDetailActivity.EXTRA_REQUEST_ID, 1)
        ActivityScenario.launch<HelpRequestDetailActivity>(intent).use {
            onView(withId(R.id.txtDetailTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.txtDetailCategory)).check(matches(isDisplayed()))
            onView(withId(R.id.txtDetailStatus)).check(matches(isDisplayed()))
            onView(withId(R.id.txtDetailDescription)).check(matches(isDisplayed()))
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val HELP_REQUEST_DETAIL_JSON =
            """{"id":1,"hub":null,"hub_name":null,"category":"MEDICAL","urgency":"HIGH","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null},"title":"Need insulin","description":"I need insulin urgently","image_urls":[],"latitude":null,"longitude":null,"location_text":null,"status":"OPEN","comment_count":0,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}"""
    }
}
