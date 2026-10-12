package io.branch.referral

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

/** EMT-4540: an open completing while a deep link request is queued must not clear the link that request has yet to send. */
class PostInitClearTest : BranchTestBase() {

    /** Holds the first `/v3/events/open` until released; records every `/v3/deeplink` and open body, and the link prefs when each `/v3/deeplink` arrives. */
    private class OpenHoldingRemote(private val prefs: () -> PrefHelper) : BranchRemoteInterface() {
        val openEntered = CountDownLatch(1)
        val releaseOpen = CountDownLatch(1)
        @Volatile var openReleasedInTime = false
        val deepLinks = CopyOnWriteArrayList<JSONObject>()
        val opens = CopyOnWriteArrayList<JSONObject>()
        val prefsAtDeepLink = CopyOnWriteArrayList<List<String>>()
        private val openCount = AtomicInteger()

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse = when {
            url.orEmpty().endsWith("v3/deeplink") -> {
                prefsAtDeepLink.add(prefs().let { listOf(it.linkClickIdentifier, it.pushIdentifier, it.appLink, it.externalIntentUri) })
                deepLinks.add(JSONObject(payload.toString()))
                BranchResponse(resolvedBody(if (deepLinks.size == 1) FIRST_CAMPAIGN else SECOND_CAMPAIGN), 200)
            }
            url.orEmpty().endsWith("v3/events/open") -> {
                opens.add(JSONObject(payload.toString()))
                if (openCount.incrementAndGet() == 1) {
                    openEntered.countDown()
                    openReleasedInTime = releaseOpen.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
                BranchResponse(OPEN_RESPONSE, 200)
            }
            else -> BranchResponse("{}", 200)
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()
    private val remote = OpenHoldingRemote { PrefHelper.getInstance(context) }

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.INTERNET)
    }

    @After
    override fun tearDownBase() {
        remote.releaseOpen.countDown()
        Branch.shutDown()
        super.tearDownBase()
        Branch._userAgentString = ""
    }

    @Test
    fun appLinkWithAClickId_queuedBehindAnInFlightOpen_keepsItsLink() {
        val (sent, open) = resolveQueuedBehindAnInFlightOpen(activity(Intent(Intent.ACTION_VIEW, Uri.parse(APP_LINK)).putExtra("branch_intent", true)))

        // A click id link never writes android_app_link_url: extractClickID returns before extractAppLink.
        assertEquals("sent: $sent", CLICK_ID, sent.optString(Defines.Jsonkey.LinkIdentifier.key))
        assertTrue("sent: $sent", sent.optString(Defines.Jsonkey.External_Intent_URI.key).startsWith(APP_URL))
        assertTrue("sent: $sent", sent.optString(Defines.Jsonkey.External_Intent_Extra.key).contains("branch_intent"))
        assertEquals("open: $open", CLICK_ID, open.optString(Defines.Jsonkey.LinkIdentifier.key))
    }

    @Test
    fun schemeLinkWithAClickId_queuedBehindAnInFlightOpen_keepsItsLink() {
        val (sent, open) = resolveQueuedBehindAnInFlightOpen(activity(Intent(Intent.ACTION_VIEW, Uri.parse(SCHEME_LINK))))

        assertEquals("sent: $sent", CLICK_ID, sent.optString(Defines.Jsonkey.LinkIdentifier.key))
        assertTrue("sent: $sent", sent.optString(Defines.Jsonkey.External_Intent_URI.key).startsWith(SCHEME_URL))
        assertEquals("open: $open", CLICK_ID, open.optString(Defines.Jsonkey.LinkIdentifier.key))
    }

    @Test
    fun pushLink_queuedBehindAnInFlightOpen_keepsItsLink() {
        val (sent, open) = resolveQueuedBehindAnInFlightOpen(activity(Intent(Intent.ACTION_MAIN).putExtra(Defines.IntentKeys.BranchURI.key, PUSH_LINK)))

        assertEquals("sent: $sent", PUSH_LINK, sent.optString(Defines.Jsonkey.AndroidPushIdentifier.key))
        assertEquals("open: $open", PUSH_LINK, open.optString(Defines.Jsonkey.AndroidPushIdentifier.key))
    }

