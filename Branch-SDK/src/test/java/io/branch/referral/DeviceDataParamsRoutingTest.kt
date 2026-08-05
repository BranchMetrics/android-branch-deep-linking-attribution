package io.branch.referral

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment

/**
 * Exercises the real ServerRequest methods that write device/context fields into the request body,
 * verifying that each field lands in the correct location:
 *   - V1 requests → field written flat at top level
 *   - V2 requests → field written inside "user_data"
 *
 * Each test constructs a real ServerRequest subclass, injects a mocked PrefHelper, invokes the
 * production method, and inspects the resulting JSONObject.
 */
class DeviceDataParamsRoutingTest : BranchTestBase() {

    @Mock private lateinit var mockPrefHelper: PrefHelper
    @Mock private lateinit var mockBranch: Branch
    @Mock private lateinit var mockDeviceInfo: DeviceInfo
    @Mock private lateinit var mockSystemObserver: SystemObserver

    private lateinit var mockedStaticBranch: MockedStatic<Branch>

    // Concrete V1 and V2 request instances with real params_ and injected PrefHelper.
    private lateinit var v1Request: StubServerRequestV1
    private lateinit var v2Request: StubServerRequestV2

    @Before
    fun setUp() {
        super.setUpBase()

        mockedStaticBranch = Mockito.mockStatic(Branch::class.java)
        mockedStaticBranch.`when`<Branch> { Branch.getInstance() }.thenReturn(mockBranch)
        `when`(mockBranch.getDeviceInfo()).thenReturn(mockDeviceInfo)
        `when`(mockDeviceInfo.getSystemObserver()).thenReturn(mockSystemObserver)

        val context = RuntimeEnvironment.getApplication()

        // replaceHardwareIdOnValidAdvertisingId() calls DeviceInfo.getHardwareID() when a valid
        // GAID is present. Construct a real UniqueId via its package-accessible static factory.
        val stubHardwareId = SystemObserver.getUniqueID(context, true)
        `when`(mockDeviceInfo.getHardwareID()).thenReturn(stubHardwareId)

        v1Request = StubServerRequestV1(context, mockPrefHelper)
        v2Request = StubServerRequestV2(context, mockPrefHelper)
    }

    @After
    fun tearDown() {
        mockedStaticBranch.close()
    }

    // ── addDMAParams ──────────────────────────────────────────────────────────

    @Test
    fun `addDMAParams writes DMA fields to top level for V1`() {
        `when`(mockPrefHelper.isDMAParamsInitialized()).thenReturn(true)
        `when`(mockPrefHelper.getEEARegion()).thenReturn(true)
        `when`(mockPrefHelper.getAdPersonalizationConsent()).thenReturn(true)
        `when`(mockPrefHelper.getAdUserDataUsageConsent()).thenReturn(false)

        v1Request.addDMAParams()

        val params = v1Request.getPost()
        assertTrue(params.getBoolean(Defines.Jsonkey.DMA_EEA.getKey()))
        assertTrue(params.getBoolean(Defines.Jsonkey.DMA_Ad_Personalization.getKey()))
        assertFalse(params.getBoolean(Defines.Jsonkey.DMA_Ad_User_Data.getKey()))
        assertFalse("user_data must not be created by V1", params.has(Defines.Jsonkey.UserData.getKey()))
    }

    @Test
    fun `addDMAParams writes DMA fields to user_data for V2`() {
        `when`(mockPrefHelper.isDMAParamsInitialized()).thenReturn(true)
        `when`(mockPrefHelper.getEEARegion()).thenReturn(true)
        `when`(mockPrefHelper.getAdPersonalizationConsent()).thenReturn(false)
        `when`(mockPrefHelper.getAdUserDataUsageConsent()).thenReturn(true)

        v2Request.addDMAParams()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertTrue(userData.getBoolean(Defines.Jsonkey.DMA_EEA.getKey()))
        assertFalse(userData.getBoolean(Defines.Jsonkey.DMA_Ad_Personalization.getKey()))
        assertTrue(userData.getBoolean(Defines.Jsonkey.DMA_Ad_User_Data.getKey()))
        assertFalse("DMA fields must not leak to top level in V2", v2Request.getPost().has(Defines.Jsonkey.DMA_EEA.getKey()))
    }

