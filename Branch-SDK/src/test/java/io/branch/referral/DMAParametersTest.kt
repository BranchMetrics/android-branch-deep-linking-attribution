package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DMAParametersTest {

    @Test
    fun `fields are stored as supplied`() {
        val params = DMAParameters(
            eeaRegion = true,
            adPersonalizationConsent = false,
            adUserDataUsageConsent = true
        )
        assertEquals(true, params.eeaRegion)
        assertEquals(false, params.adPersonalizationConsent)
        assertEquals(true, params.adUserDataUsageConsent)
    }

    @Test
    fun `data class equality holds for identical values`() {
        val a = DMAParameters(eeaRegion = true, adPersonalizationConsent = true, adUserDataUsageConsent = true)
        val b = DMAParameters(eeaRegion = true, adPersonalizationConsent = true, adUserDataUsageConsent = true)
        assertEquals(a, b)
    }

    @Test
    fun `data class equality distinguishes different values`() {
        val a = DMAParameters(eeaRegion = true,  adPersonalizationConsent = false, adUserDataUsageConsent = false)
        val b = DMAParameters(eeaRegion = false, adPersonalizationConsent = false, adUserDataUsageConsent = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `copy produces independent instance with changed field`() {
        val original = DMAParameters(eeaRegion = true, adPersonalizationConsent = true, adUserDataUsageConsent = true)
        val copy = original.copy(adPersonalizationConsent = false)

        assertEquals(true, copy.eeaRegion)
        assertEquals(false, copy.adPersonalizationConsent)
        assertEquals(true, copy.adUserDataUsageConsent)
        // original is unchanged
        assertEquals(true, original.adPersonalizationConsent)
    }

    @Test
    fun `all false values are stored correctly`() {
        val params = DMAParameters(
            eeaRegion = false,
            adPersonalizationConsent = false,
            adUserDataUsageConsent = false
        )
        assertEquals(false, params.eeaRegion)
        assertEquals(false, params.adPersonalizationConsent)
        assertEquals(false, params.adUserDataUsageConsent)
    }
}
