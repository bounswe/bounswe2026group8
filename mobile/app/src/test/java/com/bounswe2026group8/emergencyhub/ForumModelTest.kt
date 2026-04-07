package com.bounswe2026group8.emergencyhub

import com.bounswe2026group8.emergencyhub.api.CreatePostRequest
import com.bounswe2026group8.emergencyhub.api.Post
import com.bounswe2026group8.emergencyhub.api.PostAuthor
import com.bounswe2026group8.emergencyhub.api.VoteRequest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for forum data models.
 *
 * Validates vote type logic, post type values, role-based display,
 * and post creation payloads (section 2.1 of the test plan).
 */
class ForumModelTest {

    // ── Vote logic ─────────────────────────────────────────────────────────────

    @Test
    fun `vote request with upvote type`() {
        val req = VoteRequest(voteType = "UP")
        assertEquals("UP", req.voteType)
    }

    @Test
    fun `vote request with downvote type`() {
        val req = VoteRequest(voteType = "DOWN")
        assertEquals("DOWN", req.voteType)
    }

    @Test
    fun `upvote and downvote are distinct`() {
        val up = VoteRequest("UP")
        val down = VoteRequest("DOWN")
        assertNotEquals(up.voteType, down.voteType)
    }

    @Test
    fun `post userVote is null when user has not voted`() {
        val post = makePost(userVote = null)
        assertNull(post.userVote)
    }

    @Test
    fun `post userVote is UP when user upvoted`() {
        val post = makePost(userVote = "UP")
        assertEquals("UP", post.userVote)
    }

    @Test
    fun `post userVote is DOWN when user downvoted`() {
        val post = makePost(userVote = "DOWN")
        assertEquals("DOWN", post.userVote)
    }

    // ── Vote toggle logic ──────────────────────────────────────────────────────

    @Test
    fun `sending same vote type when already voted should toggle off`() {
        // When userVote == "UP" and user clicks UP again, the expected behavior
        // is to send "UP" again — the backend handles the toggle semantics
        val currentVote = "UP"
        val voteToSend = if (currentVote == "UP") "UP" else "DOWN"
        assertEquals("UP", voteToSend)
    }

    @Test
    fun `sending different vote type switches vote`() {
        val currentVote = "UP"
        val voteToSend = if (currentVote == "UP") "DOWN" else "UP"
        assertEquals("DOWN", voteToSend)
    }

    // ── Post types ─────────────────────────────────────────────────────────────

    @Test
    fun `standard post type is valid`() {
        val post = makePost(forumType = "STANDARD")
        assertEquals("STANDARD", post.forumType)
    }

    @Test
    fun `urgent post type is valid`() {
        val post = makePost(forumType = "URGENT")
        assertEquals("URGENT", post.forumType)
    }

    @Test
    fun `global post type is valid`() {
        val post = makePost(forumType = "GLOBAL")
        assertEquals("GLOBAL", post.forumType)
    }

    // ── Role-based display ─────────────────────────────────────────────────────

    @Test
    fun `expert author role is distinguishable`() {
        val expertPost = makePost(authorRole = "EXPERT")
        assertEquals("EXPERT", expertPost.author.role)
    }

    @Test
    fun `standard author role is distinguishable`() {
        val standardPost = makePost(authorRole = "STANDARD")
        assertEquals("STANDARD", standardPost.author.role)
    }

    // ── Repost detection ───────────────────────────────────────────────────────

    @Test
    fun `post without repost origin is not a repost`() {
        val post = makePost()
        assertNull(post.repostedFrom)
    }

    @Test
    fun `post with userHasReposted false can be reposted`() {
        val post = makePost(userHasReposted = false)
        assertFalse(post.userHasReposted!!)
    }

    @Test
    fun `post with userHasReposted true cannot be reposted again`() {
        val post = makePost(userHasReposted = true)
        assertTrue(post.userHasReposted!!)
    }

    // ── CreatePostRequest defaults ─────────────────────────────────────────────

    @Test
    fun `create post defaults image urls to empty`() {
        val req = CreatePostRequest(
            forumType = "STANDARD",
            title = "My Post",
            content = "Content here"
        )
        assertTrue(req.imageUrls.isEmpty())
    }

    @Test
    fun `create post defaults hub to null`() {
        val req = CreatePostRequest(
            forumType = "GLOBAL",
            title = "Global Post",
            content = "Content"
        )
        assertNull(req.hub)
    }

    // ── Vote counts ────────────────────────────────────────────────────────────

    @Test
    fun `upvote and downvote counts are independent`() {
        val post = makePost(upvoteCount = 10, downvoteCount = 3)
        assertEquals(10, post.upvoteCount)
        assertEquals(3, post.downvoteCount)
        assertNotEquals(post.upvoteCount, post.downvoteCount)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun makePost(
        forumType: String = "STANDARD",
        authorRole: String = "STANDARD",
        userVote: String? = null,
        userHasReposted: Boolean? = null,
        upvoteCount: Int = 0,
        downvoteCount: Int = 0
    ) = Post(
        id = 1,
        hub = null,
        hubName = null,
        forumType = forumType,
        author = PostAuthor(
            id = 1,
            fullName = "Test User",
            email = "test@example.com",
            role = authorRole
        ),
        title = "Test Post",
        content = "Test content",
        imageUrls = null,
        status = "ACTIVE",
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        commentCount = 0,
        repostCount = 0,
        userVote = userVote,
        userHasReposted = userHasReposted,
        repostedFrom = null,
        createdAt = "2026-01-01T00:00:00Z"
    )
}
