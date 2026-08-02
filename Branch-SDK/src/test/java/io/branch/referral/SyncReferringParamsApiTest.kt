package io.branch.referral

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * API-surface tests for the restored synchronous deep-link parameter getters (EMT-3882).
 *
 * 5.x exposed getFirstReferringParamsSync() and getLatestReferringParamsSync(); the beta dropped
 * them for async-only access. Product asked to keep a synchronous option, so both are restored.
 * These tests pin the public contract (name, no-arg, returns JSONObject) and guard against the
 * methods being removed again. Behavioral latch-release happens on the init flow.
 */
class SyncReferringParamsApiTest {

    @Test
    fun getFirstReferringParamsSync_isPublicNoArgReturningJsonObject() {
        val method = Branch::class.java.getMethod("getFirstReferringParamsSync")
        assertTrue("should be public", Modifier.isPublic(method.modifiers))
        assertEquals("should return JSONObject", JSONObject::class.java, method.returnType)
        assertEquals("should take no arguments", 0, method.parameterCount)
    }

    @Test
    fun getLatestReferringParamsSync_isPublicNoArgReturningJsonObject() {
        val method = Branch::class.java.getMethod("getLatestReferringParamsSync")
        assertTrue("should be public", Modifier.isPublic(method.modifiers))
        assertEquals("should return JSONObject", JSONObject::class.java, method.returnType)
        assertEquals("should take no arguments", 0, method.parameterCount)
    }
}
