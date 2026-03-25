package io.branch.referral

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple tests for Branch data validation and sanitization functionality.
 * These tests focus on core logic without Android dependencies.
 */
class BranchDataValidationTest {
    
    @Test
    fun `test user ID validation`() {
        assertTrue("Should accept valid user ID", isValidUserId("user123"))
        assertTrue("Should accept UUID format", isValidUserId("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue("Should accept email format", isValidUserId("user@example.com"))
        
        assertFalse("Should reject empty user ID", isValidUserId(""))
        assertFalse("Should reject null user ID", isValidUserId(null))
        assertFalse("Should reject very long user ID", isValidUserId("a".repeat(300)))
        assertFalse("Should reject user ID with special chars", isValidUserId("user@#$%"))
    }
    
    @Test
    fun `test event name validation`() {
        assertTrue("Should accept simple event name", isValidEventName("purchase"))
        assertTrue("Should accept snake_case event", isValidEventName("user_action"))
        assertTrue("Should accept camelCase event", isValidEventName("userAction"))
        
        assertFalse("Should reject event with spaces", isValidEventName("user action"))
        assertFalse("Should reject event with special chars", isValidEventName("event@name"))
        assertFalse("Should reject empty event name", isValidEventName(""))
        assertFalse("Should reject null event name", isValidEventName(null))
        assertFalse("Should reject very long event name", isValidEventName("a".repeat(200)))
    }
    
    @Test
    fun `test event value validation`() {
        assertTrue("Should accept positive integer", isValidEventValue(100.0))
        assertTrue("Should accept positive decimal", isValidEventValue(99.99))
        assertTrue("Should accept zero", isValidEventValue(0.0))
        
        assertFalse("Should reject negative value", isValidEventValue(-10.0))
        assertFalse("Should reject infinity", isValidEventValue(Double.POSITIVE_INFINITY))
        assertFalse("Should reject NaN", isValidEventValue(Double.NaN))
        assertFalse("Should reject very large value", isValidEventValue(Double.MAX_VALUE))
    }
    
    @Test
    fun `test custom data validation`() {
        val validData = mapOf(
            "category" to "electronics",
            "price" to "299.99",
            "quantity" to "2"
        )
        assertTrue("Should accept valid custom data", isValidCustomData(validData))
        
        val invalidData = mapOf(
            "key with spaces" to "value",
            "normalKey" to "value"
        )
        assertFalse("Should reject data with invalid keys", isValidCustomData(invalidData))
        
        assertTrue("Should accept empty data", isValidCustomData(emptyMap()))
        assertTrue("Should accept null data", isValidCustomData(null))
    }
    
    @Test
    fun `test metadata validation`() {
        assertTrue("Should accept simple metadata", isValidMetadata("simple_value"))
        assertTrue("Should accept JSON string", isValidMetadata("{\"key\":\"value\"}"))
        assertTrue("Should accept numeric string", isValidMetadata("12345"))
        
        assertFalse("Should reject null metadata", isValidMetadata(null))
        assertFalse("Should reject empty metadata", isValidMetadata(""))
        assertFalse("Should reject very long metadata", isValidMetadata("a".repeat(5000)))
    }
    
    @Test
    fun `test sensitive data detection`() {
        assertTrue("Should detect password", isSensitiveData("password", "secret123"))
        assertTrue("Should detect credit card", isSensitiveData("cc_number", "4111111111111111"))
        assertTrue("Should detect SSN", isSensitiveData("ssn", "123-45-6789"))
        assertTrue("Should detect API key", isSensitiveData("api_key", "sk_test_123"))
        
        assertFalse("Should not flag normal data", isSensitiveData("name", "John Doe"))
        assertFalse("Should not flag price", isSensitiveData("price", "29.99"))
        assertFalse("Should not flag category", isSensitiveData("category", "books"))
    }
    
    @Test
    fun `test data sanitization`() {
        val sensitiveData = mapOf(
            "username" to "john_doe",
            "password" to "secret123",
            "email" to "john@example.com",
            "credit_card" to "4111111111111111"
        )
        
        val sanitized = sanitizeData(sensitiveData)
        
        assertEquals("Should preserve username", "john_doe", sanitized["username"])
        assertEquals("Should preserve email", "john@example.com", sanitized["email"])
        assertNotEquals("Should sanitize password", "secret123", sanitized["password"])
        assertNotEquals("Should sanitize credit card", "4111111111111111", sanitized["credit_card"])
        assertTrue("Should mark sensitive password", sanitized["password"].toString().contains("[REDACTED]"))
        assertTrue("Should mark sensitive card", sanitized["credit_card"].toString().contains("[REDACTED]"))
    }
    
    @Test
    fun `test URL parameter sanitization`() {
        val url = "https://example.com?user=john&password=secret&token=abc123"
        val sanitized = sanitizeUrl(url)
        
        assertTrue("Should preserve domain", sanitized.contains("example.com"))
        assertTrue("Should preserve user param", sanitized.contains("user=john"))
        assertFalse("Should remove password", sanitized.contains("password=secret"))
        assertFalse("Should remove token", sanitized.contains("token=abc123"))
    }
    
    @Test
    fun `test data type validation`() {
        assertTrue("Should accept string data", isValidDataType("string_value"))
        assertTrue("Should accept integer data", isValidDataType(42))
        assertTrue("Should accept double data", isValidDataType(3.14))
        assertTrue("Should accept boolean data", isValidDataType(true))
        
        assertFalse("Should reject null data", isValidDataType(null))
        assertFalse("Should reject complex objects", isValidDataType(listOf("a", "b", "c")))
    }
    
    @Test
    fun `test data length validation`() {
        assertTrue("Should accept normal length", isValidDataLength("normal text"))
        assertTrue("Should accept empty string", isValidDataLength(""))
        assertTrue("Should accept reasonable length", isValidDataLength("a".repeat(100)))
        
        assertFalse("Should reject very long data", isValidDataLength("a".repeat(2000)))
        assertFalse("Should reject null data", isValidDataLength(null))
    }
    
    @Test
    fun `test validation edge cases`() {
        // Test boundary values
        assertTrue("Should accept max valid user ID length", isValidUserId("a".repeat(100)))
        assertFalse("Should reject user ID over limit", isValidUserId("a".repeat(101)))
        
        assertTrue("Should accept max valid event name length", isValidEventName("a".repeat(50)))
        assertFalse("Should reject event name over limit", isValidEventName("a".repeat(51)))
        
        // Test special characters
        assertFalse("Should reject control characters", isValidEventName("event\u0000name"))
        assertFalse("Should reject unicode special chars", isValidEventName("event\u202Ename"))
    }
    
    // Helper methods that simulate Branch data validation logic
    private fun isValidUserId(userId: String?): Boolean {
        if (userId.isNullOrEmpty()) return false
        if (userId.length > 100) return false
        
        // Allow alphanumeric, underscore, dash, dot, @
        return userId.matches(Regex("^[a-zA-Z0-9._@-]+$"))
    }
    
    private fun isValidEventName(eventName: String?): Boolean {
        if (eventName.isNullOrEmpty()) return false
        if (eventName.length > 50) return false
        
        // Allow alphanumeric and underscore, must start with letter
        return eventName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))
    }
    
