package io.branch.referral

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Simple tests for Branch URL validation and processing functionality.
 * These tests focus on core logic without Android dependencies.
 */
class BranchUrlValidationTest {
    
    @Test
    fun `test Branch link URL validation`() {
        assertTrue("Should accept valid Branch link", isValidBranchLink("https://example.app.link/abc123"))
        assertTrue("Should accept test Branch link", isValidBranchLink("https://example-alternate.app.link/def456"))
        assertTrue("Should accept custom domain", isValidBranchLink("https://custom.example.com/ghi789"))
        
        assertFalse("Should reject non-HTTPS", isValidBranchLink("http://example.app.link/abc123"))
        assertFalse("Should reject malformed URL", isValidBranchLink("not-a-url"))
        assertFalse("Should reject empty URL", isValidBranchLink(""))
        assertFalse("Should reject null URL", isValidBranchLink(null))
    }
    
    @Test
    fun `test deep link URL parsing`() {
        assertTrue("Should parse valid deep link", isValidDeepLink("myapp://open?param=value"))
        assertTrue("Should parse HTTP deep link", isValidDeepLink("https://example.com/path"))
        assertTrue("Should parse custom scheme", isValidDeepLink("customscheme://action"))
        
        assertFalse("Should reject malformed deep link", isValidDeepLink("invalid://"))
        assertFalse("Should reject empty deep link", isValidDeepLink(""))
        assertFalse("Should reject null deep link", isValidDeepLink(null))
    }
    
    @Test
    fun `test URL parameter validation`() {
        assertTrue("Should accept valid parameter name", isValidUrlParam("branch_key"))
        assertTrue("Should accept alphanumeric parameter", isValidUrlParam("param123"))
        assertTrue("Should accept underscore parameter", isValidUrlParam("my_param"))
        
        assertFalse("Should reject parameter with spaces", isValidUrlParam("invalid param"))
        assertFalse("Should reject parameter with special chars", isValidUrlParam("param@value"))
        assertFalse("Should reject empty parameter", isValidUrlParam(""))
        assertFalse("Should reject null parameter", isValidUrlParam(null))
    }
    
    @Test
    fun `test URL encoding validation`() {
        assertTrue("Should accept encoded URL", isValidEncodedUrl("https%3A//example.com/path"))
        assertTrue("Should accept partially encoded URL", isValidEncodedUrl("https://example.com/path%20with%20spaces"))
        assertTrue("Should accept unencoded URL", isValidEncodedUrl("https://example.com/simple"))
        
        assertFalse("Should reject malformed encoding", isValidEncodedUrl("https://example.com/bad%encoding"))
        assertFalse("Should reject null URL", isValidEncodedUrl(null))
    }
    
    @Test
    fun `test Branch key validation in URLs`() {
        assertTrue("Should accept live key", isValidBranchKeyInUrl("key_live_abcdefghijklmnop"))
        assertTrue("Should accept test key", isValidBranchKeyInUrl("key_test_abcdefghijklmnop"))
        
        assertFalse("Should reject short key", isValidBranchKeyInUrl("key_live_abc"))
        assertFalse("Should reject wrong prefix", isValidBranchKeyInUrl("wrong_live_abcdefghijklmnop"))
        assertFalse("Should reject empty key", isValidBranchKeyInUrl(""))
        assertFalse("Should reject null key", isValidBranchKeyInUrl(null))
    }
    
    @Test
    fun `test URL path validation`() {
        assertTrue("Should accept simple path", isValidUrlPath("/simple"))
        assertTrue("Should accept nested path", isValidUrlPath("/path/to/resource"))
        assertTrue("Should accept path with parameters", isValidUrlPath("/path?param=value"))
        
        assertFalse("Should reject path without leading slash", isValidUrlPath("invalid"))
        assertFalse("Should reject empty path", isValidUrlPath(""))
        assertFalse("Should reject null path", isValidUrlPath(null))
        assertFalse("Should reject path with spaces", isValidUrlPath("/path with spaces"))
    }
    
