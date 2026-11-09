package io.branch.referral

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlin.concurrent.thread

/**
 * Which fields carry the launch URI on /v3/deeplink.
 *
 * An http or https link rides both android_app_link_url and external_intent_uri, a custom
 * scheme rides external_intent_uri alone, and a link_click_id query parameter becomes
 * link_identifier. LaunchDataTest covers the https case and the click id on an https link;
 * what it does not reach is http, a custom scheme, a click id on a scheme URI, and a launch
 * with no data. The L1 warm scenarios exercise one URI each, at the cost of a device run.
 *
 * Asserted against the literal wire names rather than the Defines constants, so renaming a
 * constant's value cannot move the wire silently.
 */
class RequestDeepLinkUriMappingTest : BranchTestBase() {

    private class RecordingRemote : BranchRemoteInterface() {
        val deepLinks = CopyOnWriteArrayList<JSONObject>()
        val opens = CopyOnWriteArrayList<JSONObject>()

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse = when {
            url.orEmpty().endsWith("v3/deeplink") -> {
                deepLinks.add(JSONObject(payload.toString()))
                BranchResponse("""{"data":"{}","randomized_device_token":"rdt"}""", 200)
            }
            url.orEmpty().endsWith("v3/events/open") -> {
                opens.add(JSONObject(payload.toString()))
                BranchResponse("""{"randomized_bundle_token":"rbt","randomized_device_token":"rdt"}""", 200)
            }
            else -> BranchResponse("{}", 200)
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var remote: RecordingRemote

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
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
    }

    private fun postFor(uri: String?): JSONObject {
        val intent = Intent(Intent.ACTION_VIEW).apply { uri?.let { data = Uri.parse(it) } }
        val activity: Activity = Robolectric.buildActivity(Activity::class.java, intent).get()
        Branch.getInstance().requestDeepLinkData(activity) { _, _ -> }
        awaitOpen()
        return remote.deepLinks[0]
    }

    @Test
    fun httpGoesToBothUriFields() {
        // LaunchDataTest pins https; the branch tests http separately, so http alone
        // regressing would be invisible.
        val post = postFor("http://bnctestbed.app.link/abc123")

        assertEquals("http://bnctestbed.app.link/abc123", post.optString("android_app_link_url"))
        assertEquals("http://bnctestbed.app.link/abc123", post.optString("external_intent_uri"))
    }

    @Test
    fun aCustomSchemeGoesToExternalIntentUriAlone() {
        val post = postFor("branchtest://open")

        assertEquals("branchtest://open", post.optString("external_intent_uri"))
        assertFalse("a scheme URI must not use the app-link field", post.has("android_app_link_url"))
    }

    @Test
    fun aSchemeUriCarriesItsLinkClickIdToo() {
        val post = postFor("branchtest://open?link_click_id=xyz789")

        assertEquals("xyz789", post.optString("link_identifier"))
        assertEquals("branchtest://open?link_click_id=xyz789", post.optString("external_intent_uri"))
    }

    @Test
    fun withoutAClickIdTheFieldIsAbsent() {
        val post = postFor("https://bnctestbed.app.link/abc123")

        assertFalse("an absent click id must leave the field off, not send it empty", post.has("link_identifier"))
    }

    @Test
    fun aLaunchWithNoDataCarriesNoUriField() {
        // The organic launch: no link opened the app. N1's capture is this case on the wire.
        val post = postFor(null)

        assertFalse(post.has("android_app_link_url"))
        assertFalse(post.has("external_intent_uri"))
        assertFalse(post.has("link_identifier"))
    }

    private fun awaitOpen() {
        val deadline = System.currentTimeMillis() + 10_000
        while (remote.opens.isEmpty() && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("no open reached the remote", remote.opens.isNotEmpty())
        // The body is recorded before the open's reply handling has finished, and tearDown
        // shuts the Branch instance down, so wait for the queue to drain (as ConsentLevelOpenTest
        // does) instead of a fixed delay.
        val drainDeadline = System.currentTimeMillis() + 10_000
        while (Branch.getInstance().requestQueue_.containsDeepLinkOrOpen()) {
            assertTrue("the request queue never drained", System.currentTimeMillis() < drainDeadline)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    /** Answers the ad ID and install referrer reads at once, so no launch waits on a lock. */
    private fun useFakeReaders() {
        val observer = object : SystemObserver() {
            override fun fetchAdId(context: Context, callback: AdsParamsFetchEvents?) {
                thread { callback?.onAdsParamsFetchFinished() }
            }

            override fun fetchInstallReferrer(context: Context, callback: InstallReferrerFetchEvents?) {
                thread { callback?.onInstallReferrersFinished() }
            }
        }
        DeviceInfo::class.java.getDeclaredField("systemObserver_").apply { isAccessible = true }.set(DeviceInfo.getInstance(), observer)
    }
}
