package io.branch.referral

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.net.HttpURLConnection

/**
 * Pins what the open does to the persisted deep link payload.
 *
 * The v3/events/open response carries no "data" key. Writing sessionParams from it
 * unconditionally cleared whatever a preceding deep link resolution had persisted, so
 * getLatestReferringParams returned empty on every link-driven launch. No existing test asserts
 * on sessionParams after an open, which is why that survived.
 */
class RequestOpenSessionParamsTest : BranchTestBase() {

    private val resolvedPayload = """{"~channel":"Distribution Channel","+clicked_branch_link":true}"""

    private lateinit var branch: Branch
    private lateinit var prefHelper: PrefHelper

    @Before
    fun setUpBranch() {
        // With Config.NONE the manifest is absent; getAutoInstance handles that and gives the
        // request a real DeviceInfo, which its constructor dereferences.
        branch = Branch.getAutoInstance(RuntimeEnvironment.getApplication())
        prefHelper = PrefHelper.getInstance(RuntimeEnvironment.getApplication())
        prefHelper.sessionParams = resolvedPayload
    }

    @Test
    fun openWithoutSessionData_leavesTheResolvedPayloadIntact() {
        processOpenResponse(JSONObject().put("invoke_register_app", true))

        assertEquals(resolvedPayload, prefHelper.sessionParams)
    }

    @Test
    fun openWithSessionData_stillWritesIt() {
        val openPayload = """{"~channel":"Organic"}"""

        processOpenResponse(JSONObject().put("data", openPayload))

        assertEquals(openPayload, prefHelper.sessionParams)
    }

    private fun processOpenResponse(body: JSONObject) {
        val request = RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
        val response = ServerResponse("open", HttpURLConnection.HTTP_OK, "req-1", "OK")
        response.setPost(body)

        // onInitSessionCompleted runs after the write and reaches collaborators this test does not
        // stand up. The write is what is under test, and it has already happened by then.
        runCatching { request.onRequestSucceeded(response, branch) }
    }
}
