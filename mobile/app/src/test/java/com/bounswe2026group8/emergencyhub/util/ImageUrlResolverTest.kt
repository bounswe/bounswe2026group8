package com.bounswe2026group8.emergencyhub.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ImageUrlResolver].
 *
 * Verifies that relative media paths from the backend are resolved correctly
 * and that external URLs are never modified.
 */
class ImageUrlResolverTest {

    private val base = "http://10.0.2.2:8000/"

    @Test
    fun `relative path gets base url prepended`() {
        val result = ImageUrlResolver.resolve("/media/uploads/photo.png", base)
        assertEquals("http://10.0.2.2:8000/media/uploads/photo.png", result)
    }

    @Test
    fun `absolute http url is returned unchanged`() {
        val url = "http://example.com/image.jpg"
        assertEquals(url, ImageUrlResolver.resolve(url, base))
    }

    @Test
    fun `absolute https url is returned unchanged`() {
        val url = "https://cdn.example.com/photo.png"
        assertEquals(url, ImageUrlResolver.resolve(url, base))
    }

    @Test
    fun `base url trailing slash is not duplicated`() {
        val result = ImageUrlResolver.resolve("/media/test.jpg", "http://10.0.2.2:8000/")
        assertEquals("http://10.0.2.2:8000/media/test.jpg", result)
    }

    @Test
    fun `base url without trailing slash still works`() {
        val result = ImageUrlResolver.resolve("/media/test.jpg", "http://10.0.2.2:8000")
        assertEquals("http://10.0.2.2:8000/media/test.jpg", result)
    }

    @Test
    fun `production base url strips api prefix for media paths`() {
        val result = ImageUrlResolver.resolve(
            "/media/uploads/doc.pdf",
            "https://emergencyhub.duckdns.org/api/"
        )
        assertEquals("https://emergencyhub.duckdns.org/media/uploads/doc.pdf", result)
    }
}
