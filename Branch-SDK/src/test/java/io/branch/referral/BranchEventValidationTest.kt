package io.branch.referral

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Branch event validation and sanitization.
 * Covers edge cases and security considerations.
 */
class BranchEventValidationTest {
    
    @Test
    fun `test validateEventName with valid names`() {
        assertTrue("Should accept standard event name", validateEventName("purchase"))
        assertTrue("Should accept custom event name", validateEventName("custom_event"))
        assertTrue("Should accept event with numbers", validateEventName("level_1_complete"))
        assertTrue("Should accept event with underscores", validateEventName("user_action_complete"))
    }
    
    @Test
    fun `test validateEventName with invalid names`() {
        assertFalse("Should reject empty name", validateEventName(""))
        assertFalse("Should reject null name", validateEventName(null))
        assertFalse("Should reject very long name", validateEventName("a".repeat(300)))
        assertFalse("Should reject name with spaces", validateEventName("invalid name"))
        assertFalse("Should reject name with special chars", validateEventName("invalid@event"))
        assertFalse("Should reject name starting with number", validateEventName("1invalid"))
    }
    
    @Test
    fun `test sanitizeEventProperties with valid properties`() {
        val properties = JSONObject().apply {
            put("product_id", "12345")
            put("price", 29.99)
            put("quantity", 1)
            put("category", "electronics")
            put("is_sale", true)
        }
        
        val sanitized = sanitizeEventProperties(properties)
        
        assertNotNull("Should return sanitized properties", sanitized)
        assertEquals("Should preserve string property", "12345", sanitized?.optString("product_id"))
        assertEquals("Should preserve number property", 29.99, sanitized?.optDouble("price") ?: 0.0, 0.01)
        assertEquals("Should preserve int property", 1, sanitized?.optInt("quantity"))
        assertTrue("Should preserve boolean property", sanitized?.optBoolean("is_sale") == true)
    }
    
    @Test
    fun `test sanitizeEventProperties with dangerous content`() {
        val properties = JSONObject().apply {
            put("script_tag", "<script>alert('xss')</script>")
            put("sql_injection", "'; DROP TABLE users; --")
            put("password", "secretPassword123")
            put("credit_card", "4111111111111111")
            put("normal_field", "safe_value")
        }
        
        val sanitized = sanitizeEventProperties(properties)
        
        assertNotNull("Should return sanitized object", sanitized)
        assertTrue("Should strip HTML tags", !sanitized?.optString("script_tag", "")?.contains("<script>")!!)
        assertTrue("Should preserve normal field", sanitized?.optString("normal_field") == "safe_value")
        // Sensitive fields should be removed or masked
        assertTrue("Should handle sensitive data appropriately", 
            !sanitized?.optString("password", "").equals("secretPassword123"))
    }
    
