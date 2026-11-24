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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * A click-through install whose first launch has attribution off, from that launch until the user
 * consents. A process that ends is a new Branch instance over the same prefs. A process that goes to
 * the background first runs the SDK's background clear; one that dies in the foreground does not.
 */
class ConsentOffInstallCreditTest : BranchTestBase() {

    /**
     * Records every POST. `/v3/deeplink` finds a link for the install's click id, once, as the server's
     * matching uses a click up when it matches it, and for OTHER_LINK.
     */
    private class RecordingRemote : BranchRemoteInterface() {
        val posts = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        private val usedClickIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        @Volatile var failDeepLink = false
        @Volatile var failOpen = false

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse {
            val path = url.orEmpty().substringAfter("branch.io/")
            val body = JSONObject(payload.toString())
            posts.add(path to body)
            return when (path) {
                "v3/deeplink" -> when {
                    failDeepLink -> BranchResponse(FAILED_BODY, 500)
                    body.optString("link_identifier") == CLICK_ID && usedClickIds.add(CLICK_ID) ->
                        BranchResponse(linkBody(REFERRING_LINK), 200)
                    body.optString("android_app_link_url") == OTHER_LINK -> BranchResponse(linkBody(OTHER_LINK), 200)
                    else -> BranchResponse(ORGANIC_BODY, 200)
                }
                "v3/events/open" ->
                    if (failOpen) BranchResponse(FAILED_BODY, 500) else BranchResponse(OPEN_RESPONSE, 200)
                else -> BranchResponse("{}", 200)
            }
        }

        fun deepLinks() = posts.filter { it.first == "v3/deeplink" }.map { it.second }
        fun opens() = posts.filter { it.first == "v3/events/open" }.map { it.second }
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var remote: RecordingRemote
    private val referrerReads = AtomicInteger()
    @Volatile private var playReferrer: String? = REFERRER_WITH_CLICK_ID

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.INTERNET)
        remote = RecordingRemote()
        newProcess()
        val prefs = PrefHelper.getInstance(context)
        prefs.setRandomizedBundleToken(PrefHelper.NO_STRING_VALUE)
        prefs.setRandomizedDeviceToken(PrefHelper.NO_STRING_VALUE)
        prefs.setSessionID(PrefHelper.NO_STRING_VALUE)
        prefs.installParams = PrefHelper.NO_STRING_VALUE
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
    fun homeThenBack_noNewCall_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        toForeground()

