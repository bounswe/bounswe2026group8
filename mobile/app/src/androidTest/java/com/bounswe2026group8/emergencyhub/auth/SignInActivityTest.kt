package com.bounswe2026group8.emergencyhub.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.SignInActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [SignInActivity].
 *
 * Covers TC-AUTH-002: valid login, invalid credentials, and empty-field validation.
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 * POST_NOTIFICATIONS is pre-granted in BaseInstrumentedTest.setUp() for API 33+.
 */
@RunWith(AndroidJUnit4::class)
class SignInActivityTest : BaseInstrumentedTest() {

    // ── TC-AUTH-002a: valid login ─────────────────────────────────────────────────

    @Test
    fun validCredentials_navigatesToDashboard() {
        // DashboardActivity makes several async calls (fetchMe, FCM token, hub load,
        // mesh sync) — route all paths via Dispatcher to avoid exhausted-queue errors
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/login" ->
                    MockResponse().setResponseCode(200).setBody(LOGIN_SUCCESS_JSON)
                request.path == "/me" ->
                    MockResponse().setResponseCode(200).setBody(USER_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(SignInActivity::class.java)

        onView(withId(R.id.inputEmail))
            .perform(replaceText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.inputPassword))
            .perform(replaceText("password123"), closeSoftKeyboard())
        onView(withId(R.id.btnSignIn)).perform(click())

        // DashboardActivity takes over — verify its welcome text is visible
        onView(withId(R.id.txtWelcome)).check(matches(isDisplayed()))
    }

    // ── TC-AUTH-002b: invalid credentials ────────────────────────────────────────

    @Test
    fun invalidCredentials_staysOnSignIn() {
        // Only one request expected (POST /login → 400); Dashboard never launches
        mockWebServer.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"message":"Invalid email or password"}""")
        )

        ActivityScenario.launch(SignInActivity::class.java)

        onView(withId(R.id.inputEmail))
            .perform(replaceText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.inputPassword))
            .perform(replaceText("wrongpassword"), closeSoftKeyboard())
        onView(withId(R.id.btnSignIn)).perform(click())

        // Still on SignInActivity — email field and re-enabled button are visible
        onView(withId(R.id.inputEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSignIn)).check(matches(isEnabled()))
    }

    // ── TC-AUTH-002c: empty-field validation ─────────────────────────────────────

    @Test
    fun emptyFields_noRequestSent() {
        ActivityScenario.launch(SignInActivity::class.java)

        // Click sign-in with both fields empty
        onView(withId(R.id.btnSignIn)).perform(click())

        // Validation is client-side — no network call should be made
        assertEquals(0, mockWebServer.requestCount)

        // Still on SignInActivity
        onView(withId(R.id.inputEmail)).check(matches(isDisplayed()))
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val LOGIN_SUCCESS_JSON = """{"message":"Login successful","token":"fake-jwt","refresh":"fake-refresh","user":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null}}"""
        private const val USER_JSON = """{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD","hub":null,"neighborhood_address":null,"expertise_field":null}"""
        private const val HUBS_JSON = """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
    }
}
