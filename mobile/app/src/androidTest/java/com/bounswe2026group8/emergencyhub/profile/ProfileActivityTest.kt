package com.bounswe2026group8.emergencyhub.profile

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.api.UserData
import com.bounswe2026group8.emergencyhub.auth.TokenManager
import com.bounswe2026group8.emergencyhub.ui.ProfileActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [ProfileActivity].
 *
 * Covers: profile screen renders the logged-in user's name, email, and role badge.
 *
 * [ProfileActivity.loadIdentity] reads from [TokenManager] synchronously, so the
 * user object must be pre-seeded before the activity launches (not via a /me call).
 *
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class ProfileActivityTest : BaseInstrumentedTest() {

    // ── Profile screen renders user data ──────────────────────────────────────────

    @Test
    fun profile_rendersUserData() {
        injectToken("fake-jwt-token")

        // ProfileActivity reads identity from TokenManager cache synchronously.
        // Pre-seed the user object so name/email/role are available immediately.
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(ctx).saveUser(
            UserData(
                id = 1,
                fullName = "Test User",
                email = "test@test.com",
                role = "STANDARD",
                hub = null,
                neighborhoodAddress = null,
                expertiseField = null
            )
        )

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/me" ->
                    MockResponse().setResponseCode(200).setBody(USER_JSON)
                request.path == "/profile" ->
                    MockResponse().setResponseCode(200).setBody(PROFILE_JSON)
                request.path == "/resources" ->
                    MockResponse().setResponseCode(200).setBody("[]")
                request.path == "/expertise" ->
                    MockResponse().setResponseCode(200).setBody("[]")
                request.path?.startsWith("/expertise-categories/") == true ->
                    MockResponse().setResponseCode(200).setBody("[]")
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.txtFullName)).check(matches(isDisplayed()))
            onView(withId(R.id.txtEmail)).check(matches(isDisplayed()))
            onView(withId(R.id.txtRoleBadge)).check(matches(isDisplayed()))
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val USER_JSON =
            """{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null}"""
        private const val PROFILE_JSON =
            """{"phone_number":null,"blood_type":null,"emergency_contact_phone":null,"special_needs":null,"has_disability":false,"availability_status":"SAFE","bio":null,"preferred_language":"en","emergency_contact":null,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z"}"""
    }
}
