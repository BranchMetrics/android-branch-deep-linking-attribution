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
import io.branch.referral.util.BranchEvent
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** setAutomaticOpenEvents and Branch.sendOpen. */
class AutomaticOpenEventsTest : BranchTestBase() {

    /**
     * Records every POST in order. `/v3/deeplink` answers with link data only when the request
     * carries a link, as the server does. It can also be held, failed, or told to find no link.
     */
    private class RecordingRemote : BranchRemoteInterface() {
        val posts = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        val deepLinkEntered = Semaphore(0)
        @Volatile var holdDeepLink: CountDownLatch? = null
        @Volatile var failDeepLink = false
        @Volatile var noLinkFound = false

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            val path = url.orEmpty().substringAfter("branch.io/")
            val body = JSONObject(payload.toString())
            posts.add(path to body)
            return when (path) {
                "v3/deeplink" -> {
                    deepLinkEntered.release()
                    holdDeepLink?.await(10, TimeUnit.SECONDS)
                    when {
                        failDeepLink -> BranchResponse(FAILED_BODY, 500)
                        noLinkFound || !body.has("android_app_link_url") -> BranchResponse(ORGANIC_BODY, 200)
                        else -> BranchResponse(linkBody(body.getString("android_app_link_url")), 200)
                    }
                }
                "v3/events/open" -> BranchResponse(OPEN_RESPONSE, 200)
                else -> BranchResponse("{}", 200)
            }
        }

