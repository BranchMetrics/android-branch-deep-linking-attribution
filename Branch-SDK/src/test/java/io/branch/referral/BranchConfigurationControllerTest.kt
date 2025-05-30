package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BranchConfigurationControllerTest {

    @Mock
    private lateinit var mockBranch: Branch

    @Mock
    private lateinit var mockPrefHelper: PrefHelper

    private lateinit var controller: BranchConfigurationController

    @Before
    fun setup() {
        controller = BranchConfigurationController()
        // Mock Branch.getInstance() to return our mock instance
        val branchField = Branch::class.java.getDeclaredField("branchReferral_")
        branchField.isAccessible = true
        branchField.set(null, mockBranch)
        
        // Mock prefHelper_ to return our mock instance
        `when`(mockBranch.prefHelper_).thenReturn(mockPrefHelper)
    }

    @Test
    fun `test setDelayedSessionInitUsed sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setDelayedSessionInitUsed(expectedValue)

        // Then
        verify(mockPrefHelper).delayedSessionInitUsed = expectedValue
    }

    @Test
    fun `test isTestModeEnabled returns correct value`() {
        // Given
        val expectedValue = true
        BranchUtil.setTestMode(expectedValue)

        // When
        val result = controller.isTestModeEnabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `test isTrackingDisabled returns correct value`() {
        // Given
        val expectedValue = true
        `when`(mockPrefHelper.getBool("bnc_tracking_disabled")).thenReturn(expectedValue)

        // When
        val result = controller.isTrackingDisabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `test setTrackingDisabled sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setTrackingDisabled(expectedValue)

        // Then
        verify(mockPrefHelper).setBool("bnc_tracking_disabled", expectedValue)
    }

    @Test
    fun `test setTestModeEnabled sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setTestModeEnabled(expectedValue)

        // Then
        assertTrue(BranchUtil.isTestModeEnabled())
    }

    @Test
    fun `test setInstantDeepLinkingEnabled sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setInstantDeepLinkingEnabled(expectedValue)

        // Then
        verify(mockPrefHelper).setBool("bnc_instant_deep_linking_enabled", expectedValue)
    }

    @Test
    fun `test isInstantDeepLinkingEnabled returns correct value`() {
        // Given
        val expectedValue = true
        `when`(mockPrefHelper.getBool("bnc_instant_deep_linking_enabled")).thenReturn(expectedValue)

        // When
        val result = controller.isInstantDeepLinkingEnabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `test setDeferInitForPluginRuntime sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setDeferInitForPluginRuntime(expectedValue)

        // Then
        verify(mockPrefHelper).setBool("bnc_defer_init_for_plugin_runtime", expectedValue)
    }

    @Test
    fun `test serializeConfiguration returns correct JSON object`() {
        // Given
        val expectedDelayedSessionInit = true
        val expectedTestMode = true
        val expectedTrackingDisabled = true
        val expectedInstantDeepLinkingEnabled = true
        val expectedDeferInitForPluginRuntime = true

        // Setup mocks
        `when`(mockPrefHelper.delayedSessionInitUsed).thenReturn(expectedDelayedSessionInit)
        BranchUtil.setTestMode(expectedTestMode)
        `when`(mockPrefHelper.getBool("bnc_tracking_disabled")).thenReturn(expectedTrackingDisabled)
        `when`(mockPrefHelper.getBool("bnc_instant_deep_linking_enabled")).thenReturn(expectedInstantDeepLinkingEnabled)
        `when`(mockPrefHelper.getBool("bnc_defer_init_for_plugin_runtime")).thenReturn(expectedDeferInitForPluginRuntime)

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertEquals(expectedDelayedSessionInit, result.getBoolean("expectDelayedSessionInitialization"))
        assertEquals(expectedTestMode, result.getBoolean("testMode"))
        assertEquals(expectedTrackingDisabled, result.getBoolean("trackingDisabled"))
        assertEquals(expectedInstantDeepLinkingEnabled, result.getBoolean("instantDeepLinkingEnabled"))
        assertEquals(expectedDeferInitForPluginRuntime, result.getBoolean("deferInitForPluginRuntime"))
    }

    @Test
    fun `test serializeConfiguration handles exception gracefully`() {
        // Given
        `when`(mockPrefHelper.delayedSessionInitUsed).thenThrow(RuntimeException("Test exception"))

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertNotNull(result)
        assertEquals(0, result.length())
    }
} 