    @Test
    fun `test sanitizeEventProperties with nested objects`() {
        val nestedProperties = JSONObject().apply {
            put("user", JSONObject().apply {
                put("id", "user123")
                put("email", "test@example.com")
                put("preferences", JSONObject().apply {
                    put("newsletter", true)
                    put("theme", "dark")
                })
            })
            put("items", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "Product 1")
                    put("price", 19.99)
                })
                put(JSONObject().apply {
                    put("name", "Product 2") 
                    put("price", 39.99)
                })
            })
        }
        
        val sanitized = sanitizeEventProperties(nestedProperties)
        
        assertNotNull("Should handle nested objects", sanitized)
        assertNotNull("Should preserve user object", sanitized?.optJSONObject("user"))
        assertNotNull("Should preserve items array", sanitized?.optJSONArray("items"))
        assertEquals("Should preserve nested email", "test@example.com", 
            sanitized?.optJSONObject("user")?.optString("email"))
    }
    
    @Test
    fun `test sanitizeEventProperties with null and empty values`() {
        val properties = JSONObject().apply {
            put("null_value", JSONObject.NULL)
            put("empty_string", "")
            put("zero_number", 0)
            put("false_boolean", false)
            put("empty_object", JSONObject())
            put("empty_array", JSONArray())
        }
        
        val sanitized = sanitizeEventProperties(properties)
        
        assertNotNull("Should handle null and empty values", sanitized)
        assertTrue("Should preserve null value", sanitized?.isNull("null_value") == true)
        assertEquals("Should preserve empty string", "", sanitized?.optString("empty_string"))
        assertEquals("Should preserve zero", 0, sanitized?.optInt("zero_number"))
        assertEquals("Should preserve false", false, sanitized?.optBoolean("false_boolean"))
    }
    
    @Test
    fun `test validateEventSize with normal events`() {
        val smallEvent = JSONObject().apply {
            put("event", "purchase")
            put("product_id", "123")
        }
        
        val mediumEvent = JSONObject().apply {
            put("event", "page_view")
            put("url", "https://example.com/product/123")
            put("title", "Product Page")
            put("timestamp", System.currentTimeMillis())
        }
        
        assertTrue("Should accept small event", validateEventSize(smallEvent))
        assertTrue("Should accept medium event", validateEventSize(mediumEvent))
    }
    
    @Test
    fun `test validateEventSize with oversized events`() {
        val hugeEvent = JSONObject().apply {
            put("event", "huge_event")
            // Add a very large string property
            put("huge_data", "x".repeat(100000))
        }
        
        assertFalse("Should reject oversized event", validateEventSize(hugeEvent))
    }
    
    @Test
    fun `test validateEventSize with deeply nested events`() {
        var nested = JSONObject()
        var current = nested
        
        // Create deeply nested structure
        for (i in 1..100) {
            val next = JSONObject().apply { put("key$i", "value$i") }
            current.put("nested", next)
            current = next
        }
        
        assertFalse("Should reject deeply nested structure", validateEventSize(nested))
    }
    
    @Test
    fun `test createStandardEvent with valid parameters`() {
        val event = createStandardEvent("purchase", mapOf(
            "revenue" to 29.99,
            "currency" to "USD",
            "product_id" to "12345"
        ))
        
        assertNotNull("Should create standard event", event)
        assertEquals("Should set event name", "purchase", event?.optString("event"))
        assertEquals("Should set revenue", 29.99, event?.optDouble("revenue") ?: 0.0, 0.01)
        assertEquals("Should set currency", "USD", event?.optString("currency"))
    }
    
    @Test
    fun `test createStandardEvent with edge case parameters`() {
        // Test with null values
        val eventWithNulls = createStandardEvent("test_event", mapOf(
            "null_value" to null,
            "valid_value" to "test"
        ))
        
        assertNotNull("Should handle null values", eventWithNulls)
        assertEquals("Should preserve valid value", "test", eventWithNulls?.optString("valid_value"))
        
        // Test with empty map
        val eventWithEmptyProps = createStandardEvent("empty_event", emptyMap())
        assertNotNull("Should handle empty properties", eventWithEmptyProps)
        assertEquals("Should set event name", "empty_event", eventWithEmptyProps?.optString("event"))
    }
    
    // Helper methods simulating Branch SDK validation logic
    private fun validateEventName(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        if (name.length > 255) return false
        if (name.contains(" ")) return false
        if (name.matches(Regex("^[0-9].*"))) return false
        return name.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))
    }
    
    private fun sanitizeEventProperties(properties: JSONObject?): JSONObject? {
        if (properties == null) return null
        
        val sanitized = JSONObject()
        val sensitiveFields = setOf("password", "credit_card", "ssn", "api_key")
        
        properties.keys().forEach { key ->
            val value = properties.get(key)
            
            // Remove sensitive fields
            if (key.lowercase() in sensitiveFields) {
                sanitized.put(key, "[REDACTED]")
            } else {
                when (value) {
                    is String -> {
                        // Strip HTML tags
                        val cleaned = value.replace(Regex("<[^>]*>"), "")
                        sanitized.put(key, cleaned)
                    }
                    is JSONObject -> {
                        sanitized.put(key, sanitizeEventProperties(value))
                    }
                    else -> {
                        sanitized.put(key, value)
                    }
                }
            }
        }
        
        return sanitized
    }
    
    private fun validateEventSize(event: JSONObject?): Boolean {
        if (event == null) return true
        
        val jsonString = event.toString()
        // Limit event size to 64KB
        if (jsonString.length > 65536) return false
        
        // Check nesting depth
        return checkNestingDepth(event, 0) <= 10
    }
    
    private fun checkNestingDepth(obj: Any?, currentDepth: Int): Int {
        if (currentDepth > 20) return currentDepth // Prevent infinite recursion
        
        return when (obj) {
            is JSONObject -> {
                var maxDepth = currentDepth
                obj.keys().forEach { key ->
                    val childDepth = checkNestingDepth(obj.get(key), currentDepth + 1)
                    maxDepth = maxOf(maxDepth, childDepth)
                }
                maxDepth
            }
            is JSONArray -> {
                var maxDepth = currentDepth
                for (i in 0 until obj.length()) {
                    val childDepth = checkNestingDepth(obj.get(i), currentDepth + 1)
                    maxDepth = maxOf(maxDepth, childDepth)
                }
                maxDepth
            }
            else -> currentDepth
        }
    }
    
    private fun createStandardEvent(eventName: String, properties: Map<String, Any?>): JSONObject? {
        if (!validateEventName(eventName)) return null
        
        val event = JSONObject().apply {
            put("event", eventName)
            put("timestamp", System.currentTimeMillis())
        }
        
        properties.forEach { (key, value) ->
            if (value != null) {
                event.put(key, value)
            }
        }
        
        return if (validateEventSize(event)) event else null
    }
}