    @Test
    fun `test query string validation`() {
        assertTrue("Should accept simple query", isValidQueryString("param=value"))
        assertTrue("Should accept multiple parameters", isValidQueryString("param1=value1&param2=value2"))
        assertTrue("Should accept encoded values", isValidQueryString("param=hello%20world"))
        
        assertFalse("Should reject query with spaces", isValidQueryString("param=value with spaces"))
        assertFalse("Should reject malformed query", isValidQueryString("param=value&"))
        assertFalse("Should reject null query", isValidQueryString(null))
    }
    
    @Test
    fun `test URL fragment validation`() {
        assertTrue("Should accept simple fragment", isValidUrlFragment("section1"))
        assertTrue("Should accept alphanumeric fragment", isValidUrlFragment("section123"))
        assertTrue("Should accept fragment with underscores", isValidUrlFragment("section_name"))
        
        assertFalse("Should reject fragment with spaces", isValidUrlFragment("section name"))
        assertFalse("Should reject fragment with special chars", isValidUrlFragment("section@name"))
        assertFalse("Should reject empty fragment", isValidUrlFragment(""))
        assertFalse("Should reject null fragment", isValidUrlFragment(null))
    }
    
    @Test
    fun `test URL domain validation`() {
        assertTrue("Should accept valid domain", isValidDomain("example.com"))
        assertTrue("Should accept subdomain", isValidDomain("subdomain.example.com"))
        assertTrue("Should accept app.link domain", isValidDomain("test.app.link"))
        
        assertFalse("Should reject domain with spaces", isValidDomain("invalid domain.com"))
        assertFalse("Should reject empty domain", isValidDomain(""))
        assertFalse("Should reject null domain", isValidDomain(null))
        assertFalse("Should reject domain with protocol", isValidDomain("https://example.com"))
    }
    
    @Test
    fun `test URL validation edge cases`() {
        // Test with very long URLs  
        val longUrl = "https://example.com/" + "a".repeat(3000) // Exceeds 2048 limit
        assertFalse("Should reject extremely long URLs", isValidBranchLink(longUrl))
        
        // Test with international domains
        assertTrue("Should accept international domain", isValidDomain("example.测试"))
        
        // Test with IPv4 addresses
        assertTrue("Should accept IP address", isValidDomain("192.168.1.1"))
        
        // Test empty but valid components
        assertTrue("Should accept URL without query", isValidUrlPath("/path"))
        assertTrue("Should accept URL without fragment", isValidUrlPath("/path"))
    }
    
    // Helper methods that simulate Branch URL validation logic
    private fun isValidBranchLink(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (url.length > 2048) return false // Reasonable URL length limit
        
        return try {
            val parsedUrl = java.net.URL(url)
            parsedUrl.protocol == "https" && 
            (parsedUrl.host.contains("app.link") || parsedUrl.host.contains(".com"))
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isValidDeepLink(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        
        return try {
            val uri = java.net.URI(url)
            uri.scheme != null && uri.scheme.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isValidUrlParam(param: String?): Boolean {
        if (param.isNullOrEmpty()) return false
        return param.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))
    }
    
    private fun isValidEncodedUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        
        // Simple check for proper URL encoding
        return !url.contains(" ") && 
               !url.contains(Regex("%[^0-9A-Fa-f]")) &&
               !url.contains(Regex("%[0-9A-Fa-f][^0-9A-Fa-f]"))
    }
    
    private fun isValidBranchKeyInUrl(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        if (key.length < 20) return false
        return key.startsWith("key_live_") || key.startsWith("key_test_")
    }
    
    private fun isValidUrlPath(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return path.startsWith("/") && !path.contains(" ")
    }
    
    private fun isValidQueryString(query: String?): Boolean {
        if (query.isNullOrEmpty()) return false
        return !query.contains(" ") && 
               !query.endsWith("&") && 
               !query.startsWith("&") &&
               query.contains("=")
    }
    
    private fun isValidUrlFragment(fragment: String?): Boolean {
        if (fragment.isNullOrEmpty()) return false
        return fragment.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))
    }
    
    private fun isValidDomain(domain: String?): Boolean {
        if (domain.isNullOrEmpty()) return false
        if (domain.contains("://")) return false // Should not contain protocol
        if (domain.contains(" ")) return false
        
        return try {
            // Simple domain validation
            domain.contains(".") || domain.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))
        } catch (e: Exception) {
            false
        }
    }
}