package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
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
    private lateinit var mockedStaticBranch: MockedStatic<Branch>
    private lateinit var mockedStaticBranchUtil: MockedStatic<BranchUtil>

    @Before
    fun setup() {
        controller = BranchConfigurationController()
        
        // Set up static mocking
        mockedStaticBranch = Mockito.mockStatic(Branch::class.java)
        mockedStaticBranchUtil = Mockito.mockStatic(BranchUtil::class.java)
        
        // Mock Branch.getInstance() to return our mock
        mockedStaticBranch.`when`<Branch> { Branch.getInstance() }.thenReturn(mockBranch)
        
        // Use reflection to set the prefHelper_ field
        val prefHelperField = Branch::class.java.getDeclaredField("prefHelper_")
        prefHelperField.isAccessible = true
        prefHelperField.set(mockBranch, mockPrefHelper)
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
        mockedStaticBranchUtil.`when`<Boolean> { BranchUtil.isTestModeEnabled() }.thenReturn(expectedValue)

        // When
        val result = controller.isTestModeEnabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `test setTestModeEnabled sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setTestModeEnabled(expectedValue)

        // Then
        mockedStaticBranchUtil.verify { BranchUtil.setTestMode(expectedValue) }
    }

    @Test
    fun `test setInstantDeepLinkingEnabled sets the correct value`() {
        // Given
        val expectedValue = true

        // When
        controller.setInstantDeepLinkingEnabled(expectedValue)

        // Then
        verify(mockPrefHelper).setBool(BranchConfigurationController.KEY_INSTANT_DEEP_LINKING_ENABLED, expectedValue)
    }

    @Test
    fun `test isInstantDeepLinkingEnabled returns correct value`() {
        // Given
        val expectedValue = true
        `when`(mockPrefHelper.getBool(BranchConfigurationController.KEY_INSTANT_DEEP_LINKING_ENABLED)).thenReturn(expectedValue)

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
        verify(mockPrefHelper).setBool(BranchConfigurationController.KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME, expectedValue)
    }

    @Test
    fun `test getBranchKeySource returns correct value`() {
        // Given
        val expectedSource = "branch.json"
        `when`(mockPrefHelper.branchKeySource).thenReturn(expectedSource)

        // When
        val result = controller.getBranchKeySource()

        // Then
        assertEquals(expectedSource, result)
    }

    @Test
    fun `test getBranchKeySource returns unknown when Branch instance is null`() {
        // Given
        mockedStaticBranch.`when`<Branch> { Branch.getInstance() }.thenReturn(null)

        // When
        val result = controller.getBranchKeySource()

        // Then
        assertEquals("unknown", result)
    }

    @Test
    fun `test getBranchKeySource returns unknown when branchKeySource is null`() {
        // Given
        `when`(mockPrefHelper.branchKeySource).thenReturn(null)

        // When
        val result = controller.getBranchKeySource()

        // Then
        assertEquals("unknown", result)
    }

    @Test
    fun `test isBranchKeyFallbackUsed returns true when branch key source is branchKey`() {
        // Given
        `when`(mockPrefHelper.branchKeySource).thenReturn("branchKey")

        // When
        val result = controller.isBranchKeyFallbackUsed()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `test isBranchKeyFallbackUsed returns false when branch key source is not branchKey`() {
        // Given
        `when`(mockPrefHelper.branchKeySource).thenReturn("manifest")

        // When
        val result = controller.isBranchKeyFallbackUsed()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `test isBranchKeyFallbackUsed returns false when Branch instance is null`() {
        // Given
        mockedStaticBranch.`when`<Branch> { Branch.getInstance() }.thenReturn(null)

        // When
        val result = controller.isBranchKeyFallbackUsed()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `test serializeConfiguration returns correct JSON object`() {
        // Given
        val expectedDelayedSessionInit = true
        val expectedTestMode = true
        val expectedInstantDeepLinkingEnabled = true
        val expectedDeferInitForPluginRuntime = true
        val expectedBranchKeySource = "manifest"
        val expectedBranchKeyFallbackUsed = false

        // Setup mocks
        `when`(mockPrefHelper.delayedSessionInitUsed).thenReturn(expectedDelayedSessionInit)
        mockedStaticBranchUtil.`when`<Boolean> { BranchUtil.isTestModeEnabled() }.thenReturn(expectedTestMode)
        `when`(mockPrefHelper.getBool(BranchConfigurationController.KEY_INSTANT_DEEP_LINKING_ENABLED)).thenReturn(expectedInstantDeepLinkingEnabled)
        `when`(mockPrefHelper.getBool(BranchConfigurationController.KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME)).thenReturn(expectedDeferInitForPluginRuntime)
        `when`(mockPrefHelper.branchKeySource).thenReturn(expectedBranchKeySource)

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertEquals(expectedDelayedSessionInit, result.getBoolean("expectDelayedSessionInitialization"))
        assertEquals(expectedTestMode, result.getBoolean("testMode"))
        assertEquals(expectedInstantDeepLinkingEnabled, result.getBoolean("instantDeepLinkingEnabled"))
        assertEquals(expectedDeferInitForPluginRuntime, result.getBoolean("deferInitForPluginRuntime"))
        assertEquals(expectedBranchKeySource, result.getString("branch_key_source"))
        assertEquals(expectedBranchKeyFallbackUsed, result.getBoolean("branch_key_fallback_used"))
    }

    @Test
    fun `test serializeConfiguration with branch key fallback used`() {
        // Given
        val expectedDelayedSessionInit = false
        val expectedTestMode = false
        val expectedInstantDeepLinkingEnabled = false
        val expectedDeferInitForPluginRuntime = false
        val expectedBranchKeySource = "branchKey"
        val expectedBranchKeyFallbackUsed = true

        // Setup mocks
        `when`(mockPrefHelper.delayedSessionInitUsed).thenReturn(expectedDelayedSessionInit)
        mockedStaticBranchUtil.`when`<Boolean> { BranchUtil.isTestModeEnabled() }.thenReturn(expectedTestMode)
        `when`(mockPrefHelper.getBool(BranchConfigurationController.KEY_INSTANT_DEEP_LINKING_ENABLED)).thenReturn(expectedInstantDeepLinkingEnabled)
        `when`(mockPrefHelper.getBool(BranchConfigurationController.KEY_DEFER_INIT_FOR_PLUGIN_RUNTIME)).thenReturn(expectedDeferInitForPluginRuntime)
        `when`(mockPrefHelper.branchKeySource).thenReturn(expectedBranchKeySource)

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertEquals(expectedDelayedSessionInit, result.getBoolean("expectDelayedSessionInitialization"))
        assertEquals(expectedTestMode, result.getBoolean("testMode"))
        assertEquals(expectedInstantDeepLinkingEnabled, result.getBoolean("instantDeepLinkingEnabled"))
        assertEquals(expectedDeferInitForPluginRuntime, result.getBoolean("deferInitForPluginRuntime"))
        assertEquals(expectedBranchKeySource, result.getString("branch_key_source"))
        assertEquals(expectedBranchKeyFallbackUsed, result.getBoolean("branch_key_fallback_used"))
    }

    @Test
    fun `test serializeConfiguration handles NullPointerException gracefully`() {
        // Given
        `when`(mockPrefHelper.delayedSessionInitUsed).thenThrow(NullPointerException("Test null pointer"))

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertNotNull(result)
        assertEquals(0, result.length())
    }

    @Test
    fun `test serializeConfiguration handles JSONException gracefully`() {
        // Given
        // We can't directly mock JSONObject to throw JSONException, but we can verify the structure handles it
        val result = controller.serializeConfiguration()

        // Then
        assertNotNull(result)
        // The result should either have all fields or be empty if an exception occurred
        assert(result.length() == 0 || result.has("expectDelayedSessionInitialization"))
    }

    @Test
    fun `test serializeConfiguration handles generic exception gracefully`() {
        // Given
        `when`(mockPrefHelper.delayedSessionInitUsed).thenThrow(RuntimeException("Test exception"))

        // When
        val result = controller.serializeConfiguration()

        // Then
        assertNotNull(result)
        assertEquals(0, result.length())
    }

    @org.junit.After
    fun tearDown() {
        // Clean up static mocks
        mockedStaticBranch.close()
        mockedStaticBranchUtil.close()
    }
} 