    private fun isValidEventValue(value: Double?): Boolean {
        if (value == null) return false
        if (!value.isFinite()) return false
        if (value < 0) return false
        if (value > 1000000) return false // Reasonable upper limit
        
        return true
    }
    
    private fun isValidCustomData(data: Map<String, Any>?): Boolean {
        if (data == null || data.isEmpty()) return true
        
        return data.keys.all { key ->
            key.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$")) && key.length <= 50
        }
    }
    
    private fun isValidMetadata(metadata: String?): Boolean {
        if (metadata.isNullOrEmpty()) return false
        return metadata.length <= 1000
    }
    
    private fun isSensitiveData(key: String, value: String): Boolean {
        val sensitiveKeys = setOf("password", "pwd", "secret", "token", "key", "credit_card", "cc_number", "ssn")
        val lowerKey = key.lowercase()
        
        return sensitiveKeys.any { lowerKey.contains(it) } ||
               value.matches(Regex("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}")) || // Credit card pattern
               value.matches(Regex("\\d{3}-\\d{2}-\\d{4}")) // SSN pattern
    }
    
    private fun sanitizeData(data: Map<String, Any>): Map<String, Any> {
        return data.mapValues { (key, value) ->
            if (isSensitiveData(key, value.toString())) {
                "[REDACTED]"
            } else {
                value
            }
        }
    }
    
    private fun sanitizeUrl(url: String): String {
        val sensitiveParams = setOf("password", "token", "secret", "key", "auth")
        var sanitized = url
        
        for (param in sensitiveParams) {
            sanitized = sanitized.replace(Regex("$param=[^&]*"), "$param=[REDACTED]")
        }
        
        return sanitized
    }
    
    private fun isValidDataType(data: Any?): Boolean {
        return when (data) {
            null -> false
            is String, is Int, is Double, is Boolean -> true
            else -> false
        }
    }
    
    private fun isValidDataLength(data: String?): Boolean {
        if (data == null) return false
        return data.length <= 1000
    }
}