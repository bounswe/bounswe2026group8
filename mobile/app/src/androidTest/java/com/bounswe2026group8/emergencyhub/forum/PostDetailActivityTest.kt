package com.bounswe2026group8.emergencyhub.forum

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bounswe2026group8.emergencyhub.BaseInstrumentedTest
import com.bounswe2026group8.emergencyhub.R
import com.bounswe2026group8.emergencyhub.ui.PostDetailActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Espresso instrumented tests for [PostDetailActivity].
 *
 * Covers: post detail screen renders content and comments, and comment
 * submission reaches the backend.
 *
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class PostDetailActivityTest : BaseInstrumentedTest() {

    // ── Post detail renders content and comments ───────────────────────────────────

    @Test
    fun postDetail_rendersContent() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path?.startsWith("/forum/posts/") == true &&
                        request.path?.contains("/comments/") == true ->
                    MockResponse().setResponseCode(200).setBody(COMMENTS_JSON)
                request.path == "/forum/posts/1/" ->
                    MockResponse().setResponseCode(200).setBody(POST_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, PostDetailActivity::class.java).putExtra("post_id", 1)
        ActivityScenario.launch<PostDetailActivity>(intent).use {
            onView(withId(R.id.txtPostTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.txtPostContent)).check(matches(isDisplayed()))
            onView(withId(R.id.txtPostAuthor)).check(matches(isDisplayed()))
            // Comment loaded from mock
            onView(withText("Great post")).check(matches(isDisplayed()))
        }
    }

    // ── Comment submission sends POST to backend ──────────────────────────────────

    @Test
    fun commentSubmission_sendsRequest() {
        injectToken("fake-jwt-token")

        val commentLatch = CountDownLatch(1)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path?.startsWith("/forum/posts/") == true &&
                        request.path?.contains("/comments/") == true &&
                        request.method == "POST" -> {
                    commentLatch.countDown()
                    MockResponse().setResponseCode(201).setBody(NEW_COMMENT_JSON)
                }
                request.path?.startsWith("/forum/posts/") == true &&
                        request.path?.contains("/comments/") == true ->
                    MockResponse().setResponseCode(200).setBody("[]")
                request.path == "/forum/posts/1/" ->
                    MockResponse().setResponseCode(200).setBody(POST_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, PostDetailActivity::class.java).putExtra("post_id", 1)
        ActivityScenario.launch<PostDetailActivity>(intent).use {
            onView(withId(R.id.inputComment))
                .perform(replaceText("My comment"), closeSoftKeyboard())
            onView(withId(R.id.btnPostComment)).perform(click())

            assertTrue(
                "Expected POST /forum/posts/1/comments/ to be called",
                commentLatch.await(5, TimeUnit.SECONDS)
            )
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val POST_JSON =
            """{"id":1,"hub":null,"hub_name":null,"forum_type":"GLOBAL","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD"},"title":"Test Post","content":"Test content","image_urls":[],"status":"PUBLISHED","upvote_count":0,"downvote_count":0,"comment_count":1,"repost_count":0,"user_vote":null,"user_has_reposted":false,"reposted_from":null,"created_at":"2026-01-01T00:00:00Z"}"""
        private const val COMMENTS_JSON =
            """[{"id":1,"post":1,"author":{"id":2,"full_name":"Other User","email":"other@test.com","role":"STANDARD"},"content":"Great post","created_at":"2026-01-01T00:01:00Z"}]"""
        private const val NEW_COMMENT_JSON =
            """{"id":2,"post":1,"author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD"},"content":"My comment","created_at":"2026-01-01T00:02:00Z"}"""
    }
}
