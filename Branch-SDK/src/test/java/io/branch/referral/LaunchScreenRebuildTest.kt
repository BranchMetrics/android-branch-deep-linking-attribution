package io.branch.referral

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import org.robolectric.android.controller.ActivityController
import java.util.concurrent.CopyOnWriteArrayList

/**
 * EMT-4087: one tap on a link is credited once. When Android rebuilds the screen a link opened, after a
 * configuration change or after it ended the app's process, the launch is sent without the link.
 */
class LaunchScreenRebuildTest : BranchTestBase() {

    /** Records every `/v3/deeplink` and `/v3/events/open`, and matches a deeplink that sends a link. */
    private class RecordingRemote : BranchRemoteInterface() {
        val deepLinks = CopyOnWriteArrayList<JSONObject>()
        val opens = CopyOnWriteArrayList<JSONObject>()

        override fun doRestfulGet(url: String?): BranchResponse = BranchResponse("{}", 200)

        override fun doRestfulPost(url: String?, payload: JSONObject?): BranchResponse = when {
            url.orEmpty().endsWith("v3/deeplink") -> {
                val body = JSONObject(payload.toString())
                deepLinks.add(body)
                BranchResponse(if (LINK_FIELDS.any { body.has(it) }) CLICKED_BODY else ORGANIC_BODY, 200)
            }
            url.orEmpty().endsWith("v3/events/open") -> {
                opens.add(JSONObject(payload.toString()))
                BranchResponse(OPEN_RESPONSE, 200)
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
    }

    @After
    override fun tearDownBase() {
        setProcessState(Lifecycle.State.CREATED)
        super.tearDownBase()
        Branch.shutDown()
        Branch._userAgentString = ""
    }

    @Test
    fun activityCall_afterAndroidEndedTheApp_sendsNoLink() {
        val first = screen(linkIntent(LINK_A)).create()
        Branch.getInstance().requestDeepLinkData(first.get()) { _, _ -> }
        awaitOpens(1)
        val saved = backgroundAndSave(first)

        // Android rebuilds the screen with its own copy of the intent that opened it.
        val rebuilt = screen(linkIntent(LINK_A)).create(saved)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        awaitOpens(2)

        assertCredited(0, Defines.Jsonkey.AndroidAppLinkURL.key)
        assertNotCredited(1)
    }

    @Test
    fun activityCall_afterAndroidEndedTheApp_doesNotSendTheClickIdAgain() {
        val first = screen(linkIntent(SCHEME_LINK)).create()
        Branch.getInstance().requestDeepLinkData(first.get()) { _, _ -> }
        awaitOpens(1)
        val saved = backgroundAndSave(first)

        // Android's copy still has the click id the first read took out of the screen's intent.
        val rebuilt = screen(linkIntent(SCHEME_LINK)).create(saved)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        awaitOpens(2)

        assertCredited(0, Defines.Jsonkey.LinkIdentifier.key)
        assertNotCredited(1)
    }

    @Test
    fun uriCall_afterAConfigurationChange_sendsNoLink() {
        val intent = linkIntent(LINK_A)
        val first = screen(intent).create()
        Branch.getInstance().requestDeepLinkData(intent.data) { _, _ -> }
        awaitOpens(1)
        val saved = Bundle().also { first.saveInstanceState(it) }

        // A configuration change rebuilds the screen with the same intent.
        val rotated = screen(intent).create(saved)
        Branch.getInstance().requestDeepLinkData(rotated.get().intent.data) { _, _ -> }
        awaitOpens(2)

        assertCredited(0, Defines.Jsonkey.AndroidAppLinkURL.key)
        assertNotCredited(1)
    }

    @Test
    fun uriCall_afterTheRebuiltScreenResumed_isANewTap() {
        val intent = linkIntent(LINK_A)
        val first = screen(intent).create()
        Branch.getInstance().requestDeepLinkData(intent.data) { _, _ -> }
        awaitOpens(1)
        val saved = Bundle().also { first.saveInstanceState(it) }
        val rotated = screen(intent).create(saved)
        Branch.getInstance().requestDeepLinkData(intent.data) { _, _ -> }
        awaitOpens(2)
        rotated.start().resume()

        // The user taps the link again; onNewIntent calls with it without setIntent.
        Branch.getInstance().requestDeepLinkData(Uri.parse(LINK_A)) { _, _ -> }
        awaitOpens(3)

        assertNotCredited(1)
        assertCredited(2, Defines.Jsonkey.AndroidAppLinkURL.key)
    }

    @Test
    fun activityCall_rebuiltScreenThenANewLinkThroughOnNewIntent_creditsTheNewLink() {
        val first = screen(linkIntent(LINK_A)).create()
        Branch.getInstance().requestDeepLinkData(first.get()) { _, _ -> }
        awaitOpens(1)
        val saved = backgroundAndSave(first)
        val rebuilt = screen(linkIntent(LINK_A)).create(saved)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        awaitOpens(2)

        rebuilt.get().intent = linkIntent(LINK_C)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        awaitOpens(3)

        assertNotCredited(1)
        assertCredited(2, Defines.Jsonkey.AndroidAppLinkURL.key)
        assertEquals(LINK_C, remote.deepLinks[2].optString(Defines.Jsonkey.AndroidAppLinkURL.key))
    }

    @Test
    fun rebuiltScreenAndANewLinkCalledBackToBack_eachSendsItsOpen_andTheNewLinkIsCredited() {
        val first = screen(linkIntent(LINK_A)).create()
        Branch.getInstance().requestDeepLinkData(first.get()) { _, _ -> }
        awaitOpens(1)
        val saved = backgroundAndSave(first)

        // Android rebuilds the screen, whose onCreate calls, then hands it link C through onNewIntent.
        val rebuilt = screen(linkIntent(LINK_A)).create(saved)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        rebuilt.get().intent = linkIntent(LINK_C)
        Branch.getInstance().requestDeepLinkData(rebuilt.get()) { _, _ -> }
        awaitOpens(3)

        assertNotCredited(1)
        assertEquals(LINK_C, remote.deepLinks[2].optString(Defines.Jsonkey.AndroidAppLinkURL.key))
        val credited = remote.opens.drop(1).filter { it.has(LINK_DATA) }
        assertEquals("one of the two opens credits a link: ${remote.opens}", 1, credited.size)
        assertEquals(LINK_C, credited[0].optString(Defines.Jsonkey.AndroidAppLinkURL.key))
    }

    @Test
    fun screenWithAnUnreadLink_savesNoMark() {
        val first = screen(linkIntent(LINK_A)).create()
        val saved = Bundle().also { first.saveInstanceState(it) }

        assertFalse(saved.getBoolean(BranchLaunchScreenObserver.KEY_LINK_USED, false))
    }

    private fun screen(intent: Intent): ActivityController<Activity> = Robolectric.buildActivity(Activity::class.java, intent)

    private fun linkIntent(link: String) = Intent(Intent.ACTION_VIEW, Uri.parse(link))

    /** Home, then Android saves the screen, as before it ends the app's process. */
    private fun backgroundAndSave(screen: ActivityController<Activity>): Bundle {
        setProcessState(Lifecycle.State.RESUMED)
        setProcessState(Lifecycle.State.CREATED)
        settle(300)
        return Bundle().also { screen.saveInstanceState(it) }
    }

    private fun assertCredited(index: Int, field: String) {
        assertTrue("deeplink $index sends its link: ${remote.deepLinks[index]}", remote.deepLinks[index].has(field))
        assertTrue("open $index credits the link: ${remote.opens[index]}", remote.opens[index].has(LINK_DATA))
    }

    private fun assertNotCredited(index: Int) {
        for (field in LINK_FIELDS) {
            assertFalse("deeplink $index sends $field: ${remote.deepLinks[index]}", remote.deepLinks[index].has(field))
        }
        assertFalse("open $index credits a link: ${remote.opens[index]}", remote.opens[index].has(LINK_DATA))
    }

    private fun setProcessState(state: Lifecycle.State) {
        (ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry).currentState = state
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun awaitOpens(count: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (remote.opens.size < count && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertEquals(count, remote.opens.size)
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
        const val LINK_A = "https://example.app.link/linkA"
        const val LINK_C = "https://example.app.link/linkC"
        const val SCHEME_LINK = "exampleapp://open?link_click_id=123456"
        const val LINK_DATA = "link_data"
        val LINK_FIELDS = listOf(
            Defines.Jsonkey.AndroidAppLinkURL.key,
            Defines.Jsonkey.External_Intent_URI.key,
            Defines.Jsonkey.LinkIdentifier.key,
        )
        const val ORGANIC_BODY =
            """{"data":"{\"+clicked_branch_link\":false,\"+is_first_session\":false}","randomized_device_token":"rdt"}"""
        const val CLICKED_BODY =
            """{"data":"{\"+clicked_branch_link\":true,\"~campaign\":\"summer\"}","randomized_device_token":"rdt"}"""
        const val OPEN_RESPONSE =
            """{"randomized_bundle_token":"rbt","randomized_device_token":"rdt","session_id":"sid"}"""
    }
}
