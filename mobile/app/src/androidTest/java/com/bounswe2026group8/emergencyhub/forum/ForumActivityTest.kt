package com.bounswe2026group8.emergencyhub.forum

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
import com.bounswe2026group8.emergencyhub.ui.ForumActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumented tests for [ForumActivity].
 *
 * Covers: forum list renders mocked posts, and tapping a post opens
 * [com.bounswe2026group8.emergencyhub.ui.PostDetailActivity].
 *
 * All HTTP calls are intercepted by MockWebServer via [BaseInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class ForumActivityTest : BaseInstrumentedTest() {

    // ── Forum list renders posts ───────────────────────────────────────────────────

    @Test
    fun forumList_rendersPosts() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path?.startsWith("/forum/posts/") == true ->
                    MockResponse().setResponseCode(200).setBody(POSTS_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(ForumActivity::class.java).use {
            onView(withText("Test Post")).check(matches(isDisplayed()))
        }
    }

    // ── Tapping a post opens PostDetailActivity ────────────────────────────────────

    @Test
    fun postClick_opensPostDetail() {
        injectToken("fake-jwt-token")

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                // Most specific first: comments endpoint
                request.path?.startsWith("/forum/posts/") == true &&
                        request.path?.contains("/comments/") == true ->
                    MockResponse().setResponseCode(200).setBody("[]")
                // Post detail (exact path, no query string)
                request.path == "/forum/posts/1/" ->
                    MockResponse().setResponseCode(200).setBody(POST_JSON)
                // Posts list (catches /forum/posts/?forum_type=...)
                request.path?.startsWith("/forum/posts/") == true ->
                    MockResponse().setResponseCode(200).setBody(POSTS_JSON)
                request.path == "/hubs/" ->
                    MockResponse().setResponseCode(200).setBody(HUBS_JSON)
                else ->
                    MockResponse().setResponseCode(200).setBody("{}")
            }
        }

        ActivityScenario.launch(ForumActivity::class.java).use {
            // Wait for list to load, then click the post title
            onView(withText("Test Post")).perform(click())

            // PostDetailActivity is now in the foreground
            onView(withId(R.id.txtPostTitle)).check(matches(isDisplayed()))
        }
    }

    // ── Shared response fixtures ──────────────────────────────────────────────────

    companion object {
        private const val HUBS_JSON =
            """[{"id":1,"name":"Istanbul","slug":"istanbul"}]"""
        private const val POSTS_JSON =
            """[{"id":1,"hub":null,"hub_name":null,"forum_type":"GLOBAL","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD"},"title":"Test Post","content":"Test content","image_urls":[],"status":"PUBLISHED","upvote_count":0,"downvote_count":0,"comment_count":1,"repost_count":0,"user_vote":null,"user_has_reposted":false,"reposted_from":null,"created_at":"2026-01-01T00:00:00Z"}]"""
        private const val POST_JSON =
            """{"id":1,"hub":null,"hub_name":null,"forum_type":"GLOBAL","author":{"id":1,"full_name":"Test User","email":"test@test.com","role":"STANDARD"},"title":"Test Post","content":"Test content","image_urls":[],"status":"PUBLISHED","upvote_count":0,"downvote_count":0,"comment_count":1,"repost_count":0,"user_vote":null,"user_has_reposted":false,"reposted_from":null,"created_at":"2026-01-01T00:00:00Z"}"""
    }
}
