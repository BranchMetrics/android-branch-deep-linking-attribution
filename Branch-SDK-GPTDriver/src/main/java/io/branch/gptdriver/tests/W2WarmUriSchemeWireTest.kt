package io.branch.gptdriver.tests

import org.junit.Test

/**
 * warm_uriScheme: warm_https_onNewIntent's launch state, entered through the URI scheme
 * instead of https.
 *
 * Same shape as W1WarmHttpsWireTest: launch, generate a link so the device is a returning
 * one, background, deliver, settle. Only the delivered URI differs.
 *
 * The scheme changes which field carries the URI. RequestDeepLink puts an http or https URI
 * in android_app_link_url and everything else in external_intent_uri, and lifts a
 * link_click_id query parameter into link_identifier when one is present. A bare
 * branchtest:// URI therefore reaches /v3/deeplink with external_intent_uri set and no
 * link_identifier, which is what this drives and what its contract records.
 *
 * The URI form follows ReferringUrlUtilityTests, which uses branchtest://home and
 * branchtest://?gclid=12345. The TestBed's button generates https links only, so there is no
 * scheme link with a real click id to deliver here.
 *
 * No ActivityScenarioRule, for the reason W1WarmHttpsWireTest documents: the rule's after()
 * closes a scenario that has lost lifecycle control once a new intent arrived through
 * startActivity.
 *
 * Produces no assertion of its own. The capture is the output.
 */
class W2WarmUriSchemeWireTest {

    private val driver = WarmScenarioDriver()

    @Test
    fun warmUriSchemeLinkEmitsWirePayload() {
        driver.launch()
        // Not read back: the generated link is not delivered here, only the scheme URI is.
        driver.generateLink()
        driver.background()
        driver.deliver(SCHEME_URI)
    }

    private companion object {
        const val SCHEME_URI = "branchtest://open"
    }
}
