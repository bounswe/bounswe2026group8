package com.bounswe2026group8.emergencyhub.tutorial

import android.view.View
import android.view.ViewParent
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.TutorialActivity
import com.google.android.material.card.MaterialCardView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for [TutorialActivity].
 *
 * Covers the mobile guided-tour shell and representative tutorial flows:
 * guide navigation/restart, creating a tutorial forum post, and navigating from
 * help-request list into request detail. TutorialActivity is a programmatic UI,
 * so these instrumented tests are the meaningful mobile coverage for it.
 */
@RunWith(AndroidJUnit4::class)
class TutorialActivityTest : BaseInstrumentedTest() {

    @Test
    fun tutorialGuide_canAdvanceSkipAndRestart() {
        ActivityScenario.launch(TutorialActivity::class.java).use {
            onView(withText(R.string.emergency_hub)).check(matches(isDisplayed()))
            onView(withText(R.string.tutorial_dashboard_start_title)).check(matches(isDisplayed()))
            onView(withText(appString(R.string.tutorial_step_count, 1, 6))).check(matches(isDisplayed()))

            onView(withText(R.string.tutorial_next)).perform(click())
            onView(withText(R.string.tutorial_dashboard_hub_title)).check(matches(isDisplayed()))
            onView(withText(appString(R.string.tutorial_step_count, 2, 6))).check(matches(isDisplayed()))

            onView(withText(R.string.tutorial_skip_guide)).perform(click())
            onView(withText(R.string.tutorial_show_tutorial)).check(matches(isDisplayed()))

            onView(withText(R.string.tutorial_show_tutorial)).perform(click())
            onView(withText(R.string.tutorial_dashboard_start_title)).check(matches(isDisplayed()))
            onView(withText(appString(R.string.tutorial_step_count, 1, 6))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun forumTutorial_canCreateLocalPostAndReturnToForumList() {
        val newTitle = "Mobile tutorial unit test post"
        val newBody = "Neighbors can use the school entrance for charging phones."

        ActivityScenario.launch(TutorialActivity::class.java).use {
            onView(allOf(withText(R.string.feature_forum), isDisplayed()))
                .perform(clickNearestMaterialCard())

            onView(withText(R.string.forum_title)).check(matches(isDisplayed()))
            onView(withText(R.string.tutorial_forum_tabs_title)).check(matches(isDisplayed()))
            onView(withText(appString(R.string.tutorial_step_count, 1, 4))).check(matches(isDisplayed()))

            onView(withText(R.string.tutorial_new_post)).perform(scrollTo(), click())
            onView(withText(R.string.tutorial_create_post_title)).check(matches(isDisplayed()))
            onView(withText(R.string.tutorial_post_type_title)).check(matches(isDisplayed()))

            onView(allOf(isAssignableFrom(EditText::class.java), withText("Charging station open at the community center")))
                .perform(scrollTo(), replaceText(newTitle), closeSoftKeyboard())
            onView(allOf(isAssignableFrom(EditText::class.java), withText("Volunteers can help people charge phones until 18:00. Bring your own cable if possible.")))
                .perform(scrollTo(), replaceText(newBody), closeSoftKeyboard())

            onView(withText(R.string.tutorial_create_post_button)).perform(scrollTo(), click())

            onView(withText(R.string.forum_title)).check(matches(isDisplayed()))
            onView(withText(newTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun helpTutorial_canFilterMedicalRequestsAndOpenDetail() {
        ActivityScenario.launch(TutorialActivity::class.java).use {
            onView(allOf(withText(R.string.feature_help), isDisplayed()))
                .perform(clickNearestMaterialCard())

            onView(withText(R.string.help_center_title)).check(matches(isDisplayed()))
            onView(withText(R.string.tutorial_help_switch_title)).check(matches(isDisplayed()))
            onView(withText(appString(R.string.tutorial_step_count, 1, 4))).check(matches(isDisplayed()))

            onView(withText(R.string.tutorial_filter_medical)).perform(click())
            onView(withText("First aid kit needed near the bus stop"))
                .perform(scrollTo(), clickNearestMaterialCard())

            onView(withText(R.string.tutorial_help_detail_title)).check(matches(isDisplayed()))
            onView(withText(R.string.tutorial_review_request_title)).check(matches(isDisplayed()))
            onView(withText(containsString("Barbaros bus stop"))).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    private fun appString(resId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *args)

    private fun clickNearestMaterialCard(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription(): String = "click nearest MaterialCardView ancestor"

        override fun perform(uiController: UiController, view: View) {
            var parent: ViewParent? = view.parent
            while (parent != null && parent !is MaterialCardView) {
                parent = parent.parent
            }
            val card = parent as? MaterialCardView
                ?: throw AssertionError("No MaterialCardView ancestor found for ${view.javaClass.simpleName}")
            card.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }
}