        fun opens() = posts.filter { it.first == "v3/events/open" }.map { it.second }
        fun deepLinks() = posts.filter { it.first == "v3/deeplink" }.map { it.second }
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val linkA: Uri get() = Uri.parse("https://example.app.link/a")
    private val linkB: Uri get() = Uri.parse("https://example.app.link/b")
    private lateinit var remote: RecordingRemote
    private val logs = CopyOnWriteArrayList<String>()
    private val adIdReads = AtomicInteger()
    private val holdNextAdIdRead = AtomicBoolean(false)
    private val adIdRelease = CountDownLatch(1)

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.INTERNET)
        remote = RecordingRemote()
        initialize(automaticOpenEvents = false)
    }

    @After
    override fun tearDownBase() {
        remote.holdDeepLink?.countDown()
        adIdRelease.countDown()
        setProcessState(Lifecycle.State.CREATED)
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
    }

    @Test
    fun deepLinkResponse_keepsTheOpenUntilSendOpen() {
        requestDeepLink(linkA)
        assertEquals("the open waits for sendOpen", 0, settledOpens(count = 0).size)

        assertTrue("the sendOpen callback never fired", sendOpen().await(10, TimeUnit.SECONDS))

        assertOneOpen(linkA)
    }

    @Test
    fun dataSetBeforeSendOpen_isOnTheOpen_notOnTheDeepLink() {
        requestDeepLink(linkA)
        Branch.getInstance().setRequestMetadata("\$partner_id", "p-123")
        Branch.getInstance().addFacebookPartnerParameterWithName("em", "a".repeat(64))

        sendOpen()

        val open = settledOpens().single()
        assertEquals("p-123", open.getJSONObject("metadata").getString("\$partner_id"))
        assertTrue("the partner parameter must be on the open: $open", open.toString().contains("a".repeat(64)))
        val deepLink = remote.deepLinks().single()
        assertFalse("the metadata was set after /v3/deeplink: $deepLink", deepLink.toString().contains("p-123"))
    }

    @Test
    fun failedDeepLink_thenSendOpen_sendsOneOpenWithoutLinkData() {
        remote.failDeepLink = true
        requestDeepLink(linkA)

        sendOpen()

        assertOneOpen(null)
    }

    @Test
    fun sendOpenBeforeTheResponse_sendsTheOpenWhenTheResponseArrives() {
        val release = startHeldDeepLink(linkA)
        val called = sendOpen()

        assertEquals("the callback waits for the response", 1L, called.count)
        release.countDown()

        assertTrue("the sendOpen callback never fired", called.await(10, TimeUnit.SECONDS))
        assertOneOpen(linkA)
    }

    @Test
    fun sendOpenBeforeRequestDeepLinkData_sendsTheOpenWithTheLink() {
        val called = sendOpen()
        assertEquals("nothing is sent before /v3/deeplink", 0, settledOpens(count = 0).size)

        requestDeepLink(linkA)

        assertTrue("the sendOpen callback never fired", called.await(10, TimeUnit.SECONDS))
        assertEquals(linkA.toString(), remote.deepLinks().single().optString("android_app_link_url"))
        assertOneOpen(linkA)
    }

    @Test
    fun secondLinkBeforeSendOpen_sendsOneOpenForTheNewerLink() {
        requestDeepLink(linkA)
        requestDeepLink(linkB)

        sendOpen()

        assertOneOpen(linkB)
    }

    @Test
    fun secondLink_sendOpenInItsCallback_sendsTheNewerLink() {
        requestDeepLink(linkA)
        val sent = CountDownLatch(1)

        Branch.getInstance().requestDeepLinkData(linkB) { _, _ ->
            Branch.getInstance().sendOpen { sent.countDown() }
        }

        assertTrue("the sendOpen callback never fired", sent.await(10, TimeUnit.SECONDS))
        assertOneOpen(linkB)
    }

    /** The newer response arrives while the kept open is pending, so it is not kept. */
    @Test
    fun rotation_sendOpenWhileTheRebuiltScreensDeepLinkWaits_sendsOneOpen() {
        setProcessState(Lifecycle.State.RESUMED)
        val activity = Robolectric.buildActivity(Activity::class.java, Intent(Intent.ACTION_VIEW, linkA)).get()
        requestDeepLink(activity)
        val release = startHeldDeepLink(activity)
        val called = sendOpen()

        release.countDown()
        assertTrue("the sendOpen callback never fired", called.await(10, TimeUnit.SECONDS))
        awaitNoDeepLink()
        setProcessState(Lifecycle.State.CREATED)

        assertEquals(linkA.toString(), remote.deepLinks()[1].optString("android_app_link_url"))
        assertOneOpen(linkA)
    }

    @Test
    fun secondDeepLinkFails_keepsTheFirstLink() {
        requestDeepLink(linkA)
        remote.failDeepLink = true
        requestDeepLink(linkB)

        sendOpen()

        assertOneOpen(linkA)
    }

    @Test
    fun secondResponseWithoutALink_keepsTheFirstLink() {
        requestDeepLink(linkA)
        remote.noLinkFound = true
        requestDeepLink(linkB)

        sendOpen()

        assertOneOpen(linkA)
    }

    @Test
    fun consentOff_optInBeforeSendOpen_sendsNothingUntilSendOpen() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        requestDeepLink(linkA)

        setLevel(Defines.BranchAttributionLevel.FULL)

        assertEquals("opt-in alone must not send the open", 0, settledOpens(count = 0).size)
        val reads = adIdReads.get()
        sendOpen()
        assertOneOpen(linkA)
        assertEquals("the open kept while attribution was off reads the ad ID", reads + 1, adIdReads.get())
    }

    @Test
    fun consentOff_sendOpenBeforeOptIn_sendsAtOptIn_andBothCallbacksFire() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        requestDeepLink(linkA)
        val app = sendOpen()
        assertEquals(0, settledOpens(count = 0).size)
        val optIn = CountDownLatch(1)

        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL) { _, _, _ ->
            optIn.countDown()
        }

        assertTrue("the app's sendOpen callback", app.await(10, TimeUnit.SECONDS))
        assertTrue("the opt-in callback", optIn.await(10, TimeUnit.SECONDS))
        assertOneOpen(linkA)
    }

    @Test
    fun consentOff_sendOpenThenOptInBeforeTheResponse_sendsOneOpenWithTheLink() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        val release = startHeldDeepLink(linkA)
        val app = sendOpen()
        setLevel(Defines.BranchAttributionLevel.FULL)

        release.countDown()

        assertTrue("the app's sendOpen callback", app.await(10, TimeUnit.SECONDS))
        assertOneOpen(linkA)
    }

    @Test
    fun consentTurnedOffWhileAnOpenIsKept_sendsAfterOptInAndSendOpen() {
        requestDeepLink(linkA)
        setLevel(Defines.BranchAttributionLevel.NONE)
        sendOpen()
        assertEquals(0, settledOpens(count = 0).size)

        setLevel(Defines.BranchAttributionLevel.FULL)

        assertOneOpen(linkA)
    }

    @Test
    fun consentOffAndOnWhileAnOpenIsKept_sendOpenReadsTheAdIdAgain() {
        requestDeepLink(linkA)
        setLevel(Defines.BranchAttributionLevel.NONE)
        setLevel(Defines.BranchAttributionLevel.FULL)
        val reads = adIdReads.get()

        sendOpen()

        assertOneOpen(linkA)
        assertEquals("the open reads the ad ID again", reads + 1, adIdReads.get())
    }

    @Test
    fun consentOff_aResponseWithoutALinkReplacesTheKeptLink() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        requestDeepLink(linkA)
        remote.noLinkFound = true
        requestDeepLink(linkB)

        sendOpen()
        setLevel(Defines.BranchAttributionLevel.FULL)

        assertOneOpen(null)
    }

    @Test
    fun backgroundWithAnOpenKept_sendsIt() {
        setProcessState(Lifecycle.State.RESUMED)
        requestDeepLink(linkA)

        setProcessState(Lifecycle.State.CREATED)

        assertOneOpen(linkA)
    }

    @Test
    fun backgroundWhileDeepLinkWaits_sendsItsOpenWhenTheResponseArrives() {
        setProcessState(Lifecycle.State.RESUMED)
        val release = startHeldDeepLink(linkA)

        setProcessState(Lifecycle.State.CREATED)
        release.countDown()

        assertOneOpen(linkA)
    }

    @Test
    fun backgroundWithNothingWaiting_sendsNothing() {
        setProcessState(Lifecycle.State.RESUMED)

        setProcessState(Lifecycle.State.CREATED)

        assertEquals(0, settledOpens(count = 0).size)
    }

    @Test
    fun sendOpenBeforeRequestDeepLinkData_thenBackground_sendsAnOpenWithoutLinkData() {
        setProcessState(Lifecycle.State.RESUMED)
        val called = sendOpen()
        assertEquals("nothing is sent before the background", 0, settledOpens(count = 0).size)

        setProcessState(Lifecycle.State.CREATED)

        assertTrue("the sendOpen callback never fired", called.await(10, TimeUnit.SECONDS))
        assertOneOpen(null)
    }

    @Test
    fun consentOff_backgroundWithAnOpenKept_sendsNothing() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        setProcessState(Lifecycle.State.RESUMED)
        requestDeepLink(linkA)

        setProcessState(Lifecycle.State.CREATED)

        assertEquals(0, settledOpens(count = 0).size)
    }

    @Test
    fun setAutomaticOpenEventsTrue_backgroundAfterTheOpen_sendsNothingExtra() {
        initialize(automaticOpenEvents = true)
        setProcessState(Lifecycle.State.RESUMED)
        requestDeepLink(linkA)
        settledOpens()

        setProcessState(Lifecycle.State.CREATED)

        assertEquals("only the open of the requestDeepLinkData call", 1, settledOpens().size)
    }

    @Test
    fun firstInstall_eventBeforeSendOpen_failsWithNoSession_andWarnsOnce() {
        initialize(automaticOpenEvents = false, returningUser = false, captureLogs = true)
        requestDeepLink(linkA)
        val failed = CountDownLatch(1)
        val code = AtomicInteger()

        BranchEvent("first_event").logEvent(context, object : BranchEvent.BranchLogEventCallback {
            override fun onSuccess(responseCode: Int) {}
            override fun onFailure(e: Exception) {
                code.set((e as BranchException).branchError.errorCode)
                failed.countDown()
            }
        })
        BranchEvent("second_event").logEvent(context)

        assertTrue("the event must fail before the open", failed.await(10, TimeUnit.SECONDS))
        assertEquals(BranchError.ERR_NO_SESSION, code.get())
        assertEquals("the warning is logged once", 1, warnings())
    }

    @Test
    fun eventWhileTheDeepLinkWaits_warnsOnce() {
        initialize(automaticOpenEvents = false, captureLogs = true)
        val release = startHeldDeepLink(linkA)

        BranchEvent("first_event").logEvent(context)
        BranchEvent("second_event").logEvent(context)
        release.countDown()
        settledOpens(count = 0)

        assertEquals("the warning is logged once", 1, warnings())
    }

    @Test
    fun eventAfterSendOpenWhileTheDeepLinkWaits_logsNoWarning() {
        initialize(automaticOpenEvents = false, captureLogs = true)
        val release = startHeldDeepLink(linkA)
        sendOpen()

        BranchEvent("an_event").logEvent(context)
        release.countDown()
        settledOpens()

        assertEquals(0, warnings())
    }

    @Test
    fun setAutomaticOpenEventsTrue_eventBeforeTheOpen_logsNoWarning() {
        initialize(automaticOpenEvents = true, captureLogs = true)
        requestDeepLink(linkA)

        BranchEvent("an_event").logEvent(context)
        settledOpens()

        assertEquals(0, warnings())
    }

    @Test
    fun consentOffDropsTheDeepLinkASendOpenWaitsFor_theNextResponseSendsTheOpen() {
        holdNextAdIdRead.set(true)
        Branch.getInstance().requestDeepLinkData(linkA) { _, _ -> }
        val app = sendOpen()
        setLevel(Defines.BranchAttributionLevel.NONE)
        adIdRelease.countDown()
        setLevel(Defines.BranchAttributionLevel.FULL)
        assertEquals("no /v3/deeplink answered, so nothing is sent", 0, settledOpens(count = 0).size)

        requestDeepLink(linkB)

        assertTrue("the app's sendOpen callback never fired", app.await(10, TimeUnit.SECONDS))
        assertOneOpen(linkB)
    }

    @Test
    fun consentOffDropsAQueuedOpen_answersItsCallbackWithTrackingDisabled() {
        setLevel(Defines.BranchAttributionLevel.NONE)
        requestDeepLink(linkA)
        setLevel(Defines.BranchAttributionLevel.FULL)
        holdNextAdIdRead.set(true)
        val error = AtomicReference<BranchError?>()
        val called = CountDownLatch(1)
        Branch.getInstance().sendOpen { e -> error.set(e); called.countDown() }

        setLevel(Defines.BranchAttributionLevel.NONE)
        adIdRelease.countDown()

        assertTrue("the sendOpen callback never fired", called.await(10, TimeUnit.SECONDS))
        assertEquals(BranchError.ERR_BRANCH_TRACKING_DISABLED, error.get()?.errorCode)
        assertEquals(0, settledOpens(count = 0).size)
    }

    @Test
    fun sendOpenAfterConsentOffWhileTheDeepLinkIsSending_sendsOneOpenWithTheLink() {
        setProcessState(Lifecycle.State.RESUMED)
        val release = startHeldDeepLink(linkA)
        setLevel(Defines.BranchAttributionLevel.NONE)
        val app = sendOpen()
        setLevel(Defines.BranchAttributionLevel.FULL)

        release.countDown()

        assertTrue("the app's sendOpen callback never fired", app.await(10, TimeUnit.SECONDS))
        setProcessState(Lifecycle.State.CREATED)
        assertOneOpen(linkA)
    }

    private fun initialize(automaticOpenEvents: Boolean, returningUser: Boolean = true, captureLogs: Boolean = false) {
        Branch.shutDown()
        Branch._userAgentString = "test-agent"
        val builder = BranchConfiguration.Builder("key_live_test123")
            .setAutomaticOpenEvents(automaticOpenEvents)
            .setRemoteInterface(remote)
        if (captureLogs) {
            builder.setLoggingCallback { message, _ -> logs.add(message) }
        }
        Branch.initialize(context, builder.build())
        useFakeReaders()
        if (returningUser) {
            PrefHelper.getInstance(context).setRandomizedBundleToken("rbt")
            PrefHelper.getInstance(context).setRandomizedDeviceToken("rdt")
            PrefHelper.getInstance(context).setSessionID("sid")
        } else {
            // Undoes setUpBase's own returningUser default, so isInstallLaunch() sees a first install.
            PrefHelper.getInstance(context).setRandomizedBundleToken(PrefHelper.NO_STRING_VALUE)
            PrefHelper.getInstance(context).setRandomizedDeviceToken(PrefHelper.NO_STRING_VALUE)
            PrefHelper.getInstance(context).setSessionID(PrefHelper.NO_STRING_VALUE)
        }
    }

    /**
     * The ad ID and install referrer reads finish at once, and count how often they ran. With
     * holdNextAdIdRead set, the next ad ID read waits for adIdRelease.
     */
    private fun useFakeReaders() {
        val observer = object : SystemObserver() {
            override fun fetchAdId(context: Context, callback: AdsParamsFetchEvents?) {
                adIdReads.incrementAndGet()
                val hold = holdNextAdIdRead.getAndSet(false)
                thread {
                    if (hold) adIdRelease.await(10, TimeUnit.SECONDS)
                    setGAID("38400000-8cf0-11bd-b23e-10b96e40000d")
                    callback?.onAdsParamsFetchFinished()
                }
            }

            override fun fetchInstallReferrer(context: Context, callback: InstallReferrerFetchEvents?) {
                thread { callback?.onInstallReferrersFinished() }
            }
        }
        DeviceInfo::class.java.getDeclaredField("systemObserver_").apply { isAccessible = true }
            .set(DeviceInfo.getInstance(), observer)
    }

    /** Calls requestDeepLinkData and waits until its response was handed over. */
    private fun requestDeepLink(link: Uri?) = requestDeepLinkWith { Branch.getInstance().requestDeepLinkData(link, it) }

    private fun requestDeepLink(activity: Activity) = requestDeepLinkWith { Branch.getInstance().requestDeepLinkData(activity, it) }

    private fun requestDeepLinkWith(start: (Branch.BranchReferralInitListener) -> Unit) {
        val done = CountDownLatch(1)
        start(Branch.BranchReferralInitListener { _, _ -> done.countDown() })
        assertTrue("requestDeepLinkData never called back", done.await(10, TimeUnit.SECONDS))
        awaitNoDeepLink()
    }

    /** Waits until no RequestDeepLink still waits for its response; fails after 10 seconds. */
    private fun awaitNoDeepLink() {
        val deadline = System.currentTimeMillis() + 10_000
        while (Branch.getInstance().requestQueue_.containsDeepLink()) {
            assertTrue("a RequestDeepLink still waits for its response after 10 seconds", System.currentTimeMillis() < deadline)
            idle()
        }
    }

    /** Starts requestDeepLinkData(uri) with `/v3/deeplink` held; the returned latch releases it. */
    private fun startHeldDeepLink(link: Uri?): CountDownLatch =
        holdDeepLinkWhile { Branch.getInstance().requestDeepLinkData(link) { _, _ -> } }

    /** Starts requestDeepLinkData(activity) with `/v3/deeplink` held; the returned latch releases it. */
    private fun startHeldDeepLink(activity: Activity): CountDownLatch =
        holdDeepLinkWhile { Branch.getInstance().requestDeepLinkData(activity) { _, _ -> } }

    private fun holdDeepLinkWhile(start: () -> Unit): CountDownLatch {
        val release = CountDownLatch(1)
        remote.holdDeepLink = release
        remote.deepLinkEntered.drainPermits()
        start()
        assertTrue("/v3/deeplink never reached the network", remote.deepLinkEntered.tryAcquire(10, TimeUnit.SECONDS))
        return release
    }

    /** Calls sendOpen; the returned latch opens when its callback fires. */
    private fun sendOpen(): CountDownLatch {
        val called = CountDownLatch(1)
        Branch.getInstance().sendOpen { called.countDown() }
        return called
    }

    /** Exactly one open, with link's link_data, or none when link is null. */
    private fun assertOneOpen(link: Uri?, opens: List<JSONObject> = settledOpens()) {
        assertEquals("one open: ${opens.map { linkOf(it) }}", 1, opens.size)
        assertEquals(link?.toString(), linkOf(opens[0]))
    }

    private fun warnings() = logs.count { it.startsWith("Warning, Branch logEvent, LATD or QR code called before sendOpen") }

    /** Waits for `count` opens, then long enough that one more would have been sent too. */
    private fun settledOpens(count: Int = 1): List<JSONObject> {
        val deadline = System.currentTimeMillis() + 10_000
        while (remote.opens().size < count && System.currentTimeMillis() < deadline) idle()
        val settle = System.currentTimeMillis() + 1_000
        while (System.currentTimeMillis() < settle) idle()
        return remote.opens()
    }

    private fun idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(10)
    }

    private fun setProcessState(state: Lifecycle.State) {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState = state
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun setLevel(level: Defines.BranchAttributionLevel) =
        Branch.getInstance().setConsumerProtectionAttributionLevel(level)

    private fun linkOf(open: JSONObject): String? = open.optJSONObject("link_data")?.optString("~referring_link")

    private companion object {
        const val ORGANIC_BODY =
            """{"data":"{\"+clicked_branch_link\":false,\"+is_first_session\":false}","randomized_device_token":"rdt"}"""
        const val FAILED_BODY = """{"error":"boom"}"""
        const val OPEN_RESPONSE =
            """{"randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""

        fun linkBody(link: String): String = JSONObject()
            .put("data", JSONObject().put("+clicked_branch_link", true).put("~referring_link", link).toString())
            .put("randomized_device_token", "rdt")
            .toString()
    }
}
