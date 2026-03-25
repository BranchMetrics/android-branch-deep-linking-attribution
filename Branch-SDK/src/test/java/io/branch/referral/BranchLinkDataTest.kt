package io.branch.referral

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Branch link data handling and validation.
 * Focuses on real-world scenarios and edge cases.
 */
class BranchLinkDataTest {
    
    @Test
    fun `test createLinkData with valid parameters`() {
        val linkData = createLinkData(
            title = "Test Link",
            description = "Test Description", 
            imageUrl = "https://example.com/image.jpg"
        )
        
        assertEquals("Should set correct title", "Test Link", linkData["title"])
        assertEquals("Should set correct description", "Test Description", linkData["description"])
        assertEquals("Should set correct imageUrl", "https://example.com/image.jpg", linkData["imageUrl"])
    }
    
    @Test
    fun `test createLinkData with null parameters`() {
        val linkData = createLinkData(
            title = null,
            description = null,
            imageUrl = null
        )
        
        // Should handle nulls gracefully
        assertTrue("Should create object even with null parameters", linkData is JSONObject)
        assertEquals("Should have empty title", "", linkData.optString("title", ""))
    }
    
    @Test
    fun `test createLinkData with empty strings`() {
        val linkData = createLinkData(
            title = "",
            description = "",
            imageUrl = ""
        )
        
        assertEquals("Should handle empty title", "", linkData["title"])
        assertEquals("Should handle empty description", "", linkData["description"])
        assertEquals("Should handle empty imageUrl", "", linkData["imageUrl"])
    }
    
    @Test
    fun `test createLinkData with special characters`() {
        val linkData = createLinkData(
            title = "Test Link with émojis 🔗",
            description = "Description with \"quotes\" and 'apostrophes'",
            imageUrl = "https://example.com/image with spaces.jpg"
        )
        
        assertEquals("Should handle émojis in title", "Test Link with émojis 🔗", linkData["title"])
        assertEquals("Should handle quotes in description", "Description with \"quotes\" and 'apostrophes'", linkData["description"])
        assertEquals("Should handle spaces in URL", "https://example.com/image with spaces.jpg", linkData["imageUrl"])
    }
    
    @Test
    fun `test createLinkData with very long strings`() {
        val longTitle = "A".repeat(1000)
        val longDescription = "B".repeat(5000)
        val longUrl = "https://example.com/" + "path/".repeat(100)
        
        val linkData = createLinkData(
            title = longTitle,
            description = longDescription,
            imageUrl = longUrl
        )
        
        assertEquals("Should handle long title", longTitle, linkData["title"])
        assertEquals("Should handle long description", longDescription, linkData["description"])
        assertEquals("Should handle long URL", longUrl, linkData["imageUrl"])
    }
    
    @Test
    fun `test validateLinkData with valid data`() {
        val validData = JSONObject().apply {
            put("title", "Valid Title")
            put("description", "Valid Description")
            put("imageUrl", "https://valid.url/image.jpg")
        }
        
        assertTrue("Should validate correct link data", validateLinkData(validData))
    }
    
    @Test
    fun `test validateLinkData with invalid data`() {
        // Test with malformed URL
        val invalidUrlData = JSONObject().apply {
            put("title", "Title")
            put("imageUrl", "not-a-valid-url")
        }
        
        assertFalse("Should reject malformed URL", validateLinkData(invalidUrlData))
        
        // Test with empty required fields
        val emptyData = JSONObject()
        assertFalse("Should reject empty data", validateLinkData(emptyData))
    }
    
    @Test
    fun `test parseLinkDataFromJson with valid JSON`() {
        val jsonString = """
            {
                "title": "Parsed Title",
                "description": "Parsed Description",
                "imageUrl": "https://parsed.url/image.jpg",
                "customData": {
                    "key": "value"
                }
            }
        """.trimIndent()
        
        val linkData = parseLinkDataFromJson(jsonString)
        
        assertNotNull("Should parse valid JSON", linkData)
        assertEquals("Should extract title", "Parsed Title", linkData?.optString("title"))
        assertEquals("Should extract description", "Parsed Description", linkData?.optString("description"))
        assertNotNull("Should extract nested object", linkData?.optJSONObject("customData"))
    }
    
    @Test
    fun `test parseLinkDataFromJson with invalid JSON`() {
        // Malformed JSON
        assertNull("Should handle malformed JSON", parseLinkDataFromJson("{invalid json}"))
        
        // Empty string
        assertNull("Should handle empty string", parseLinkDataFromJson(""))
        
        // Null input
        assertNull("Should handle null input", parseLinkDataFromJson(null))
        
        // Non-JSON string
        assertNull("Should handle plain text", parseLinkDataFromJson("just plain text"))
    }
    
    @Test
    fun `test mergeLinkData with overlapping keys`() {
        val base = JSONObject().apply {
            put("title", "Base Title")
            put("description", "Base Description")
            put("existingKey", "baseValue")
        }
        
        val overlay = JSONObject().apply {
            put("title", "Overlay Title") // This should override
            put("newKey", "newValue") // This should be added
        }
        
        val merged = mergeLinkData(base, overlay)
        
        assertEquals("Should override with overlay title", "Overlay Title", merged.optString("title"))
        assertEquals("Should keep base description", "Base Description", merged.optString("description"))
        assertEquals("Should keep existing key", "baseValue", merged.optString("existingKey"))
        assertEquals("Should add new key", "newValue", merged.optString("newKey"))
    }
    
    @Test
    fun `test mergeLinkData with null inputs`() {
        val validData = JSONObject().apply { put("key", "value") }
        
        // Test with null base
        val result1 = mergeLinkData(null, validData)
        assertEquals("Should handle null base", validData.toString(), result1.toString())
        
        // Test with null overlay
        val result2 = mergeLinkData(validData, null)
        assertEquals("Should handle null overlay", validData.toString(), result2.toString())
        
        // Test with both null
        val result3 = mergeLinkData(null, null)
        assertEquals("Should handle both null", 0, result3.length())
    }
    
    // Helper methods that simulate real Branch SDK functionality
    private fun createLinkData(title: String?, description: String?, imageUrl: String?): JSONObject {
        return JSONObject().apply {
            if (title != null) put("title", title)
            if (description != null) put("description", description)
            if (imageUrl != null) put("imageUrl", imageUrl)
        }
    }
    
    private fun validateLinkData(data: JSONObject?): Boolean {
        if (data == null) return false
        
        // Validate URL if present
        val imageUrl = data.optString("imageUrl", null)
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            try {
                java.net.URL(imageUrl)
            } catch (e: Exception) {
                return false
            }
        }
        
        // Must have at least title or description
        val title = data.optString("title", "")
        val description = data.optString("description", "")
        return title.isNotEmpty() || description.isNotEmpty()
    }
    
    private fun parseLinkDataFromJson(jsonString: String?): JSONObject? {
        if (jsonString.isNullOrEmpty()) return null
        return try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun mergeLinkData(base: JSONObject?, overlay: JSONObject?): JSONObject {
        val result = JSONObject()
        
        // Add base data
        base?.let { baseObj ->
            baseObj.keys().forEach { key ->
                result.put(key, baseObj.get(key))
            }
        }
        
        // Add/override with overlay data
        overlay?.let { overlayObj ->
            overlayObj.keys().forEach { key ->
                result.put(key, overlayObj.get(key))
            }
        }
        
        return result
    }
}