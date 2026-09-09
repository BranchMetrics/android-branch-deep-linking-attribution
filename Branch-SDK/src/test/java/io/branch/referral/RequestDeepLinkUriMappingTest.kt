package io.branch.referral

import android.net.Uri
import io.branch.coroutines.RequestDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * Which field carries the URI on /v3/deeplink, and when link_identifier appears.
 *
 * RequestDeepLink's init puts an http or https URI in android_app_link_url and everything
 * else in external_intent_uri, and lifts a link_click_id query parameter into
 * link_identifier. Nothing asserted that in either source set: a grep for
 * external_intent_uri or AndroidAppLinkURL over src/test and src/androidTest returned
 * nothing before this file.
 *
 * The L1 warm scenarios reach the same branch, but a capture can only ever exercise the one
 * URI it delivered, at the cost of a device run. The mapping is a pure function of the
 * scheme, so it belongs here, where every case is cheap. W1 and W2 keep the halves a unit
 * test cannot reach: that the manifest filter matches and that the OS delivers the intent.
 *
 * Asserted against the literal wire names rather than the Defines constants. Renaming a
 * constant's value changes the wire, and a test written against the constant would follow it
 * silently.
 */
class RequestDeepLinkUriMappingTest : BranchTestBase() {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    override fun setUpBase() {
        super.setUpBase()
        Branch.initialize(context, BranchConfiguration.Builder("key_live_test123").build())
    }

    private fun postFor(uri: String?) =
        RequestDeepLink(context, uri?.let(Uri::parse), null, false).post

    @Test
    fun httpsGoesToAppLinkUrl() {
        val post = postFor("https://bnctestbed.app.link/abc123")

        assertEquals("https://bnctestbed.app.link/abc123", post.optString("android_app_link_url"))
        assertFalse("https must not use the scheme field", post.has("external_intent_uri"))
    }

    @Test
    fun httpGoesToAppLinkUrlToo() {
        // The branch tests both, so both are pinned. http alone regressing would be invisible.
        val post = postFor("http://bnctestbed.app.link/abc123")

        assertEquals("http://bnctestbed.app.link/abc123", post.optString("android_app_link_url"))
        assertFalse(post.has("external_intent_uri"))
    }

    @Test
    fun aCustomSchemeGoesToExternalIntentUri() {
        val post = postFor("branchtest://open")

        assertEquals("branchtest://open", post.optString("external_intent_uri"))
        assertFalse("a scheme URI must not use the app-link field", post.has("android_app_link_url"))
    }

    @Test
    fun aLinkClickIdBecomesTheLinkIdentifier() {
        val post = postFor("https://bnctestbed.app.link/abc123?link_click_id=xyz789")

        assertEquals("xyz789", post.optString("link_identifier"))
    }

    @Test
    fun aSchemeUriCarriesItsLinkClickIdToo() {
        // The click id is lifted before the scheme is looked at, so it is not an
        // https-only path. W2 delivers a scheme URI without one, which is why this
        // case has no scenario behind it.
        val post = postFor("branchtest://open?link_click_id=xyz789")

        assertEquals("xyz789", post.optString("link_identifier"))
        assertEquals("branchtest://open?link_click_id=xyz789", post.optString("external_intent_uri"))
    }

    @Test
    fun withoutAClickIdTheFieldIsAbsent() {
        val post = postFor("https://bnctestbed.app.link/abc123")

        assertFalse(
            "an absent click id must leave the field off, not send it empty",
            post.has("link_identifier"),
        )
    }

    @Test
    fun aNullUriCarriesNeitherField() {
        // The organic launch: MainActivity calls handleDeepLink unconditionally, with
        // null data when no link opened the app. N1's capture is this case on the wire.
        val post = postFor(null)

        assertFalse(post.has("android_app_link_url"))
        assertFalse(post.has("external_intent_uri"))
        assertFalse(post.has("link_identifier"))
    }

    @Test
    fun theTwoUriFieldsAreNeverBothPresent() {
        // The invariant behind W1 and W2 contracting external_intent_uri in opposite
        // directions: exactly one of the two describes any given request.
        for (uri in listOf(
            "https://bnctestbed.app.link/abc123",
            "http://bnctestbed.app.link/abc123",
            "branchtest://open",
        )) {
            val post = postFor(uri)
            assertTrue(
                "exactly one URI field must be set for $uri",
                post.has("android_app_link_url") != post.has("external_intent_uri"),
            )
        }
    }
}