    @Test
    fun `addDMAParams is a no-op when DMA params not initialized`() {
        `when`(mockPrefHelper.isDMAParamsInitialized()).thenReturn(false)

        v1Request.addDMAParams()
        v2Request.addDMAParams()

        assertFalse(v1Request.getPost().has(Defines.Jsonkey.DMA_EEA.getKey()))
        assertFalse(v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey()).has(Defines.Jsonkey.DMA_EEA.getKey()))
    }

    // ── addConsumerProtectionAttributionLevel ─────────────────────────────────

    @Test
    fun `attribution level writes to top level for V1`() {
        `when`(mockPrefHelper.isAttributionLevelInitialized()).thenReturn(true)
        `when`(mockPrefHelper.getConsumerProtectionAttributionLevel()).thenReturn(Defines.BranchAttributionLevel.FULL)

        v1Request.callAddConsumerProtectionAttributionLevel()

        assertEquals("FULL", v1Request.getPost().getString(Defines.Jsonkey.Consumer_Protection_Attribution_Level.getKey()))
        assertFalse(v1Request.getPost().has(Defines.Jsonkey.UserData.getKey()))
    }

    @Test
    fun `attribution level writes to user_data for V2`() {
        `when`(mockPrefHelper.isAttributionLevelInitialized()).thenReturn(true)
        `when`(mockPrefHelper.getConsumerProtectionAttributionLevel()).thenReturn(Defines.BranchAttributionLevel.FULL)

        v2Request.callAddConsumerProtectionAttributionLevel()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertEquals("FULL", userData.getString(Defines.Jsonkey.Consumer_Protection_Attribution_Level.getKey()))
        assertFalse("Must not leak to top level in V2", v2Request.getPost().has(Defines.Jsonkey.Consumer_Protection_Attribution_Level.getKey()))
    }

    @Test
    fun `attribution level is no-op when not initialized`() {
        `when`(mockPrefHelper.isAttributionLevelInitialized()).thenReturn(false)

        v1Request.callAddConsumerProtectionAttributionLevel()
        v2Request.callAddConsumerProtectionAttributionLevel()

        assertFalse(v1Request.getPost().has(Defines.Jsonkey.Consumer_Protection_Attribution_Level.getKey()))
        assertFalse(v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey()).has(Defines.Jsonkey.Consumer_Protection_Attribution_Level.getKey()))
    }

    // ── updateLimitFacebookTracking ───────────────────────────────────────────

    @Test
    fun `limit FB tracking writes to top level for V1`() {
        `when`(mockPrefHelper.isAppTrackingLimited()).thenReturn(true)

        v1Request.callUpdateLimitFacebookTracking()

        assertTrue(v1Request.getPost().getBoolean(Defines.Jsonkey.limitFacebookTracking.getKey()))
        assertFalse(v1Request.getPost().has(Defines.Jsonkey.UserData.getKey()))
    }

    @Test
    fun `limit FB tracking writes to user_data for V2`() {
        `when`(mockPrefHelper.isAppTrackingLimited()).thenReturn(true)

        v2Request.callUpdateLimitFacebookTracking()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertTrue(userData.getBoolean(Defines.Jsonkey.limitFacebookTracking.getKey()))
        assertFalse(v2Request.getPost().has(Defines.Jsonkey.limitFacebookTracking.getKey()))
    }

    @Test
    fun `limit FB tracking is no-op when false`() {
        `when`(mockPrefHelper.isAppTrackingLimited()).thenReturn(false)

        v1Request.callUpdateLimitFacebookTracking()
        v2Request.callUpdateLimitFacebookTracking()

        assertFalse(v1Request.getPost().has(Defines.Jsonkey.limitFacebookTracking.getKey()))
        assertFalse(v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey()).has(Defines.Jsonkey.limitFacebookTracking.getKey()))
    }

    // ── updateDisableAdNetworkCallouts ────────────────────────────────────────

    @Test
    fun `disable ad callouts writes to top level for V1`() {
        `when`(mockPrefHelper.getAdNetworkCalloutsDisabled()).thenReturn(true)

        v1Request.callUpdateDisableAdNetworkCallouts()

        assertTrue(v1Request.getPost().getBoolean(Defines.Jsonkey.DisableAdNetworkCallouts.getKey()))
        assertFalse(v1Request.getPost().has(Defines.Jsonkey.UserData.getKey()))
    }

    @Test
    fun `disable ad callouts writes to user_data for V2`() {
        `when`(mockPrefHelper.getAdNetworkCalloutsDisabled()).thenReturn(true)

        v2Request.callUpdateDisableAdNetworkCallouts()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertTrue(userData.getBoolean(Defines.Jsonkey.DisableAdNetworkCallouts.getKey()))
        assertFalse(v2Request.getPost().has(Defines.Jsonkey.DisableAdNetworkCallouts.getKey()))
    }

    @Test
    fun `disable ad callouts is no-op when false`() {
        `when`(mockPrefHelper.getAdNetworkCalloutsDisabled()).thenReturn(false)

        v1Request.callUpdateDisableAdNetworkCallouts()
        v2Request.callUpdateDisableAdNetworkCallouts()

        assertFalse(v1Request.getPost().has(Defines.Jsonkey.DisableAdNetworkCallouts.getKey()))
        assertFalse(v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey()).has(Defines.Jsonkey.DisableAdNetworkCallouts.getKey()))
    }

    // ── updateGAdsParams ──────────────────────────────────────────────────────

    @Test
    fun `updateGAdsParams writes lat_val and google_advertising_id to top level for V1`() {
        `when`(mockSystemObserver.getLATVal()).thenReturn(0)
        `when`(mockSystemObserver.getAID()).thenReturn("test-gaid-1234")
        `when`(mockPrefHelper.getConsumerProtectionAttributionLevel()).thenReturn(Defines.BranchAttributionLevel.FULL)
        `when`(mockPrefHelper.isAttributionLevelInitialized()).thenReturn(true)

        v1Request.updateGAdsParams()

        val params = v1Request.getPost()
        assertEquals(0, params.getInt(Defines.Jsonkey.LATVal.getKey()))
        assertEquals("test-gaid-1234", params.getString(Defines.Jsonkey.GoogleAdvertisingID.getKey()))
        assertFalse("V1 must not use aaid key", params.has(Defines.Jsonkey.AAID.getKey()))
        assertFalse("V1 must not use limit_ad_tracking key", params.has(Defines.Jsonkey.LimitedAdTracking.getKey()))
    }

    @Test
    fun `updateGAdsParams writes limit_ad_tracking and aaid to user_data for V2`() {
        `when`(mockSystemObserver.getLATVal()).thenReturn(0)
        `when`(mockSystemObserver.getAID()).thenReturn("test-gaid-5678")
        `when`(mockPrefHelper.getConsumerProtectionAttributionLevel()).thenReturn(Defines.BranchAttributionLevel.FULL)
        `when`(mockPrefHelper.isAttributionLevelInitialized()).thenReturn(true)

        v2Request.updateGAdsParams()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertEquals(0, userData.getInt(Defines.Jsonkey.LimitedAdTracking.getKey()))
        assertEquals("test-gaid-5678", userData.getString(Defines.Jsonkey.AAID.getKey()))
        assertFalse("V2 must not use lat_val key", userData.has(Defines.Jsonkey.LATVal.getKey()))
        assertFalse("V2 must not use google_advertising_id key", userData.has(Defines.Jsonkey.GoogleAdvertisingID.getKey()))
        // Must not leak into top-level params
        assertFalse(v2Request.getPost().has(Defines.Jsonkey.LimitedAdTracking.getKey()))
        assertFalse(v2Request.getPost().has(Defines.Jsonkey.AAID.getKey()))
    }

    @Test
    fun `updateGAdsParams sets unidentified_device at top level for V1 when no ad id`() {
        `when`(mockSystemObserver.getLATVal()).thenReturn(1)
        `when`(mockSystemObserver.getAID()).thenReturn("")

        v1Request.updateGAdsParams()

        assertTrue(v1Request.getPost().getBoolean(Defines.Jsonkey.UnidentifiedDevice.getKey()))
    }

    @Test
    fun `updateGAdsParams sets unidentified_device inside user_data for V2 when no ad id`() {
        `when`(mockSystemObserver.getLATVal()).thenReturn(1)
        `when`(mockSystemObserver.getAID()).thenReturn("")

        v2Request.updateGAdsParams()

        val userData = v2Request.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey())
        assertTrue(userData.getBoolean(Defines.Jsonkey.UnidentifiedDevice.getKey()))
        assertFalse(v2Request.getPost().has(Defines.Jsonkey.UnidentifiedDevice.getKey()))
    }

}

