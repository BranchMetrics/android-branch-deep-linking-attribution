package io.branch.referral

import io.branch.interfaces.IBranchLoggingCallbacks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [DMAParameters] and its [DMAParameters.Builder].
 *
 * The builder exists so a Java call site names each field: three positional booleans are as
 * transposable inside a constructor as they were in setDMAParamsForEEA.
 */
@RunWith(RobolectricTestRunner::class)
class DMAParametersTest {

    private val captured = mutableListOf<String>()

    @Before
    fun setUp() {
        captured.clear()
        BranchLogger.loggerCallback = IBranchLoggingCallbacks { message, _ -> captured.add(message) }
        BranchLogger.loggingEnabled = true
        BranchLogger.loggingLevel = BranchLogger.BranchLogLevel.VERBOSE
    }

    @After
    fun tearDown() {
        BranchLogger.loggerCallback = null
        BranchLogger.loggingEnabled = false
        BranchLogger.loggingLevel = BranchLogger.BranchLogLevel.DEBUG
    }

    // -----------------------------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------------------------

    @Test
    fun `builder stores every field`() {
        val params = DMAParameters.Builder()
            .setEeaRegion(true)
            .setAdPersonalizationConsent(false)
            .setAdUserDataUsageConsent(true)
            .build()

        assertTrue(params.eeaRegion)
        assertFalse(params.adPersonalizationConsent)
        assertTrue(params.adUserDataUsageConsent)
    }

    @Test
    fun `unset fields default to false`() {
        val params = DMAParameters.Builder().build()

        assertFalse(params.eeaRegion)
        assertFalse(params.adPersonalizationConsent)
        assertFalse(params.adUserDataUsageConsent)
    }

    @Test
    fun `setters return the builder for chaining`() {
        val builder = DMAParameters.Builder()
        assertEquals(builder, builder.setEeaRegion(true))
        assertEquals(builder, builder.setAdPersonalizationConsent(true))
        assertEquals(builder, builder.setAdUserDataUsageConsent(true))
    }

    // -----------------------------------------------------------------------------------------
    // Equality
    // -----------------------------------------------------------------------------------------

    @Test
    fun `instances with the same values are equal`() {
        val a = DMAParameters.Builder().setEeaRegion(true).setAdUserDataUsageConsent(true).build()
        val b = DMAParameters.Builder().setEeaRegion(true).setAdUserDataUsageConsent(true).build()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `instances with different values are not equal`() {
        val a = DMAParameters.Builder().setEeaRegion(true).build()
        val b = DMAParameters.Builder().build()

        assertNotEquals(a, b)
    }

    @Test
    fun `a warning does not affect equality`() {
        val warned = DMAParameters.Builder().setAdUserDataUsageConsent(true).build()
        val same = DMAParameters.Builder().setAdUserDataUsageConsent(true).build()

        assertEquals(warned, same)
    }

    // -----------------------------------------------------------------------------------------
    // toBuilder
    // -----------------------------------------------------------------------------------------

    @Test
    fun `toBuilder round-trips every field`() {
        val original = DMAParameters.Builder()
            .setEeaRegion(true)
            .setAdPersonalizationConsent(true)
            .setAdUserDataUsageConsent(true)
            .build()

        assertEquals(original, original.toBuilder().build())
    }

    @Test
    fun `toBuilder changes one field without restating the others`() {
        val original = DMAParameters.Builder()
            .setEeaRegion(true)
            .setAdPersonalizationConsent(true)
            .setAdUserDataUsageConsent(true)
            .build()

        val revoked = original.toBuilder().setAdPersonalizationConsent(false).build()

        assertTrue(revoked.eeaRegion)
        assertFalse(revoked.adPersonalizationConsent)
        assertTrue(revoked.adUserDataUsageConsent)
    }

    // -----------------------------------------------------------------------------------------
    // Inconsistency warning
    //
    // Determined at build() but not logged there — at that point the logger is still disabled and
    // the line would be lost. It is emitted by whichever consumer takes the object.
    // -----------------------------------------------------------------------------------------

    private val inconsistent = "consent was granted with eeaRegion false"

    @Test
    fun `build does not log the warning`() {
        DMAParameters.Builder().setAdUserDataUsageConsent(true).build()

        assertTrue(
            "the warning must be carried on the instance, not emitted from build()",
            captured.none { it.contains(inconsistent) }
        )
    }

    @Test
    fun `consumer emits the warning when consent is granted outside the EEA`() {
        DMAParameters.Builder().setAdUserDataUsageConsent(true).build().logWarnings()

        assertTrue(captured.any { it.contains(inconsistent) })
    }

    @Test
    fun `adPersonalizationConsent outside the EEA also warns`() {
        DMAParameters.Builder().setAdPersonalizationConsent(true).build().logWarnings()

        assertTrue(captured.any { it.contains(inconsistent) })
    }

    @Test
    fun `consent inside the EEA does not warn`() {
        DMAParameters.Builder()
            .setEeaRegion(true)
            .setAdPersonalizationConsent(true)
            .setAdUserDataUsageConsent(true)
            .build()
            .logWarnings()

        assertTrue(captured.none { it.contains(inconsistent) })
    }

    @Test
    fun `no consent outside the EEA does not warn`() {
        DMAParameters.Builder().build().logWarnings()

        assertTrue(captured.none { it.contains(inconsistent) })
    }

    @Test
    fun `toBuilder carries the warning to the rebuilt instance`() {
        val rebuilt = DMAParameters.Builder().setAdUserDataUsageConsent(true).build().toBuilder().build()
        rebuilt.logWarnings()

        assertTrue(captured.any { it.contains(inconsistent) })
    }

    @Test
    fun `correcting the region through toBuilder clears the warning`() {
        val corrected = DMAParameters.Builder()
            .setAdUserDataUsageConsent(true)
            .build()
            .toBuilder()
            .setEeaRegion(true)
            .build()
        corrected.logWarnings()

        assertTrue(captured.none { it.contains(inconsistent) })
    }

    // -----------------------------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------------------------

    @Test
    fun `toString lists every field and no warning state`() {
        val text = DMAParameters.Builder().setEeaRegion(true).build().toString()

        assertTrue(text.contains("eeaRegion=true"))
        assertTrue(text.contains("adPersonalizationConsent=false"))
        assertTrue(text.contains("adUserDataUsageConsent=false"))
        assertFalse(text.contains("warning"))
    }
}
