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
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestOpen Succeeded. Response: ${response.`object`}")

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
                // Write the slot only when this response actually carries session data. The
                // v3/events/open response has no "data" key, link-driven or organic, so writing
                // whatever it says always cleared the payload a deep link resolution had just
                // persisted. A data-less response leaves the slot as it stands; if the endpoint
                // starts returning session data, the write resumes on its own.
                if (responseJson.has(Defines.Jsonkey.Data.key)) {
                    prefHelper_.sessionParams = responseJson.getString(Defines.Jsonkey.Data.key)
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