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
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** The first launch after a click-through install, with attribution off. */
class ConsentOffInstallReferrerTest : BranchTestBase() {

    /** Records every POST. `/v3/deeplink` finds the link only when the request carries its click id. */
    private class RecordingRemote : BranchRemoteInterface() {
        val posts = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        @Volatile var failDeepLink = false

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            val path = url.orEmpty().substringAfter("branch.io/")
            val body = JSONObject(payload.toString())
            posts.add(path to body)
            return when (path) {
                "v3/deeplink" -> when {
                    failDeepLink -> BranchResponse(FAILED_BODY, 500)
                    body.optString("link_identifier") == CLICK_ID -> BranchResponse(LINK_BODY, 200)
                    else -> BranchResponse(ORGANIC_BODY, 200)
                }
                "v3/events/open" -> BranchResponse(OPEN_RESPONSE, 200)
                else -> BranchResponse("{}", 200)
            }
        }

        fun deepLinks() = posts.filter { it.first == "v3/deeplink" }.map { it.second }
        fun opens() = posts.filter { it.first == "v3/events/open" }.map { it.second }
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var remote: RecordingRemote
    private val adIdReads = AtomicInteger()
    private val referrerReads = AtomicInteger()
    @Volatile private var playReferrer: String? = REFERRER_WITH_CLICK_ID
    @Volatile private var referrerReadRelease: CountDownLatch? = null

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.INTERNET)
        resetReferrerClickId()
        remote = RecordingRemote()
        startApp()
        firstInstall()
    }

    @After
    override fun tearDownBase() {
        setProcessState(Lifecycle.State.CREATED)
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
        resetReferrerClickId()
    }

    @Test
    fun firstLaunchAtNone_referrerHasAClickId_deepLinkSendsIt_andTheCallbackGetsTheLink() {
        val (params, error) = requestDeepLink(null)

        val deepLink = remote.deepLinks().single()
        assertEquals(CLICK_ID, deepLink.optString("link_identifier"))
        assertTrue(deepLink.optBoolean("tracking_disabled"))
        assertNull(error)
        assertEquals(REFERRING_LINK, params?.optString("~referring_link"))
    }

    @Test
    fun firstLaunchAtNone_referrerReadFinishesAfterTheCallReturns_deepLinkStillSendsTheClickId() {
        val release = CountDownLatch(1)
        referrerReadRelease = release

        requestDeepLinkWith {
            Branch.getInstance().requestDeepLinkData(null as Uri?, it)
            release.countDown()
        }

        assertEquals(CLICK_ID, remote.deepLinks().single().optString("link_identifier"))
    }

    @Test
    fun firstLaunchAtNone_referrerHasAClickId_sendsNoAdIdAndNoOtherReferrerField() {
        requestDeepLink(null)

        val deepLink = remote.deepLinks().single()
        val sent = ATTRIBUTION_FIELDS.filter { deepLink.has(it) }
        assertTrue("sent with attribution off: $sent in $deepLink", sent.isEmpty())
        assertEquals("the ad ID was read with attribution off", 0, adIdReads.get())
    }

    @Test
    fun firstLaunchAtNone_referrerClickId_sendsTheSameFieldsAsTheClickIdInALink() {
        playReferrer = null
        requestDeepLink(Uri.parse("myapp://open?link_click_id=$CLICK_ID"))
        val fromLink = remote.deepLinks().single()
        background()
        PrefHelper.getInstance(context).installParams = PrefHelper.NO_STRING_VALUE

        playReferrer = REFERRER_WITH_CLICK_ID
        startApp()
        requestDeepLink(null)

        val fromReferrer = remote.deepLinks().drop(1).single()
        assertEquals(fromLink.keys().asSequence().toSortedSet(), fromReferrer.keys().asSequence().toSortedSet())
        assertEquals(fromLink.optString("link_identifier"), fromReferrer.optString("link_identifier"))
    }

    @Test
    fun firstLaunchAtNone_referrerHasAClickId_thenOptInFull_sendsOneOpenWithTheLink() {
        requestDeepLink(null)

        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL)

        assertOneOpenWithTheLink(settledOpens())
    }

    @Test
    fun firstLaunchAtNone_referrerHasAClickId_thenOptInReduced_sendsOneOpenWithTheLink() {
        requestDeepLink(null)

        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.REDUCED)

        assertOneOpenWithTheLink(settledOpens())
    }

    @Test
    fun firstLaunchAtNone_fromTheLauncher_deepLinkSendsTheReferrerClickId() {
        val activity = Robolectric.buildActivity(
            Activity::class.java,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        ).get()

        requestDeepLinkWith { Branch.getInstance().requestDeepLinkData(activity, it) }

        assertEquals(CLICK_ID, remote.deepLinks().single().optString("link_identifier"))
    }

    @Test
    fun referrerLinkDelivered_nextLaunchAtNone_doesNotSendTheClickIdAgain() {
        requestDeepLink(null)
        assertEquals(CLICK_ID, remote.deepLinks().single().optString("link_identifier"))
        background()

        startApp()
        val (params, _) = requestDeepLink(null)

        val again = remote.deepLinks().drop(1).filter { it.has("link_identifier") }
        assertTrue("the install's click id was sent again: $again", again.isEmpty())
        assertFalse("the install's link was delivered again: $params", params?.optBoolean("+clicked_branch_link") == true)
    }

    @Test
    fun deepLinkFailedOnTheFirstLaunchAtNone_nextLaunchSendsTheClickIdAgain() {
        remote.failDeepLink = true
        requestDeepLink(null)
        remote.failDeepLink = false
        background()

        startApp()
        val (params, _) = requestDeepLink(null)

        assertEquals(listOf(CLICK_ID, CLICK_ID), remote.deepLinks().map { it.optString("link_identifier") })
        assertEquals(REFERRING_LINK, params?.optString("~referring_link"))
    }

    @Test
    fun firstLaunchAtNone_referrerWithoutAClickId_sendsNoDeepLink() {
        playReferrer = ORGANIC_REFERRER

        requestDeepLink(null)

        assertEquals(0, remote.deepLinks().size)
    }

    @Test
    fun returningUserAtNone_doesNotReadTheReferrer() {
        PrefHelper.getInstance(context).setRandomizedBundleToken("rbt")

        requestDeepLink(null)

        assertEquals(0, referrerReads.get())
        assertEquals(0, remote.deepLinks().size)
    }

    @Test
    fun firstLaunchAtFull_stillSendsTheReferrerFieldsAndReadsTheAdId() {
        startApp(Defines.BranchAttributionLevel.FULL)

        requestDeepLink(null)

        val deepLink = remote.deepLinks().single()
        assertEquals(CLICK_ID, deepLink.optString("link_identifier"))
        assertEquals(REFERRER_WITH_CLICK_ID, deepLink.optString("install_referrer_extras"))
        assertEquals("PlayStore", deepLink.optString("app_store"))
        assertFalse(deepLink.optBoolean("tracking_disabled"))
        assertEquals(1, adIdReads.get())
    }

    /** A cold start: a new Branch instance over the prefs that earlier launches saved. */
    private fun startApp(level: Defines.BranchAttributionLevel = Defines.BranchAttributionLevel.NONE) {
        Branch.shutDown()
        Branch._userAgentString = "test-agent"
        Branch.initialize(
            context,
            BranchConfiguration.Builder("key_live_test123")
                .setRemoteInterface(remote)
                .setAttributionLevel(level)
                .build()
        )
        useFakeReaders()
        setProcessState(Lifecycle.State.RESUMED)
    }

    private fun firstInstall() {
        val prefs = PrefHelper.getInstance(context)
        prefs.setRandomizedBundleToken(PrefHelper.NO_STRING_VALUE)
        prefs.setRandomizedDeviceToken(PrefHelper.NO_STRING_VALUE)
        prefs.setSessionID(PrefHelper.NO_STRING_VALUE)
        prefs.installParams = PrefHelper.NO_STRING_VALUE
    }

    /**
     * The ad ID read finishes at once. The install referrer read parses playReferrer as Play returns it,
     * after referrerReadRelease opens when it is set.
     */
    private fun useFakeReaders() {
        val observer = object : SystemObserver() {
            override fun fetchAdId(context: Context, callback: AdsParamsFetchEvents?) {
                adIdReads.incrementAndGet()
                thread {
                    setGAID("38400000-8cf0-11bd-b23e-10b96e40000d")
                    callback?.onAdsParamsFetchFinished()
                }
            }

            override fun fetchInstallReferrer(context: Context, callback: InstallReferrerFetchEvents?) {
                referrerReads.incrementAndGet()
                thread {
                    referrerReadRelease?.await(10, TimeUnit.SECONDS)
                    playReferrer?.let {
                        AppStoreReferrer.processReferrerInfo(
                            context, it, CLICK_TS, INSTALL_BEGIN_TS, Defines.Jsonkey.Google_Play_Store.key,
                            false, INSTALL_BEGIN_TS + 1, CLICK_TS + 1
                        )
                    }
                    callback?.onInstallReferrersFinished()
                }
            }
        }
        DeviceInfo::class.java.getDeclaredField("systemObserver_").apply { isAccessible = true }
            .set(DeviceInfo.getInstance(), observer)
    }

    private fun resetReferrerClickId() {
        AppStoreReferrer::class.java.getDeclaredField("installID_").apply { isAccessible = true }
            .set(null, PrefHelper.NO_STRING_VALUE)
    }

    private fun requestDeepLink(uri: Uri?): Pair<JSONObject?, BranchError?> =
        requestDeepLinkWith { Branch.getInstance().requestDeepLinkData(uri, it) }

    /** Calls requestDeepLinkData, waits for its callback and for its request to leave the queue. */
    private fun requestDeepLinkWith(start: (Branch.BranchReferralInitListener) -> Unit): Pair<JSONObject?, BranchError?> {
        val result = AtomicReference<Pair<JSONObject?, BranchError?>>()
        val done = CountDownLatch(1)
        start(Branch.BranchReferralInitListener { params, error -> result.set(params to error); done.countDown() })
        waitUntil("requestDeepLinkData never called back") { done.count == 0L }
        waitUntil("the request never left the queue") { !Branch.getInstance().requestQueue_.containsDeepLinkOrOpen() }
        return result.get()
    }

    private fun background() {
        setProcessState(Lifecycle.State.CREATED)
        waitUntil("the request never left the queue") { !Branch.getInstance().requestQueue_.containsDeepLinkOrOpen() }
    }

    private fun assertOneOpenWithTheLink(opens: List<JSONObject>) {
        assertEquals("one open: $opens", 1, opens.size)
        assertEquals(REFERRING_LINK, opens[0].optJSONObject("link_data")?.optString("~referring_link"))
        assertFalse(opens[0].optBoolean("tracking_disabled"))
    }

    /** Waits for the first open, then long enough that a second one would have been sent too. */
    private fun settledOpens(): List<JSONObject> {
        val deadline = System.currentTimeMillis() + 10_000
        while (remote.opens().isEmpty() && System.currentTimeMillis() < deadline) idle()
        val settle = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < settle) idle()
        return remote.opens()
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            assertTrue(message, System.currentTimeMillis() < deadline)
            idle()
        }
    }

    private fun idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(10)
    }

    private fun setProcessState(state: Lifecycle.State) {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState = state
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private companion object {
        const val CLICK_ID = "1234567890123456789"
        const val REFERRING_LINK = "https://example.app.link/first-link"
        const val REFERRER_WITH_CLICK_ID = "link_click_id=$CLICK_ID&utm_source=google-play&utm_medium=cpc"
        const val ORGANIC_REFERRER = "utm_source=google-play&utm_medium=organic"
        const val CLICK_TS = 1_794_900_000L
        const val INSTALL_BEGIN_TS = 1_794_900_030L

        /** Device and install referrer fields that attribution needs. */
        val ATTRIBUTION_FIELDS = listOf(
            "install_referrer_extras", "app_store", "is_meta_ct", "link_click_id", "google_search_install_referrer",
            "clicked_referrer_ts", "install_begin_ts", "clicked_referrer_server_ts", "install_begin_server_ts",
            "advertising_ids", "google_advertising_id", "aaid", "hardware_id",
            "randomized_device_token", "randomized_bundle_token"
        )

        val LINK_BODY: String = JSONObject()
            .put("data", JSONObject()
                .put("+clicked_branch_link", true)
                .put("~referring_link", REFERRING_LINK)
                .put("~campaign", "summer")
                .toString())
            .put("randomized_device_token", "rdt")
            .toString()
        const val ORGANIC_BODY =
            """{"data":"{\"+clicked_branch_link\":false,\"+is_first_session\":false}","randomized_device_token":"rdt"}"""
        const val FAILED_BODY = """{"error":"boom"}"""
        const val OPEN_RESPONSE =
            """{"randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""
    }
}
