package io.branch.referral.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * EMT-3881: [PerTargetChannelConfig] is a plain Kotlin object with no Android dependency, so these
 * run as plain JUnit tests, no Robolectric needed.
 */
class PerTargetChannelConfigTest {

    @Before
    fun setUp() {
        // Process-wide singleton state -- start every test from the shipped defaults.
        PerTargetChannelConfig.reset()
        BranchChannelMap.clearOverrides()
    }

    @After
    fun tearDown() {
        PerTargetChannelConfig.reset()
        BranchChannelMap.clearOverrides()
    }

    /** This is the spike gate: a regression here ships unverified behaviour to every device. */
    @Test
    fun `feature is disabled by default with LONG as the default strategy`() {
        assertFalse(
            "per-target channel attribution must ship off, pending the device spike",
            PerTargetChannelConfig.isEnabled()
        )
        assertEquals(PerTargetLinkStrategy.LONG, PerTargetChannelConfig.getLinkStrategy())
    }

    @Test
    fun `setEnabled and setLinkStrategy are honoured`() {
        PerTargetChannelConfig.setEnabled(true)
        PerTargetChannelConfig.setLinkStrategy(PerTargetLinkStrategy.SHORT)

        assertTrue(PerTargetChannelConfig.isEnabled())
        assertEquals(PerTargetLinkStrategy.SHORT, PerTargetChannelConfig.getLinkStrategy())
    }

    @Test
    fun `resolveTargetPackages returns the full channel map when no allowlist is set`() {
        assertEquals(BranchChannelMap.mappedPackages(), PerTargetChannelConfig.resolveTargetPackages())
    }

    @Test
    fun `resolveTargetPackages returns the intersection when an allowlist is set`() {
        PerTargetChannelConfig.setTargetPackages(setOf("com.whatsapp", "com.facebook.katana"))

        assertEquals(
            setOf("com.whatsapp", "com.facebook.katana"),
            PerTargetChannelConfig.resolveTargetPackages()
        )
    }

    @Test
    fun `resolveTargetPackages drops an allowlisted package the channel map does not know`() {
        PerTargetChannelConfig.setTargetPackages(setOf("com.whatsapp", "com.example.unmapped"))

        assertEquals(setOf("com.whatsapp"), PerTargetChannelConfig.resolveTargetPackages())
    }

    @Test
    fun `setTargetPackages null restores the full channel map`() {
        PerTargetChannelConfig.setTargetPackages(setOf("com.whatsapp"))

        PerTargetChannelConfig.setTargetPackages(null)

        assertEquals(BranchChannelMap.mappedPackages(), PerTargetChannelConfig.resolveTargetPackages())
    }

    @Test
    fun `reset restores disabled, LONG strategy, and no allowlist`() {
        PerTargetChannelConfig.setEnabled(true)
        PerTargetChannelConfig.setLinkStrategy(PerTargetLinkStrategy.SHORT)
        PerTargetChannelConfig.setTargetPackages(setOf("com.whatsapp"))

        PerTargetChannelConfig.reset()

        assertFalse(PerTargetChannelConfig.isEnabled())
        assertEquals(PerTargetLinkStrategy.LONG, PerTargetChannelConfig.getLinkStrategy())
        assertEquals(BranchChannelMap.mappedPackages(), PerTargetChannelConfig.resolveTargetPackages())
    }
}
