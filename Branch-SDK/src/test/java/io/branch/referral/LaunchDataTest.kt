package io.branch.referral

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import io.branch.data.InstallReferrerResult
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * EMT-4480: the launch requests carry the advertising ID and the store install data, like
 * master's install and open did, and the install data rides only the launch it belongs to.
 */
class LaunchDataTest : BranchTestBase() {

    /** Records every `/v3/deeplink` and `/v3/events/open` body, with the time it arrived. */
    private class RecordingRemote : BranchRemoteInterface() {
        val deepLinks = CopyOnWriteArrayList<JSONObject>()
        val opens = CopyOnWriteArrayList<JSONObject>()
        @Volatile var deepLinkBody = ORGANIC_BODY

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse = when {
            url.orEmpty().endsWith("v3/deeplink") -> {
                deepLinks.add(JSONObject(payload.toString()))
                BranchResponse(deepLinkBody, 200)
            }
            url.orEmpty().endsWith("v3/events/open") -> {
                opens.add(JSONObject(payload.toString()))
                BranchResponse(OPEN_RESPONSE, 200)
            }
            else -> BranchResponse("{}", 200)
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = PrefHelper.getInstance(context)
    private lateinit var remote: RecordingRemote

    private val adIdReads = AtomicInteger()
    private val referrerReads = AtomicInteger()
    @Volatile private var adId: Pair<Int, String?>? = null
    @Volatile private var referrer: InstallReferrerResult? = null
    @Volatile private var readDelayMs = 0L
    @Volatile private var hangAdIdRead = false
    @Volatile private var hangReferrerRead = false

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.shutDown()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())
        Branch._userAgentString = "test-agent"
        remote = RecordingRemote()
        Branch.getInstance().setBranchRemoteInterface(remote)
        useFakeReaders()
    }

    @After
    override fun tearDownBase() {
        setProcessState(Lifecycle.State.CREATED)
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
    }

    @Test
    fun kotlinCanLookUpADeferredDeepLink() {
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        assertEquals(1, remote.deepLinks.size)
    }

    @Test
    fun tappedLinkClickId_isNotReplacedBySavedReferrerOne() {
        prefs.setLinkClickIdentifier(REFERRER_CLICK_ID)

        Branch.getInstance().requestDeepLinkData(Uri.parse("https://example.app.link/abc?link_click_id=$TAPPED_CLICK_ID")) { _, _ -> }
        awaitOpens(1)

        assertEquals(TAPPED_CLICK_ID, remote.deepLinks[0].optString(Defines.Jsonkey.LinkIdentifier.key))
    }

    @Test
    fun savedInstallData_isSentOnItsLaunch_andNotAfterTheOpenSucceeds() {
        prefs.setAppStoreReferrer(PLAY_REFERRER)
        prefs.setLinkClickIdentifier(REFERRER_CLICK_ID)

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(2)

        assertEquals(PLAY_REFERRER, remote.deepLinks[0].optString(REFERRER_KEY))
        assertEquals(REFERRER_CLICK_ID, remote.deepLinks[0].optString(LINK_ID_KEY))
        assertEquals(PLAY_REFERRER, remote.opens[0].optString(REFERRER_KEY))
        assertFalse("the next launch must not resend the install referrer", remote.deepLinks[1].has(REFERRER_KEY))
        assertFalse("the next launch must not resend the referrer's link id", remote.deepLinks[1].has(LINK_ID_KEY))
        assertFalse("the next launch must not resend the install referrer", remote.opens[1].has(REFERRER_KEY))
    }

    @Test
    fun firstLaunch_bothRequestsWaitForTheInstallReferrer_andCarryIt() {
        referrer = playReferrer()
        readDelayMs = 300

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        val deepLink = remote.deepLinks[0]
        assertEquals(PLAY_REFERRER_WITH_CLICK, deepLink.optString(REFERRER_KEY))
        assertEquals(REFERRER_CLICK_ID, deepLink.optString(LINK_ID_KEY))
        assertEquals(Defines.Jsonkey.Google_Play_Store.key, deepLink.optString(Defines.Jsonkey.App_Store.key))
        assertEquals(PLAY_REFERRER_WITH_CLICK, remote.opens[0].optString(REFERRER_KEY))
    }

