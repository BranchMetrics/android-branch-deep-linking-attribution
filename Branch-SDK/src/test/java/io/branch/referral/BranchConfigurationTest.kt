package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Simple tests for Branch configuration validation and handling.
 * These tests focus on core logic without Android dependencies.
 */
class BranchConfigurationTest {
    
    @Test
    fun `test branch key validation`() {
        assertTrue("Should accept valid live key", isValidBranchKey("key_live_abcdefghijklmnop"))
        assertTrue("Should accept valid test key", isValidBranchKey("key_test_abcdefghijklmnop"))
        
        assertFalse("Should reject null key", isValidBranchKey(null))
        assertFalse("Should reject empty key", isValidBranchKey(""))
        assertFalse("Should reject short key", isValidBranchKey("key_live_abc"))
        assertFalse("Should reject wrong prefix", isValidBranchKey("wrong_live_abcdefghijklmnop"))
        assertFalse("Should reject missing underscore", isValidBranchKey("keyliveabcdefghijklmnop"))
    }
    
    @Test
    fun `test configuration parameter validation`() {
        assertTrue("Should accept valid timeout", isValidTimeout(5000))
        assertTrue("Should accept zero timeout", isValidTimeout(0))
        
        assertFalse("Should reject negative timeout", isValidTimeout(-1000))
        assertFalse("Should reject extremely large timeout", isValidTimeout(Integer.MAX_VALUE))
        
        assertTrue("Should accept valid retry count", isValidRetryCount(3))
        assertTrue("Should accept zero retries", isValidRetryCount(0))
        
        assertFalse("Should reject negative retries", isValidRetryCount(-1))
        assertFalse("Should reject excessive retries", isValidRetryCount(100))
    }
    
    @Test
    fun `test URL validation for deep links`() {
        assertTrue("Should accept HTTPS URL", isValidDeepLinkUrl("https://example.com/path"))
        assertTrue("Should accept HTTP URL", isValidDeepLinkUrl("http://example.com/path"))
        assertTrue("Should accept custom scheme", isValidDeepLinkUrl("myapp://open?param=value"))
        
        assertFalse("Should reject malformed URL", isValidDeepLinkUrl("not-a-url"))
        assertFalse("Should reject empty URL", isValidDeepLinkUrl(""))
        assertFalse("Should reject null URL", isValidDeepLinkUrl(null))
        assertFalse("Should reject file URL", isValidDeepLinkUrl("file:///path/to/file"))
    }
    
    @Test
    fun `test environment detection`() {
        assertTrue("Should detect test environment", isTestEnvironment("key_test_abcdefghijklmnop"))
        assertFalse("Should detect live environment", isTestEnvironment("key_live_abcdefghijklmnop"))
        assertFalse("Should handle invalid key", isTestEnvironment("invalid_key"))
        assertFalse("Should handle null key", isTestEnvironment(null))
    }
    
    @Test
    fun `test configuration merging`() {
        val baseConfig = mapOf(
            "timeout" to 5000,
            "retries" to 3,
            "debug" to false
        )
        
        val override = mapOf(
            "timeout" to 10000,
            "debug" to true,
            "new_setting" to "value"
        )
        
        val merged = mergeConfigurations(baseConfig, override)
        
        assertEquals("Should override timeout", 10000, merged["timeout"])
        assertEquals("Should keep retries from base", 3, merged["retries"])
        assertEquals("Should override debug", true, merged["debug"])
        assertEquals("Should add new setting", "value", merged["new_setting"])
    }
    
    @Test
    fun `test configuration merging with null inputs`() {
        val config = mapOf("key" to "value")
        
        val merged1 = mergeConfigurations(null, config)
        assertEquals("Should handle null base", config, merged1)
        
        val merged2 = mergeConfigurations(config, null)
        assertEquals("Should handle null override", config, merged2)
        
        val merged3 = mergeConfigurations(null, null)
        assertTrue("Should handle both null", merged3.isEmpty())
    }
    
    @Test
    fun `test configuration sanitization`() {
        val unsafeConfig = mapOf(
            "api_key" to "secret_key_123",
            "user_token" to "user_token_456",
            "safe_setting" to "safe_value",
            "password" to "user_password"
        )
        
        val sanitized = sanitizeConfiguration(unsafeConfig)
        
        assertEquals("Should preserve safe setting", "safe_value", sanitized["safe_setting"])
        assertNotEquals("Should sanitize API key", "secret_key_123", sanitized["api_key"])
        assertNotEquals("Should sanitize password", "user_password", sanitized["password"])
        assertTrue("Should mark sensitive data", sanitized["api_key"].toString().contains("[REDACTED]"))
    }
    
    @Test
    fun `test configuration validation edge cases`() {
        // Test with extreme values
        assertFalse("Should reject negative timeout", isValidTimeout(-1))
        assertFalse("Should reject timeout too large", isValidTimeout(Int.MAX_VALUE))
        
        // Test with boundary values
        assertTrue("Should accept minimum valid timeout", isValidTimeout(100))
        assertTrue("Should accept maximum valid timeout", isValidTimeout(300000)) // 5 minutes
        
        // Test retry count boundaries using actual SDK constants
        assertTrue("Should accept minimum retries", isValidRetryCount(0))
        assertTrue("Should accept SDK max retries", isValidRetryCount(PrefHelper.MAX_RETRIES))
        assertFalse("Should reject excessive retries", isValidRetryCount(PrefHelper.MAX_RETRIES + 1))
    }
    
    // Helper methods that simulate Branch configuration logic
    private fun isValidBranchKey(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        if (key.length < 20) return false
        return key.startsWith("key_live_") || key.startsWith("key_test_")
    }
    
    private fun isValidTimeout(timeout: Int): Boolean {
        return timeout >= 0 && timeout <= 300000 // 0 to 5 minutes
    }
    
    private fun isValidRetryCount(retries: Int): Boolean {
        // Use actual SDK constant to centralize logic and avoid hardcoded values
        return retries >= 0 && retries <= PrefHelper.MAX_RETRIES
    }
    
    private fun isValidDeepLinkUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (url.startsWith("file://")) return false
        
        return try {
            val parsedUrl = java.net.URL(url)
            parsedUrl.protocol in listOf("http", "https", "myapp", "custom")
        } catch (e: Exception) {
            // Try as URI for custom schemes
            try {
                val uri = java.net.URI(url)
                uri.scheme != null
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    private fun isTestEnvironment(branchKey: String?): Boolean {
        return branchKey?.startsWith("key_test_") == true
    }
    
    private fun mergeConfigurations(base: Map<String, Any>?, override: Map<String, Any>?): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        base?.let { result.putAll(it) }
        override?.let { result.putAll(it) }
        
        return result
    }
    
    private fun sanitizeConfiguration(config: Map<String, Any>?): Map<String, Any> {
        if (config == null) return emptyMap()
        
        val sensitiveKeys = setOf("api_key", "user_token", "password", "secret", "token")
        val result = mutableMapOf<String, Any>()
        
        config.forEach { (key, value) ->
            if (sensitiveKeys.any { key.lowercase().contains(it) }) {
                result[key] = "[REDACTED]"
            } else {
                result[key] = value
            }
        }
        
        return result
    }
}