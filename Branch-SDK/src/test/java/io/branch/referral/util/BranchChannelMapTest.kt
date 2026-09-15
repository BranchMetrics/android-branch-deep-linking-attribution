package io.branch.referral.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * EMT-3881: [BranchChannelMap] is a plain Kotlin object with no Android dependency, so these run
 * as plain JUnit tests, no Robolectric needed.
 */
class BranchChannelMapTest {

    @After
    fun tearDown() {
        // The map is a process-wide singleton; drop any override a test added.
        BranchChannelMap.clearOverrides()
    }

    @Test
    fun `known packages map to their iOS-aligned channel names`() {
        assertEquals("WhatsApp", BranchChannelMap.channelForPackage("com.whatsapp"))
        assertEquals("Facebook", BranchChannelMap.channelForPackage("com.facebook.katana"))
        assertEquals("Email", BranchChannelMap.channelForPackage("com.google.android.gm"))
        assertEquals("WeChat", BranchChannelMap.channelForPackage("com.tencent.mm"))
    }

    /**
     * Pins the correct package name. The legacy `SharingHelper.SHARE_WITH.WECHAT` constant has a
     * typo'd package, "jom.tencent.mm" instead of "com.tencent.mm" -- this map must not inherit
     * that typo.
     */
    @Test
    fun `WeChat maps from the correctly spelled package, not the legacy SharingHelper typo`() {
        assertEquals("WeChat", BranchChannelMap.channelForPackage("com.tencent.mm"))
        assertNull(
            "the legacy typo'd package must stay unmapped",
            BranchChannelMap.channelForPackage("jom.tencent.mm")
        )
    }

    @Test
    fun `unknown package returns null`() {
        assertNull(BranchChannelMap.channelForPackage("com.example.unmapped"))
    }

    @Test
    fun `null or empty package name returns null`() {
        assertNull(BranchChannelMap.channelForPackage(null))
        assertNull(BranchChannelMap.channelForPackage(""))
    }

    @Test
    fun `setChannelForPackage adds a mapping for a package Branch does not ship`() {
        assertNull(BranchChannelMap.channelForPackage("com.example.newapp"))

        BranchChannelMap.setChannelForPackage("com.example.newapp", "NewApp")

        assertEquals("NewApp", BranchChannelMap.channelForPackage("com.example.newapp"))
    }

    @Test
    fun `setChannelForPackage overrides an existing default mapping`() {
        assertEquals("WhatsApp", BranchChannelMap.channelForPackage("com.whatsapp"))

        BranchChannelMap.setChannelForPackage("com.whatsapp", "WA")

        assertEquals("WA", BranchChannelMap.channelForPackage("com.whatsapp"))
    }

    @Test
    fun `clearOverrides restores the shipped default mapping`() {
        BranchChannelMap.setChannelForPackage("com.whatsapp", "WA")
        assertEquals("WA", BranchChannelMap.channelForPackage("com.whatsapp"))

        BranchChannelMap.clearOverrides()

        assertEquals("WhatsApp", BranchChannelMap.channelForPackage("com.whatsapp"))
    }
}