    @Test
    fun bothLaunchRequests_waitForTheAdvertisingId_andCarryIt() {
        adId = Pair(0, GAID)
        readDelayMs = 300

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        assertEquals(GAID, remote.deepLinks[0].optString(Defines.Jsonkey.GoogleAdvertisingID.key))
        assertEquals(GAID, remote.opens[0].optString(Defines.Jsonkey.GoogleAdvertisingID.key))
    }

    /** A background while a launch still waits on its reads must not let the next launch wipe that launch's link. */
    @Test
    fun backgroundWhileALaunchWaitsOnItsReads_thenNextLaunch_firstLaunchKeepsItsLink() {
        adId = Pair(0, GAID)
        readDelayMs = 1_000
        val link = "https://example.app.link/abc"
        setProcessState(Lifecycle.State.RESUMED)
        Branch.getInstance().requestDeepLinkData(Uri.parse(link)) { _, _ -> }

        setProcessState(Lifecycle.State.CREATED)
        setProcessState(Lifecycle.State.RESUMED)
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitDeepLinks(1)

        assertEquals("the first launch's link was wiped before it was sent", link, remote.deepLinks[0].optString(Defines.Jsonkey.AndroidAppLinkURL.key))
    }

    @Test
    fun returningUser_neverReadsTheInstallReferrer() {
        prefs.randomizedBundleToken = "rbt"
        hangReferrerRead = true

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        assertEquals(0, referrerReads.get())
    }

