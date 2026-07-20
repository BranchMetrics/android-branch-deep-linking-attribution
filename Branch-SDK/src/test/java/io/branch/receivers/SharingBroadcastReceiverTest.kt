package io.branch.receivers

import android.content.ComponentName
import io.branch.referral.BranchTestBase
import io.branch.referral.util.BranchChannelMap
import io.branch.referral.util.PerTargetChannelConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * EMT-3881: `resolveChannelName` is a private implementation detail of
 * [SharingBroadcastReceiver] with no public seam. Exercised via reflection, the same pattern
 * [io.branch.referral.TriageRetryCeilingTest] uses for a package-private production method,
 * rather than widening its visibility.
 */
class SharingBroadcastReceiverTest : BranchTestBase() {

    private lateinit var receiver: SharingBroadcastReceiver
    private lateinit var resolveChannelName: Method

    @Before
    fun setUp() {
        super.setUpBase()
        PerTargetChannelConfig.reset()
        BranchChannelMap.clearOverrides()
        receiver = SharingBroadcastReceiver()
        resolveChannelName = SharingBroadcastReceiver::class.java
            .getDeclaredMethod("resolveChannelName", ComponentName::class.java)
            .apply { isAccessible = true }
    }

    @After
    fun tearDown() {
        PerTargetChannelConfig.reset()
        BranchChannelMap.clearOverrides()
    }

    @Test
    fun `returns the raw component name when the feature is disabled`() {
        PerTargetChannelConfig.setEnabled(false)
        val component = ComponentName("com.whatsapp", "com.whatsapp.ShareActivity")

        val result = resolveChannelName.invoke(receiver, component) as String

        assertEquals(component.toString(), result)
    }

    @Test
    fun `returns the friendly channel name when enabled and the target package is mapped`() {
        PerTargetChannelConfig.setEnabled(true)
        val component = ComponentName("com.whatsapp", "com.whatsapp.ShareActivity")

        val result = resolveChannelName.invoke(receiver, component) as String

        assertEquals("WhatsApp", result)
    }

    @Test
    fun `returns the raw component name when enabled but the target package is unmapped`() {
        PerTargetChannelConfig.setEnabled(true)
        val component = ComponentName("com.example.unmapped", "com.example.unmapped.ShareActivity")

        val result = resolveChannelName.invoke(receiver, component) as String

        assertEquals(component.toString(), result)
    }
}
