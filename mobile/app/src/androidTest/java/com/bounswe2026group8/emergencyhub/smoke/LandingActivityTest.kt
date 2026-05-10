package com.bounswe2026group8.emergencyhub.smoke

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.ui.LandingActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: verifies the Espresso + emulator setup is functional.
 * The activity is launched inside the test body so MockWebServer is already
 * configured (via BaseInstrumentedTest.setUp) before any lifecycle callbacks fire.
 */
@RunWith(AndroidJUnit4::class)
class LandingActivityTest : BaseInstrumentedTest() {

    @Test
    fun landingActivityLaunches() {
        ActivityScenario.launch(LandingActivity::class.java).use {
            onView(withId(android.R.id.content)).check(matches(isDisplayed()))
        }
    }
}