    @Test
    fun consentNone_readsNothing_untilOptIn_thenTheHeldOpenCarriesTheData() {
        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.NONE)
        referrer = playReferrer()
        adId = Pair(0, GAID)

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        settle(500)
        assertEquals("nothing is read while attribution is off", 0, adIdReads.get() + referrerReads.get())
        assertEquals("the open is held while attribution is off", 0, remote.opens.size)

        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL)
        awaitOpens(1)

        assertEquals(PLAY_REFERRER_WITH_CLICK, remote.opens[0].optString(REFERRER_KEY))
        assertEquals(GAID, remote.opens[0].optString(Defines.Jsonkey.GoogleAdvertisingID.key))
    }

    @Test
    fun installFields_rideBothFirstLaunchRequests_andNoLaterOnes() {
        referrer = playReferrer()
        prefs.addInstallMetadata(INSTALL_METADATA_KEY, "partner-a")

        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(2)

        for (install in listOf(remote.deepLinks[0], remote.opens[0])) {
            assertEquals(1_700_000_000L, install.optLong(Defines.Jsonkey.ClickedReferrerTimeStamp.key))
            assertEquals(1_700_000_100L, install.optLong(Defines.Jsonkey.InstallBeginTimeStamp.key))
            assertEquals(1_700_000_001L, install.optLong(Defines.Jsonkey.ClickedReferrerServerTimeStamp.key))
            assertEquals(1_700_000_101L, install.optLong(Defines.Jsonkey.InstallBeginServerTimeStamp.key))
            assertEquals(REFERRER_CLICK_ID, install.optString(Defines.Jsonkey.LinkClickID.key))
            assertTrue(install.has(Defines.Jsonkey.OperationalMetrics.key))
            assertEquals("partner-a", install.optString(INSTALL_METADATA_KEY))
        }
        for (later in listOf(remote.deepLinks[1], remote.opens[1])) {
            for (key in INSTALL_ONLY_KEYS + INSTALL_METADATA_KEY) {
                assertFalse("a later launch must not send $key", later.has(key))
            }
        }
    }

    @Test
    fun bothLaunchRequests_carryTheDeviceDetails() {
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        for (request in listOf(remote.deepLinks[0], remote.opens[0])) {
            assertTrue(request.has(Defines.Jsonkey.CPUType.key))
            assertTrue(request.has(Defines.Jsonkey.Locale.key))
            assertTrue(request.has(Defines.Jsonkey.OSVersionAndroid.key))
        }
    }

    @Test
    fun gclidInTheTappedLink_ridesTheOpen() {
        Branch.getInstance().requestDeepLinkData(Uri.parse("https://example.app.link/abc?gclid=$GCLID")) { _, _ -> }
        awaitOpens(1)

        assertEquals(GCLID, remote.opens[0].optString(Defines.Jsonkey.Gclid.key))
        assertTrue(remote.opens[0].optBoolean(Defines.Jsonkey.IsDeeplinkGclid.key))
    }

    @Test
    fun installFromALink_keepsItsParamsAsTheFirstReferringParams() {
        remote.deepLinkBody = linkBody("summer")
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)
        remote.deepLinkBody = linkBody("winter")
        Branch.getInstance().requestDeepLinkData(Uri.parse("https://example.app.link/winter")) { _, _ -> }
        awaitOpens(2)

        assertEquals("summer", Branch.getInstance().firstReferringParams.optString("~campaign"))
        assertEquals("winter", Branch.getInstance().latestReferringParams.optString("~campaign"))
    }

    @Test
    fun organicInstall_hasNoFirstReferringParams() {
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        assertEquals(0, Branch.getInstance().firstReferringParams.length())
    }

    @Test
    fun openResponse_setsTheUserUrl() {
        Branch.getInstance().requestDeepLinkData(null) { _, _ -> }
        awaitOpens(1)

        assertEquals(USER_URL, prefs.userURL)
    }

    @Test
    fun activityLaunch_sendsWhereItCameFrom_onBothRequests() {
        Branch.getInstance().requestDeepLinkData(activity(Intent(Intent.ACTION_MAIN).putExtra(Intent.EXTRA_REFERRER, Uri.parse(PLAY_STORE_REFERRER)))) { _, _ -> }
        awaitOpens(1)

        assertEquals(PLAY_STORE_REFERRER, remote.deepLinks[0].optString(Defines.Jsonkey.InitialReferrer.key))
        assertEquals(PLAY_STORE_REFERRER, remote.opens[0].optString(Defines.Jsonkey.InitialReferrer.key))
    }

    @Test
    fun activityLaunch_sendsTheIntentLinkAndContext_onBothRequests() {
        val link = "https://example.app.link/abc"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).putExtra("branch_intent", true)

        Branch.getInstance().requestDeepLinkData(activity(intent)) { _, _ -> }
        awaitOpens(1)

        for (request in listOf(remote.deepLinks[0], remote.opens[0])) {
            assertEquals("sent: $request", link, request.optString(Defines.Jsonkey.AndroidAppLinkURL.key))
            assertEquals(link, request.optString(Defines.Jsonkey.External_Intent_URI.key))
            assertTrue(JSONObject(request.optString(Defines.Jsonkey.External_Intent_Extra.key)).optBoolean("branch_intent"))
        }
    }

    @Test
    fun activityLaunch_withAClickId_sendsItAsTheLinkIdentifier() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.app.link/abc?link_click_id=$TAPPED_CLICK_ID"))

        Branch.getInstance().requestDeepLinkData(activity(intent)) { _, _ -> }
        awaitOpens(1)

        assertEquals(TAPPED_CLICK_ID, remote.deepLinks[0].optString(LINK_ID_KEY))
    }

    @Test
    fun activityLaunch_fromAPushNotification_sendsThePushLink() {
        val intent = Intent(Intent.ACTION_MAIN).putExtra(Defines.IntentKeys.BranchURI.key, PUSH_LINK)

        Branch.getInstance().requestDeepLinkData(activity(intent)) { _, _ -> }
        awaitOpens(1)

        assertEquals(PUSH_LINK, remote.deepLinks[0].optString(Defines.Jsonkey.AndroidPushIdentifier.key))
    }

    @Test
    fun activityLaunch_fromRecents_resolvesNoLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.app.link/abc"))
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        Branch.getInstance().requestDeepLinkData(activity(intent)) { _, _ -> }
        awaitOpens(1)

        assertFalse(remote.deepLinks[0].has(Defines.Jsonkey.AndroidAppLinkURL.key))
    }

    @Test
    fun activityLaunch_withASkippedUrl_neverSendsIt() {
        val oauthCallback = "https://example.com/callback?access_token=$SECRET"

        Branch.getInstance().requestDeepLinkData(activity(Intent(Intent.ACTION_VIEW, Uri.parse(oauthCallback)))) { _, _ -> }
        awaitOpens(1)

        assertFalse(remote.deepLinks[0].toString().contains(SECRET))
        assertFalse(remote.opens[0].toString().contains(SECRET))
    }

    @Test
    fun activityLaunch_sameIntentTwice_resolvesItsLinkOnce() {
        val launched = activity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.app.link/abc")))

        Branch.getInstance().requestDeepLinkData(launched) { _, _ -> }
        awaitOpens(1)
        Branch.getInstance().requestDeepLinkData(launched) { _, _ -> }
        awaitOpens(2)

        assertTrue("deep link sent: ${remote.deepLinks[0]}", remote.deepLinks[0].has(Defines.Jsonkey.AndroidAppLinkURL.key))
        assertFalse("an intent already handled must not be resolved again", remote.deepLinks[1].has(Defines.Jsonkey.AndroidAppLinkURL.key))
    }

    private fun activity(intent: Intent): Activity = Robolectric.buildActivity(Activity::class.java, intent).get()

    private fun linkBody(campaign: String) =
        """{"data":"{\"+clicked_branch_link\":true,\"~campaign\":\"$campaign\"}","randomized_device_token":"rdt"}"""

    /** Replaces the device's ad ID and install referrer reads with ones the test controls. */
    private fun useFakeReaders() {
        val observer = object : SystemObserver() {
            override fun fetchAdId(context: Context, callback: AdsParamsFetchEvents?) {
                adIdReads.incrementAndGet()
                thread {
                    if (hangAdIdRead) return@thread
                    Thread.sleep(readDelayMs)
                    adId?.let {
                        setLAT(it.first)
                        setGAID(it.second)
                    }
                    callback?.onAdsParamsFetchFinished()
                }
            }

            override fun fetchInstallReferrer(context: Context, callback: InstallReferrerFetchEvents?) {
                referrerReads.incrementAndGet()
                thread {
                    if (hangReferrerRead) return@thread
                    Thread.sleep(readDelayMs)
                    referrer?.let {
                        AppStoreReferrer.processReferrerInfo(
                            context, it.installReferrer, it.referrerClickTimestampSeconds, it.installBeginTimestampSeconds,
                            it.appStore, it.isClickThrough, it.installBeginTimestampServerSeconds, it.referrerClickTimestampServerSeconds
                        )
                    }
                    callback?.onInstallReferrersFinished()
                }
            }
        }
        DeviceInfo::class.java.getDeclaredField("systemObserver_").apply { isAccessible = true }.set(DeviceInfo.getInstance(), observer)
    }

    private fun playReferrer() =
        InstallReferrerResult(Defines.Jsonkey.Google_Play_Store.key, 1_700_000_100, PLAY_REFERRER_WITH_CLICK, 1_700_000_000, 1_700_000_101, 1_700_000_001)

    private fun setProcessState(state: Lifecycle.State) {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState = state
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitOpens(count: Int) = awaitCount(count) { remote.opens.size }

    private fun awaitDeepLinks(count: Int) = awaitCount(count) { remote.deepLinks.size }

    private fun awaitCount(count: Int, current: () -> Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (current() < count && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("expected $count, got ${current()}", current() >= count)
        settle(300)
    }

    private fun settle(ms: Long) {
        val deadline = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private companion object {
        const val TAPPED_CLICK_ID = "111111"
        const val REFERRER_CLICK_ID = "999999"
        const val GAID = "38400000-8cf0-11bd-b23e-10b96e40000d"
        const val GCLID = "gclid-test-123"
        const val PLAY_STORE_REFERRER = "android-app://com.android.vending"
        const val PUSH_LINK = "https://example.app.link/push1"
        const val SECRET = "s3cr3t-token"
        const val PLAY_REFERRER = "utm_source=google-play&utm_medium=organic"
        const val PLAY_REFERRER_WITH_CLICK = "link_click_id=$REFERRER_CLICK_ID&utm_source=google-play"
        val REFERRER_KEY = Defines.Jsonkey.GooglePlayInstallReferrer.key
        val LINK_ID_KEY = Defines.Jsonkey.LinkIdentifier.key
        const val INSTALL_METADATA_KEY = "install_partner"
        val INSTALL_ONLY_KEYS = listOf(
            Defines.Jsonkey.ClickedReferrerTimeStamp.key,
            Defines.Jsonkey.InstallBeginTimeStamp.key,
            Defines.Jsonkey.ClickedReferrerServerTimeStamp.key,
            Defines.Jsonkey.InstallBeginServerTimeStamp.key,
            Defines.Jsonkey.LinkClickID.key,
            Defines.Jsonkey.OperationalMetrics.key
        )
        const val ORGANIC_BODY =
            """{"data":"{\"+clicked_branch_link\":false,\"+is_first_session\":false}","randomized_device_token":"rdt"}"""
        const val USER_URL = "https://example.app.link?%24randomized_bundle_token=rbt"
        const val OPEN_RESPONSE =
            """{"link":"$USER_URL","randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""
    }
}
