package io.branch.referral

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * EMT-3731: standard and custom events moved onto v3. The move was required to be
 * path-only — the body shape had to stay exactly as it was on v2.
 *
 * BRANCH_API_VERSION selects body shape, not URL version, so path and shape have to be
 * pinned separately. v3/events/open shares the new path prefix but must keep the flat
 * v1/open body; only standard/custom nest under user_data.
 */
class EventRequestPathTest : BranchTestBase() {

    @Before
    fun setUp() {
        super.setUpBase()
        Branch.initialize(
            RuntimeEnvironment.getApplication(),
            BranchConfiguration.Builder("key_live_test123").build(),
        )
    }

    @After
    fun tearDown() {
        Branch.shutDown()
    }

    private fun buildEvent(path: Defines.RequestPath): ServerRequestLogEvent =
        ServerRequestLogEvent(
            RuntimeEnvironment.getApplication(),
            path,
            "my_custom_event",
            HashMap(),
            JSONObject(),
            JSONObject(),
            emptyList()
        )

    // --- Paths: pinned against literals, so a singular "v3/event/..." typo fails. -------------
    // BranchEventTest only compares the enum to itself, which such a typo would survive.

    @Test
    fun standardEventPostsToV3() {
        assertEquals("v3/events/standard", Defines.RequestPath.TrackStandardEvent.path)
    }

    @Test
    fun customEventPostsToV3() {
        assertEquals("v3/events/custom", Defines.RequestPath.TrackCustomEvent.path)
    }

    // --- Shape: the invariant the path move was not allowed to disturb. -----------------------

    @Test
    fun eventBodyNestsDeviceDataUnderUserData() {
        val post = buildEvent(Defines.RequestPath.TrackCustomEvent).post

        val userData = post.optJSONObject(Defines.Jsonkey.UserData.key)
        assertTrue("v3/events/* must nest device data under user_data", userData != null)
        for (field in listOf("brand", "model", "os")) {
            assertTrue("$field must live inside user_data", userData!!.has(field))
            assertFalse("$field must not also sit at the top level", post.has(field))
        }
    }

    @Test
    fun eventReportsV2BodyShape() {
        assertEquals(
            "Events keep the V2 (nested) shape on v3; flipping this un-nests the body",
            ServerRequest.BRANCH_API_VERSION.V2,
            buildEvent(Defines.RequestPath.TrackCustomEvent).branchRemoteAPIVersion
        )
    }

    @Test
    fun openBodyStaysFlatLikeV1Open() {
        val open = RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
        val post = open.post

        assertFalse(
            "v3/events/open must keep the flat v1/open body, not nest under user_data",
            post.has(Defines.Jsonkey.UserData.key)
        )
        for (field in listOf("brand", "model", "os")) {
            assertTrue("$field must sit at the top level for open", post.has(field))
        }
    }

    @Test
    fun openReportsV1BodyShape() {
        assertEquals(
            "v3/events/open must stay V1 so its body matches v1/open and v1/install",
            ServerRequest.BRANCH_API_VERSION.V1,
            RequestOpen(RuntimeEnvironment.getApplication(), null, false, null)
                .branchRemoteAPIVersion
        )
    }
}