    /** Needs the manual open API of #1436 (setAutomaticOpenEvents, sendOpen), so it is not applicable on 6.0.0-beta.2. */
    @Test
    fun appSentOpenInFlight_newerLinkQueued_keepsItsLink() {
        val (sent, open) = resolveQueuedBehindAnInFlightOpen(
            activity(Intent(Intent.ACTION_VIEW, Uri.parse(APP_LINK)).putExtra("branch_intent", true)),
            automaticOpenEvents = false
        )

        assertEquals("sent: $sent", CLICK_ID, sent.optString(Defines.Jsonkey.LinkIdentifier.key))
        assertTrue("sent: $sent", sent.optString(Defines.Jsonkey.External_Intent_URI.key).startsWith(APP_URL))
        assertEquals("open: $open", CLICK_ID, open.optString(Defines.Jsonkey.LinkIdentifier.key))
    }

    /**
     * Resolves a first link and holds its open in flight, queues the second launch behind it, releases the open
     * so its response runs the clear, and returns the second `/v3/deeplink` body and the open chained to it.
     */
    private fun resolveQueuedBehindAnInFlightOpen(second: Activity, automaticOpenEvents: Boolean = true): Pair<JSONObject, JSONObject> {
        initialize(automaticOpenEvents)
        Branch.getInstance().requestDeepLinkData(activity(Intent(Intent.ACTION_MAIN))) { _, _ -> }
        if (!automaticOpenEvents) {
            // The open is the app's: nothing is sent until sendOpen.
            awaitTrue("the first /v3/deeplink never resolved") { remote.deepLinks.size >= 1 }
            Branch.getInstance().sendOpen { }
        }
        assertTrue("the first open never reached the network", remote.openEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

        Branch.getInstance().requestDeepLinkData(second) { _, _ -> }
        awaitTrue("the second RequestDeepLink was never queued") { Branch.getInstance().requestQueue_.containsDeepLink() }
        settle(SETTLE_MS)
        assertEquals("the second resolve must wait for the open", 1, remote.deepLinks.size)

        remote.releaseOpen.countDown()
        awaitTrue("the second /v3/deeplink was never sent") { remote.deepLinks.size >= 2 }
        assertTrue("the open was released by the timeout, not by the test", remote.openReleasedInTime)
        // The clear ran before the second resolve was sent: its link came from the capture, not from the prefs.
        assertEquals("prefs at the second /v3/deeplink", List(4) { PrefHelper.NO_STRING_VALUE }, remote.prefsAtDeepLink[1])

        if (!automaticOpenEvents) Branch.getInstance().sendOpen { }
        awaitTrue("the open chained to the second link was never sent") { remote.opens.size >= 2 }
        val sent = remote.deepLinks[1]
        val open = remote.opens[1]
        assertEquals("open: $open", SECOND_CAMPAIGN, open.optJSONObject("link_data")?.optString("~campaign"))
        LINK_KEYS.forEach { assertEquals("$it on the open: $open", sent.opt(it)?.toString(), open.opt(it)?.toString()) }
        return sent to open
    }

    private fun initialize(automaticOpenEvents: Boolean) {
        Branch.shutDown()
        Branch._userAgentString = "test-agent"
        val config = BranchConfiguration.Builder("key_live_test123")
            .setAutomaticOpenEvents(automaticOpenEvents)
            .setRemoteInterface(remote)
            .build()
        Branch.initialize(context, config)
    }

    private fun activity(intent: Intent): Activity = Robolectric.buildActivity(Activity::class.java, intent).get()

    private fun settle(ms: Long) {
        val deadline = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private fun awaitTrue(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000
        while (!condition() && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(message, condition())
    }

    private companion object {
        const val APP_URL = "https://example.app.link/warm"
        const val CLICK_ID = "222222"
        const val APP_LINK = "$APP_URL?link_click_id=$CLICK_ID"
        const val SCHEME_URL = "myapp://open"
        const val SCHEME_LINK = "$SCHEME_URL?link_click_id=$CLICK_ID"
        const val PUSH_LINK = "https://example.app.link/push2"
        const val TIMEOUT_SECONDS = 10L
        const val SETTLE_MS = 300L
        const val FIRST_CAMPAIGN = "first"
        const val SECOND_CAMPAIGN = "second"
        val LINK_KEYS = listOf("link_identifier", "android_app_link_url", "push_identifier", "external_intent_uri", "external_intent_extra")

        fun resolvedBody(campaign: String) =
            """{"data":"{\"+clicked_branch_link\":true,\"~campaign\":\"$campaign\"}","randomized_device_token":"rdt"}"""

        const val OPEN_RESPONSE =
            """{"randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""
    }
}
