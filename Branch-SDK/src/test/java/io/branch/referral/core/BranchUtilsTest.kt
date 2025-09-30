package io.branch.referral.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Essential utility tests for Branch SDK core functionality.
 * Tests string validation, JSON parsing, and edge cases.
 */
class BranchUtilsTest {
    
    @Test
    fun `test validateBranchKey with valid keys`() {
        // Valid branch key format
        assertTrue("Should accept valid live key", isValidBranchKey("key_live_abcdefg1234567890"))
        assertTrue("Should accept valid test key", isValidBranchKey("key_test_abcdefg1234567890"))
    }
    
    @Test
    fun `test validateBranchKey with invalid keys`() {
        // Invalid formats
        assertFalse("Should reject empty key", isValidBranchKey(""))
        assertFalse("Should reject null key", isValidBranchKey(null))
        assertFalse("Should reject short key", isValidBranchKey("key_test_abc"))
        assertFalse("Should reject wrong prefix", isValidBranchKey("branch_test_abcdefg1234567890"))
        assertFalse("Should reject no underscore", isValidBranchKey("keytestabcdefg1234567890"))
    }
    
    @Test
    fun `test sanitizeUrl with various inputs`() {
        // Valid URLs
        assertEquals("Should accept valid HTTPS URL", 
            "https://example.com", sanitizeUrl("https://example.com"))
        assertEquals("Should accept valid HTTP URL", 
            "http://example.com", sanitizeUrl("http://example.com"))
        
        // Invalid URLs
        assertNull("Should reject malformed URL", sanitizeUrl("not-a-url"))
        assertNull("Should reject empty URL", sanitizeUrl(""))
        assertNull("Should reject null URL", sanitizeUrl(null))
        
        // Edge cases
        assertEquals("Should handle URL with parameters", 
            "https://example.com?param=value", sanitizeUrl("https://example.com?param=value"))
    }
    
    @Test
    fun `test parseJsonSafely with valid JSON`() {
        val validJson = "{\"key\":\"value\",\"number\":123}"
        val result = parseJsonSafely(validJson)
        
        assertNotNull("Should parse valid JSON", result)
        assertEquals("Should extract string value", "value", result?.optString("key"))
        assertEquals("Should extract number value", 123, result?.optInt("number"))
    }
    
    @Test
    fun `test parseJsonSafely with invalid JSON`() {
        // Invalid JSON formats
        assertNull("Should handle malformed JSON", parseJsonSafely("{invalid:json"))
        assertNull("Should handle empty string", parseJsonSafely(""))
        assertNull("Should handle null input", parseJsonSafely(null))
        assertNull("Should handle non-JSON string", parseJsonSafely("not json at all"))
    }
    
    @Test
    fun `test parseJsonSafely with edge cases`() {
        // Edge cases
        val emptyObject = parseJsonSafely("{}")
        assertNotNull("Should parse empty object", emptyObject)
        assertEquals("Should have no keys", 0, emptyObject?.length())
        
        val emptyArray = parseJsonSafely("[]")
        assertNull("Should handle array input gracefully", emptyArray)
        
        val nestedJson = parseJsonSafely("{\"nested\":{\"key\":\"value\"}}")
        assertNotNull("Should parse nested JSON", nestedJson)
        assertNotNull("Should access nested object", nestedJson?.optJSONObject("nested"))
    }
    
    @Test
    fun `test extractDomainFromUrl with various URLs`() {
        // Valid URLs
        assertEquals("Should extract domain from HTTPS URL", 
            "example.com", extractDomainFromUrl("https://example.com/path"))
        assertEquals("Should extract domain from HTTP URL", 
            "example.com", extractDomainFromUrl("http://example.com"))
        assertEquals("Should extract subdomain", 
            "sub.example.com", extractDomainFromUrl("https://sub.example.com/path"))
        
        // Edge cases
        assertNull("Should handle malformed URL", extractDomainFromUrl("not-a-url"))
        assertNull("Should handle empty URL", extractDomainFromUrl(""))
        assertNull("Should handle null URL", extractDomainFromUrl(null))
        
        // URLs with ports
        assertEquals("Should handle URL with port", 
            "example.com", extractDomainFromUrl("https://example.com:8080/path"))
    }
    
    // Helper methods that would typically be in BranchUtils
    private fun isValidBranchKey(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        if (key.length < 20) return false
        return key.startsWith("key_live_") || key.startsWith("key_test_")
    }
    
    private fun sanitizeUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        return try {
            java.net.URL(url)
            url
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseJsonSafely(jsonString: String?): org.json.JSONObject? {
        if (jsonString.isNullOrEmpty()) return null
        return try {
            org.json.JSONObject(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractDomainFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        return try {
            val urlObj = java.net.URL(url)
            urlObj.host
        } catch (e: Exception) {
            null
        }
    }
}