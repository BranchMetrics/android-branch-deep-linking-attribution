package io.branch.referral.util

import io.branch.referral.BranchTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * EMT-3881 spike gate: per-target channel attribution must ship DISABLED.
 *
 * Deliberately separate from [PerTargetChannelConfigTest], and deliberately without a
 * `reset()` in setup. That test resets the singleton before every case, so it can only prove what
 * `reset()` does, not what the shipped default is. A mutation flipping the field initializer to
 * `true` slipped past it unnoticed. This class observes the object in its pristine state instead,
 * relying on Robolectric giving each test class its own classloader so the Kotlin object
 * initialises fresh here.
 *
 * Nothing in this class may call [PerTargetChannelConfig.setEnabled],
 * [PerTargetChannelConfig.setLinkStrategy] or [PerTargetChannelConfig.reset] — doing so destroys
 * the very thing it measures.
 */
class PerTargetChannelDefaultsTest : BranchTestBase() {

    @Test
    fun `ships disabled, defaulting to LONG links, pending the device spike`() {
        assertFalse(
            "per-target channel attribution must ship off: OEM chooser behaviour is unverified " +
                "and enabling it by default would ship untested behaviour to every device",
            PerTargetChannelConfig.isEnabled()
        )
        assertEquals(
            "LONG costs no network call; SHORT must stay opt-in until the spike reports latency",
            PerTargetLinkStrategy.LONG,
            PerTargetChannelConfig.getLinkStrategy()
        )
    }
}
