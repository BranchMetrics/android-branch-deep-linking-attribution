package io.branch.referral

import android.content.Intent
import android.os.Bundle
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.LinkProperties
import io.branch.referral.util.PerTargetChannelConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Method

/**
 * EMT-3881: [PerTargetLinkGenerator] is package-private, so these tests live in the same package.
 * Robolectric is needed for [Bundle]/[Intent].
 */
class PerTargetLinkGeneratorTest : BranchTestBase() {

    private lateinit var buo: BranchUniversalObject
    private lateinit var linkProperties: LinkProperties

    @Before
    fun setUp() {
        super.setUpBase()
        PerTargetChannelConfig.reset()
        buo = mock(BranchUniversalObject::class.java)
        linkProperties = LinkProperties()
    }

    @After
    fun tearDown() {
        PerTargetChannelConfig.reset()
    }

    @Test
    fun `generate calls back with a null bundle when the feature is disabled`() {
        PerTargetChannelConfig.setEnabled(false)
        var invoked = false
        var result: Bundle? = Bundle() // sentinel, proves the callback actually ran

        PerTargetLinkGenerator.generate(RuntimeEnvironment.getApplication(), buo, linkProperties) { bundle ->
            invoked = true
            result = bundle
        }

        assertTrue("callback must run even when disabled", invoked)
        assertNull("disabled feature must not produce replacement extras", result)
        verifyNoInteractions(buo)
    }

    /**
     * An alias pins the share to one fixed URL, so N per-target variants would either collide
     * server-side (SHORT) or contradict what the integrator asked for (LONG). Subtle and easy to
     * regress -- pin it explicitly, including that link generation is never attempted.
     */
    @Test
    fun `generate calls back with a null bundle when the link has an alias, even if enabled`() {
        PerTargetChannelConfig.setEnabled(true)
        linkProperties.setAlias("summer-sale")
        var result: Bundle? = Bundle()

        PerTargetLinkGenerator.generate(RuntimeEnvironment.getApplication(), buo, linkProperties) { bundle ->
            result = bundle
        }

        assertNull("an alias must skip per-target attribution entirely", result)
        verifyNoInteractions(buo)
    }

    @Test
    fun `replacement bundle nests each target's EXTRA_TEXT under an outer package-keyed bundle`() {
        val urlsByPackage = mapOf(
            "com.whatsapp" to "https://example.app.link/whatsapp",
            "com.facebook.katana" to "https://example.app.link/facebook"
        )

        val replacementExtras = invokeToReplacementExtras(urlsByPackage)

        assertNotNull(replacementExtras)
        assertEquals(setOf("com.whatsapp", "com.facebook.katana"), replacementExtras!!.keySet())

        val whatsappExtras = replacementExtras.getBundle("com.whatsapp")
        assertNotNull("outer bundle must nest an inner bundle per package", whatsappExtras)
        assertEquals(
            "https://example.app.link/whatsapp",
            whatsappExtras!!.getString(Intent.EXTRA_TEXT)
        )

        val facebookExtras = replacementExtras.getBundle("com.facebook.katana")
        assertNotNull(facebookExtras)
        assertEquals(
            "https://example.app.link/facebook",
            facebookExtras!!.getString(Intent.EXTRA_TEXT)
        )
    }

    @Test
    fun `replacement bundle is null when no url was produced for any target`() {
        assertNull(invokeToReplacementExtras(emptyMap()))
    }

    /** Exercises the private static shaping helper directly, isolated from link generation. */
    private fun invokeToReplacementExtras(urlsByPackage: Map<String, String>): Bundle? {
        val method: Method = PerTargetLinkGenerator::class.java
            .getDeclaredMethod("toReplacementExtras", Map::class.java)
            .apply { isAccessible = true }
        return method.invoke(null, urlsByPackage) as Bundle?
    }
}
