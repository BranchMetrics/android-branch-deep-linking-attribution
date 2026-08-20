package io.branch.referral

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

internal class RequestOpen(
    context: Context,
    callback: Branch.BranchReferralInitListener?,
    isAutoInitialization: Boolean,
    responseData: JSONObject?
) : ServerRequestInitSession(context, Defines.RequestPath.EventsOpen, isAutoInitialization) {

    /** True when this request carries an initialization_context that the server has yet to ack. */
    private var carriedInitializationContext = false

    init {
        callback_ = callback
        try {
            val openPost = JSONObject()
            val rdt = prefHelper_.randomizedDeviceToken
            if(rdt != PrefHelper.NO_STRING_VALUE) {
                openPost.put(
                    Defines.Jsonkey.RandomizedDeviceToken.key,
                    rdt
                )
            }

            val rbt = prefHelper_.randomizedBundleToken
            if(rbt != PrefHelper.NO_STRING_VALUE) {
                openPost.put(
                    Defines.Jsonkey.RandomizedBundleToken.key,
                    rbt)
            }
            // TODO: Move registerAppInit tasks here
            if(responseData != null && responseData.has("data")){
                val dataString = responseData.getString("data")
                val dataJSON = JSONObject(dataString)
                openPost.put("link_data", dataJSON)
            }
            setPost(openPost)

            // Cover branch_sdk_request_timestamp / branch_sdk_request_unique_id in the signature.
            addClientRequestParameters()

            // The secure context is attached in applySecureContext(), not here — see there for why.
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    /**
     * First open carries initialization_context only; every later open carries activity_context
     * only. The two are never sent together.
     *
     * Runs from [doFinalUpdateOnBackgroundThread], after the host has finished writing
     * `hardware_id`, `advertising_ids`, `install_referrer_extras`, `app_store` and the rest — so the
     * canonical hashed here is the canonical the Gateway will rebuild from the posted bytes. It also
     * means Layer 1 no longer blocks the main thread: attestation can cost ~10s on the first Play
     * Integrity call after process start.
     */
    override fun applySecureContext() {
        val provider = Branch.getInstance()?.fraudDefenseProvider ?: return
        if (!prefHelper_.getBool("bnc_device_trust_checked")) {
            // Layer 1: attestation. Retried on the next open if it fails.
            try {
                val trustFields = provider.addDeviceTrustParams(post)
                if (trustFields != null) {
                    // Not marked done here: the flag is what makes every later request use
                    // Layer 2 instead, and the server can only verify those signatures once
                    // it has actually received this initialization_context. Setting it at
                    // build time strands the device if the open never lands.
                    carriedInitializationContext = true
                    SecureContextApplier.apply(trustFields, post)
                }
            } catch (e: Exception) {
                BranchLogger.w("Fraud defense failed for open: ${e.message}")
            }
        } else {
            // Layer 2 + 3: HMAC + nonce
            try {
                val sigFields = provider.addSignatureAndNonceForParams(post)
                if (sigFields != null) {
                    SecureContextApplier.apply(sigFields, post)
                }
            } catch (e: Exception) {
                BranchLogger.w("Fraud defense signature failed for open: ${e.message}")
            }
        }
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestOpen Succeeded. Response: ${response.`object`}")

        // The server has the initialization_context now, so later requests can switch to Layer 2.
        if (carriedInitializationContext) {
            prefHelper_.setBool("bnc_device_trust_checked", true)
            Branch.getInstance()?.fraudDefenseProvider?.releaseAttestationClaim()
            BranchLogger.v("Device trust attestation acknowledged by the server")
        }

        try {
            val responseJson = response.`object`

            // TODO: Should be put under v3/deeplink
            // Check for enhanced web link UX override
            if (responseJson.has(Defines.Jsonkey.Invoke_Features.key) &&
                responseJson.getJSONObject(Defines.Jsonkey.Invoke_Features.key).has("enhanced_web_link_ux")) {

                val invokeFeaturesJson = responseJson.getJSONObject(Defines.Jsonkey.Invoke_Features.key)
                BranchLogger.v("Opening browser from open request.")
                branch.openBrowserExperience(invokeFeaturesJson)

            } else {
                if (responseJson.has(Defines.Jsonkey.Data.key)) {
                    val params = responseJson.getString(Defines.Jsonkey.Data.key)
                    prefHelper_.sessionParams = params
                } else {
                    prefHelper_.sessionParams = PrefHelper.NO_STRING_VALUE
                }

                if (callback_ != null) {
                    // EMT-3860: latestReferringParams now carries the resolved deep link data,
                    // because the open POST includes external_intent_uri (via the inherited
                    // ServerRequestInitSession.onPreExecute) so the server resolves the click and
                    // returns link_data in the response.
                    callback_!!.onInitFinished(branch.latestReferringParams, null)
                }
            }

            prefHelper_.appVersion = DeviceInfo.getInstance()?.appVersion ?: ""

        } catch (ex: Exception) {
            BranchLogger.w("Caught Exception processing RequestOpen response: ${ex.message}")
        }

        onInitSessionCompleted(response, branch)
    }

    override fun handleFailure(statusCode: Int, causeMsg: String) {
        val serverErrorMessage = "Request Open failed with HTTP code: $statusCode. Server says: $causeMsg"
        BranchLogger.e(serverErrorMessage)

        // shouldRetryOnFail is false, so this attestation is spent. Hand the claim back or no
        // later open can attest and the device never registers.
        if (carriedInitializationContext) {
            Branch.getInstance()?.fraudDefenseProvider?.releaseAttestationClaim()
            BranchLogger.v("Open carrying the initialization_context failed; attestation will be retried on the next open")
        }

        if (callback_ != null) {
            val obj = JSONObject()
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes")
            } catch (ex: Exception) {
                BranchLogger.w("Caught JSONException ${ex.message}")
            }
            callback_!!.onInitFinished(
                obj,
                BranchError("Trouble initializing Branch. $this failed. $causeMsg", statusCode)
            )
        }
    }

    override fun getRequestActionName(): String = ACTION_OPEN

    override fun isGetRequest(): Boolean = false

    override fun clearCallbacks() {
        callback_ = null
    }

    override fun shouldRetryOnFail(): Boolean = false

    override fun handleErrors(context: Context): Boolean = !doesAppHasInternetPermission(context)
}