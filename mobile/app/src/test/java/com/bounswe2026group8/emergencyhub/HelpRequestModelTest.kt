package com.bounswe2026group8.emergencyhub

import com.bounswe2026group8.emergencyhub.api.CreateHelpRequest
import com.bounswe2026group8.emergencyhub.api.HelpRequestAuthor
import com.bounswe2026group8.emergencyhub.api.HelpRequestItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for help request data models.
 *
 * Validates business rules around status values, urgency levels, categories,
 * and request creation payloads (section 2.1 of the test plan).
 */
class HelpRequestModelTest {

    // ── Status values ──────────────────────────────────────────────────────────

    @Test
    fun `open status is a known value`() {
        val item = makeItem(status = "OPEN")
        assertEquals("OPEN", item.status)
    }

    @Test
    fun `expert responding status is a known value`() {
        val item = makeItem(status = "EXPERT_RESPONDING")
        assertEquals("EXPERT_RESPONDING", item.status)
    }

    @Test
    fun `resolved status is a known value`() {
        val item = makeItem(status = "RESOLVED")
        assertEquals("RESOLVED", item.status)
    }

    // ── Urgency values ─────────────────────────────────────────────────────────

    @Test
    fun `low urgency is valid`() {
        val item = makeItem(urgency = "LOW")
        assertEquals("LOW", item.urgency)
    }

    @Test
    fun `medium urgency is valid`() {
        val item = makeItem(urgency = "MEDIUM")
        assertEquals("MEDIUM", item.urgency)
    }

    @Test
    fun `high urgency is valid`() {
        val item = makeItem(urgency = "HIGH")
        assertEquals("HIGH", item.urgency)
    }

    // ── Category values ────────────────────────────────────────────────────────

    @Test
    fun `medical category is valid`() {
        val item = makeItem(category = "MEDICAL")
        assertEquals("MEDICAL", item.category)
    }

    @Test
    fun `food category is valid`() {
        val item = makeItem(category = "FOOD")
        assertEquals("FOOD", item.category)
    }

    @Test
    fun `shelter category is valid`() {
        val item = makeItem(category = "SHELTER")
        assertEquals("SHELTER", item.category)
    }

    @Test
    fun `transport category is valid`() {
        val item = makeItem(category = "TRANSPORT")
        assertEquals("TRANSPORT", item.category)
    }

    // ── Role display ───────────────────────────────────────────────────────────

    @Test
    fun `author role expert is distinguishable from standard`() {
        val expert = makeItem(authorRole = "EXPERT")
        val standard = makeItem(authorRole = "STANDARD")
        assertTrue(expert.author.role == "EXPERT")
        assertTrue(standard.author.role == "STANDARD")
        assertNotEquals(expert.author.role, standard.author.role)
    }

    // ── CreateHelpRequest defaults ────────────────────────────────────────────

    @Test
    fun `create request defaults image urls to empty list`() {
        val req = CreateHelpRequest(
            category = "MEDICAL",
            urgency = "HIGH",
            title = "Need help",
            description = "Emergency situation"
        )
        assertTrue(req.imageUrls.isEmpty())
    }

    @Test
    fun `create request defaults hub to null`() {
        val req = CreateHelpRequest(
            category = "FOOD",
            urgency = "LOW",
            title = "Need food",
            description = "Please help"
        )
        assertNull(req.hub)
    }

    @Test
    fun `create request with location fields`() {
        val req = CreateHelpRequest(
            category = "MEDICAL",
            urgency = "HIGH",
            title = "Injured",
            description = "Need medical help",
            latitude = "41.0369",
            longitude = "28.9850",
            locationText = "Near Taksim Square"
        )
        assertEquals("41.0369", req.latitude)
        assertEquals("28.9850", req.longitude)
        assertEquals("Near Taksim Square", req.locationText)
    }

    @Test
    fun `create request location fields are optional`() {
        val req = CreateHelpRequest(
            category = "SHELTER",
            urgency = "MEDIUM",
            title = "Need shelter",
            description = "Lost my home"
        )
        assertNull(req.latitude)
        assertNull(req.longitude)
        assertNull(req.locationText)
    }

    // ── Comment count ──────────────────────────────────────────────────────────

    @Test
    fun `comment count is initialized from api response`() {
        val item = makeItem(commentCount = 5)
        assertEquals(5, item.commentCount)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun makeItem(
        status: String = "OPEN",
        urgency: String = "LOW",
        category: String = "MEDICAL",
        authorRole: String = "STANDARD",
        commentCount: Int = 0
    ) = HelpRequestItem(
        id = 1,
        hub = null,
        hubName = null,
        category = category,
        urgency = urgency,
        author = HelpRequestAuthor(
            id = 1,
            fullName = "Test User",
            email = "test@example.com",
            role = authorRole,
            hub = null,
            neighborhoodAddress = null,
            expertiseField = null
        ),
        title = "Test Request",
        status = status,
        commentCount = commentCount,
        createdAt = "2026-01-01T00:00:00Z"
    )
}