// ─────────────────────────────────────────────────────────────────────────────
// Concrete stubs — real ServerRequest subclasses that:
//   1. Accept a pre-built PrefHelper mock injected via reflection
//   2. Expose package-private methods for direct invocation in tests
//   3. Pre-populate params_ with a user_data block for V2
// ─────────────────────────────────────────────────────────────────────────────

private abstract class StubServerRequest(
    context: android.content.Context,
    mockPrefHelper: PrefHelper,
    path: Defines.RequestPath
) : ServerRequest(path, JSONObject(), context) {

    init {
        // Inject the mock PrefHelper, bypassing the Context-dependent getInstance() call.
        val field = ServerRequest::class.java.getDeclaredField("prefHelper_")
        field.isAccessible = true
        field.set(this, mockPrefHelper)
    }

    // Expose package-private methods so the test class (different package) can invoke them.
    fun callAddConsumerProtectionAttributionLevel() = addConsumerProtectionAttributionLevel()
    fun callUpdateLimitFacebookTracking() = updateLimitFacebookTracking()
    fun callUpdateDisableAdNetworkCallouts() = updateDisableAdNetworkCallouts()

    // Satisfy abstract contract — not under test.
    override fun handleErrors(context: android.content.Context) = false
    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {}
    override fun handleFailure(statusCode: Int, causeMsg: String) {}
    override fun isGetRequest() = false
    override fun clearCallbacks() {}
}

