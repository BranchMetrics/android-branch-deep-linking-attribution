package io.branch.referral

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/** The request fields of the launch link, the ones PrefHelper.clearLaunchLink clears. */
internal val LAUNCH_LINK_KEYS = listOf(
    Defines.Jsonkey.LinkIdentifier.key,
    Defines.Jsonkey.AndroidAppLinkURL.key,
    Defines.Jsonkey.AndroidPushIdentifier.key,
    Defines.Jsonkey.External_Intent_URI.key,
    Defines.Jsonkey.External_Intent_Extra.key
)

/** The saved launch link fields, as request fields. */
internal fun savedLaunchLink(prefHelper: PrefHelper): JSONObject {
    val link = JSONObject()
    val values = listOf(
        prefHelper.linkClickIdentifier,
        prefHelper.appLink,
        prefHelper.pushIdentifier,
        prefHelper.externalIntentUri,
        prefHelper.externalIntentExtra
    )
    LAUNCH_LINK_KEYS.zip(values).forEach { (key, value) ->
        if (!value.isNullOrBlank() && value != PrefHelper.NO_STRING_VALUE) link.put(key, value)
    }
    return link
}

/** Replaces the launch link fields in post with link's. */
internal fun putLaunchLink(post: JSONObject, link: JSONObject) {
    for (key in LAUNCH_LINK_KEYS) {
        if (link.has(key)) post.put(key, link.get(key)) else post.remove(key)
    }
}

/** @param launchLink The launch link fields its /v3/deeplink sent, or null to send the saved ones. */
internal class RequestOpen @JvmOverloads constructor(
    context: Context,
    callback: Branch.BranchReferralInitListener?,
    isAutoInitialization: Boolean,
    responseData: JSONObject?,
    private val launchLink: JSONObject? = null
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
            if(responseData != null && responseData.has("data")){
                val dataString = responseData.getString("data")
                val dataJSON = JSONObject(dataString)
                // Only a matched link: api-open records any link_data as referring link data.
                if (dataJSON.optBoolean(Defines.Jsonkey.Clicked_Branch_Link.key)) {
                    openPost.put("link_data", dataJSON)
                }
            }
            setPost(openPost)
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    override fun doFinalUpdateOnBackgroundThread() {
        super.doFinalUpdateOnBackgroundThread()
        launchLink?.let { putLaunchLink(post, it) }
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestOpen Succeeded. Response: ${response.`object`}")

        try {
            val responseJson = response.`object`

            if (responseJson.has(Defines.Jsonkey.Link.key)) {
                prefHelper_.userURL = responseJson.getString(Defines.Jsonkey.Link.key)
            }

            // TODO: Activation: open the enhanced web link UX from the /v3/deeplink response and ignore it here.
            // api-open sends invoke_features on both responses today.
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
            }

            // Also after the web page opens: the open was sent, and sendOpen waits for this.
            if (callback_ != null) {
                // EMT-3860: latestReferringParams now carries the resolved deep link data,
                // because the open POST includes external_intent_uri (via the inherited
                // ServerRequestInitSession.onPreExecute) so the server resolves the click and
                // returns link_data in the response.
                callback_!!.onInitFinished(branch.latestReferringParams, null)
            }

            prefHelper_.appVersion = DeviceInfo.getInstance()?.appVersion ?: ""

        } catch (ex: Exception) {
            BranchLogger.w("Caught Exception processing RequestOpen response: ${ex.message}")
        }

        // The launch is counted: its install and intent data must not ride the next launch.
        branch.requestQueue_.postInitClear()

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