        assertCreditsTheInstall(consent())
    }

    @Test
    fun homeThenBack_appCallsAgainOnReturn_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        toForeground()
        request(launcher())

        assertCreditsTheInstall(consent())
    }

    @Test
    fun backgroundThenProcessEnds_relaunch_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        newProcess()
        request(launcher())

        assertCreditsTheInstall(consent())
    }

    @Test
    fun backgroundThenProcessEnds_restoredScreenMakesNoCall_consentSendsNoOpen_nextLaunchCreditsTheInstall() {
        firstLaunch()
        toBackground()
        newProcess()

        assertEquals("no requestDeepLinkData call, so no open is held", 0, consent(expectOpen = false).size)

        toBackground()
        newProcess(level = null)
        val (params, _) = request(launcher())
        assertFalse("the used click id routed again: $params", params?.optBoolean("+clicked_branch_link") == true)
        assertCreditsTheInstall(settledOpens())
    }

    @Test
    fun processDiesInTheForeground_relaunchWithTheActivity_consent_creditsTheInstall() {
        firstLaunch()
        newProcess()
        request(launcher())

        assertCreditsTheInstall(consent())
    }

    @Test
    fun processDiesInTheForeground_relaunchWithANullUri_doesNotRouteTheLinkAgain() {
        val (first, _) = request(null as Uri?)
        assertEquals(REFERRING_LINK, first?.optString("~referring_link"))
        newProcess()

        val (params, _) = request(null as Uri?)

        val again = remote.deepLinks().drop(1).filter { it.has("link_identifier") }
        assertTrue("the install's click id was sent again: $again", again.isEmpty())
        assertFalse("the link routed again: $params", params?.optBoolean("+clicked_branch_link") == true)
    }

    @Test
    fun processDiesInTheForeground_relaunchWithANullUri_consent_creditsTheInstall() {
        request(null as Uri?)
        newProcess()
        request(null as Uri?)

        assertCreditsTheInstall(consent())
    }

    @Test
    fun browserLoginRedirectsIntoTheApp_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        toForeground()
        request(view("branchcase://oauth?code=abc123"))

        assertCreditsTheInstall(consent())
    }

    @Test
    fun browserLoginRedirectsToAnHttpsLink_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        toForeground()
        request(view("https://login.example.com/callback?code=abc123"))

        assertCreditsTheInstall(consent())
    }

    @Test
    fun browserLoginReturnsThroughARedirectScreen_newIntentWithoutData_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        toForeground()
        request(Robolectric.buildActivity(Activity::class.java, Intent()).get())

        assertCreditsTheInstall(consent())
    }

    @Test
    fun appKilledInTheBrowser_redirectRelaunchesIt_consent_creditsTheInstall() {
        firstLaunch()
        toBackground()
        newProcess()
        request(view("branchcase://oauth?code=abc123"))

        assertCreditsTheInstall(consent())
    }

    @Test
    fun inAppScreenThenBack_appCallsAgainOnReturn_consent_creditsTheInstall() {
        firstLaunch()
        request(launcher())

        assertCreditsTheInstall(consent())
    }

    @Test
    fun afterABrowserTrip_latestParamsAreEmpty_firstParamsKeepTheLink() {
        firstLaunch()
        toBackground()
        toForeground()

        val latest = Branch.getInstance().latestReferringParams
        val first = Branch.getInstance().firstReferringParams
        assertFalse("latest params after the background: $latest", latest.optBoolean("+clicked_branch_link"))
        assertEquals(REFERRING_LINK, first.optString("~referring_link"))
    }

    @Test
    fun laterLaunchFromAnotherBranchLink_consent_creditsThatLink() {
        firstLaunch()
        toBackground()
        toForeground()
        request(view(OTHER_LINK))

        val opens = consent()
        assertEquals("one open: $opens", 1, opens.size)
        assertEquals(OTHER_LINK, linkOf(opens[0]))
    }

    @Test
    fun organicInstall_consent_sendsAnOpenWithoutALink() {
        playReferrer = ORGANIC_REFERRER
        firstLaunch()

        val opens = consent()
        assertEquals("one open: $opens", 1, opens.size)
        assertFalse("an organic install has no link: ${opens[0]}", opens[0].has("link_data"))
    }

    @Test
    fun firstDeepLinkFails_nextLaunchGetsTheLink_consent_creditsTheInstall() {
        remote.failDeepLink = true
        firstLaunch()
        remote.failDeepLink = false
        toBackground()
        newProcess()
        val (params, _) = request(launcher())

        assertEquals(REFERRING_LINK, params?.optString("~referring_link"))
        assertCreditsTheInstall(consent())
    }

    @Test
    fun afterConsent_laterOpensCarryNoLink_andTheReferrerIsNotReadAgain() {
        firstLaunch()
        assertCreditsTheInstall(consent())
        toBackground()
        newProcess(level = null)
        val readsBefore = referrerReads.get()

        request(launcher())

        val opens = settledOpens(count = 2)
        assertEquals("two opens: $opens", 2, opens.size)
        assertFalse("a later open carried a link: ${opens[1]}", opens[1].has("link_data"))
        assertEquals("the referrer was read again", readsBefore, referrerReads.get())
    }

    @Test
    fun openAtConsentFails_nextLaunchAtFull_sendsTheClickIdAgain_andCreditsTheInstall() {
        firstLaunch()
        remote.failOpen = true
        consent()
        remote.failOpen = false
        toBackground()
        newProcess(level = null)

        request(launcher())

        assertEquals(CLICK_ID, remote.deepLinks().last().optString("link_identifier"))
        assertEquals(REFERRING_LINK, linkOf(remote.opens().last()))
    }

    @Test
    fun userNeverConsents_noOpenIsSent() {
        firstLaunch()
        toBackground()
        toForeground()
        request(launcher())
        toBackground()
        newProcess()
        request(launcher())

        assertEquals(0, settledOpens(count = 0).size)
    }

    @Test
    fun returningUserAtNone_newCallWithoutALink_consent_sendsAnOpenWithoutALink() {
        val prefs = PrefHelper.getInstance(context)
        prefs.setRandomizedBundleToken("rbt")
        prefs.installParams = JSONObject().put("+clicked_branch_link", true).put("~referring_link", REFERRING_LINK).toString()

        request(launcher())

        val opens = consent()
        assertEquals("one open: $opens", 1, opens.size)
        assertFalse("a returning user's open carried the install's link: ${opens[0]}", opens[0].has("link_data"))
    }

    @Test
    fun automaticOpensOff_newCallBeforeConsent_sendOpen_consent_creditsTheInstall() {
        newProcess(automaticOpenEvents = false)
        firstLaunch()
        toBackground()
        newProcess(automaticOpenEvents = false)
        request(launcher())
        Branch.getInstance().sendOpen(null)

        assertCreditsTheInstall(consent())
    }

    /** A cold start: a new Branch instance over the prefs earlier launches saved. A null level keeps the saved one. */
    private fun newProcess(
        level: Defines.BranchAttributionLevel? = Defines.BranchAttributionLevel.NONE,
        automaticOpenEvents: Boolean = true
    ) {
        Branch.shutDown()
        resetReferrerClickId()
        Branch._userAgentString = "test-agent"
        val builder = BranchConfiguration.Builder("key_live_test123")
            .setRemoteInterface(remote)
            .setAutomaticOpenEvents(automaticOpenEvents)
        level?.let { builder.setAttributionLevel(it) }
        Branch.initialize(context, builder.build())
        useFakeReaders()
        setProcessState(Lifecycle.State.RESUMED)
    }

    private fun firstLaunch() = request(launcher())

    private fun launcher(): Activity = Robolectric.buildActivity(
        Activity::class.java, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    ).get()

    private fun view(link: String): Activity =
        Robolectric.buildActivity(Activity::class.java, Intent(Intent.ACTION_VIEW, Uri.parse(link))).get()

    private fun toBackground() {
        setProcessState(Lifecycle.State.CREATED)
        waitUntil("a request never left the queue") { !Branch.getInstance().requestQueue_.containsDeepLinkOrOpen() }
    }

    private fun toForeground() = setProcessState(Lifecycle.State.RESUMED)

    private fun consent(expectOpen: Boolean = true): List<JSONObject> {
        val before = remote.opens().size
        Branch.getInstance().setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.FULL)
        return settledOpens(count = if (expectOpen) before + 1 else before).drop(before)
    }

    private fun assertCreditsTheInstall(opens: List<JSONObject>) {
        assertEquals("one open: $opens", 1, opens.size)
        assertEquals("the open doesn't carry the install's link: ${opens[0]}", REFERRING_LINK, linkOf(opens[0]))
    }

    private fun linkOf(open: JSONObject): String? = open.optJSONObject("link_data")?.optString("~referring_link")

    /** The install referrer read parses playReferrer as Play returns it; the ad ID read finishes at once. */
    private fun useFakeReaders() {
        val observer = object : SystemObserver() {
            override fun fetchAdId(context: Context, callback: AdsParamsFetchEvents?) {
                thread {
                    setGAID("38400000-8cf0-11bd-b23e-10b96e40000d")
                    callback?.onAdsParamsFetchFinished()
                }
            }

            override fun fetchInstallReferrer(context: Context, callback: InstallReferrerFetchEvents?) {
                referrerReads.incrementAndGet()
                thread {
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

    private fun request(uri: Uri?) = requestWith { Branch.getInstance().requestDeepLinkData(uri, it) }

    private fun request(activity: Activity) = requestWith { Branch.getInstance().requestDeepLinkData(activity, it) }

    /** Calls requestDeepLinkData, waits for its callback and for its request to leave the queue. */
    private fun requestWith(start: (Branch.BranchReferralInitListener) -> Unit): Pair<JSONObject?, BranchError?> {
        val result = AtomicReference<Pair<JSONObject?, BranchError?>>()
        val done = CountDownLatch(1)
        start(Branch.BranchReferralInitListener { params, error -> result.set(params to error); done.countDown() })
        waitUntil("requestDeepLinkData never called back") { done.count == 0L }
        waitUntil("the request never left the queue") { !Branch.getInstance().requestQueue_.containsDeepLinkOrOpen() }
        return result.get()
    }

    /** Waits for `count` opens, then long enough that one more would have been sent too. */
    private fun settledOpens(count: Int = 1): List<JSONObject> {
        val deadline = System.currentTimeMillis() + 10_000
        while (remote.opens().size < count && System.currentTimeMillis() < deadline) idle()
        val settle = System.currentTimeMillis() + 1_500
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
        const val OTHER_LINK = "https://example.app.link/second-link"
        const val REFERRER_WITH_CLICK_ID = "link_click_id=$CLICK_ID&utm_source=google-play&utm_medium=cpc"
        const val ORGANIC_REFERRER = "utm_source=google-play&utm_medium=organic"
        const val CLICK_TS = 1_794_900_000L
        const val INSTALL_BEGIN_TS = 1_794_900_030L

        const val ORGANIC_BODY =
            """{"data":"{\"+clicked_branch_link\":false,\"+is_first_session\":false}","randomized_device_token":"rdt"}"""
        const val FAILED_BODY = """{"error":"boom"}"""
        const val OPEN_RESPONSE =
            """{"randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""

        fun linkBody(link: String): String = JSONObject()
            .put("data", JSONObject()
                .put("+clicked_branch_link", true)
                .put("~referring_link", link)
                .put("~campaign", "summer")
                .toString())
            .put("randomized_device_token", "rdt")
            .toString()
    }
}