// Expose private methods via subclass in the same package
private fun ServerRequest.addConsumerProtectionAttributionLevel() {
    val m = ServerRequest::class.java.getDeclaredMethod("addConsumerProtectionAttributionLevel")
    m.isAccessible = true
    m.invoke(this)
}

private fun ServerRequest.updateLimitFacebookTracking() {
    val m = ServerRequest::class.java.getDeclaredMethod("updateLimitFacebookTracking")
    m.isAccessible = true
    m.invoke(this)
}

private fun ServerRequest.updateDisableAdNetworkCallouts() {
    val m = ServerRequest::class.java.getDeclaredMethod("updateDisableAdNetworkCallouts")
    m.isAccessible = true
    m.invoke(this)
}

private class StubServerRequestV1(
    context: android.content.Context,
    mockPrefHelper: PrefHelper
) : StubServerRequest(context, mockPrefHelper, Defines.RequestPath.RegisterInstall) {
    override fun getBranchRemoteAPIVersion() = BRANCH_API_VERSION.V1
}

private class StubServerRequestV2(
    context: android.content.Context,
    mockPrefHelper: PrefHelper
) : StubServerRequest(context, mockPrefHelper, Defines.RequestPath.TrackStandardEvent) {
    init {
        // Mirror what setPost() does for V2: create the user_data sub-object up front.
        getPost().put(Defines.Jsonkey.UserData.getKey(), JSONObject())
    }

    override fun getBranchRemoteAPIVersion() = BRANCH_API_VERSION.V2
}

