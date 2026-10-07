package io.branch.referral

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

internal class RequestDeepLink(
    context: Context,
    callback: Branch.BranchReferralInitListener?,
    isAutoInitialization: Boolean
) : ServerRequestInitSession(context, Defines.RequestPath.Deeplink, isAutoInitialization) {

    // Set once sendOpenAfterDeepLink has this request's response; containsDeepLink() then skips it.
    @JvmField @Volatile var responseHandled = false

    // Set under Branch's heldOpenLock_ by a background: its open is sent when it responds, without sendOpen.
    @JvmField @Volatile var sendOpenWhenAnswered = false

    // The launch link when requestDeepLinkData was called. Sent instead of the saved one, which a later
    // call or consent off can change before this request is sent.
    private var launchLink: JSONObject? = null

    fun keepLaunchLink() {
        launchLink = savedLaunchLink(prefHelper_)
    }

    override fun doFinalUpdateOnBackgroundThread() {
        super.doFinalUpdateOnBackgroundThread()
        // Only the fields it had: on a first install, the install referrer read adds link_identifier later.
        launchLink?.let { link -> link.keys().forEach { post.put(it, link.get(it)) } }
    }

    /** The launch link fields this request sent, for its open: by then the saved ones may be a later call's. */
    fun sentLaunchLink(): JSONObject {
        val link = JSONObject()
        for (key in LAUNCH_LINK_KEYS) {
            post.opt(key)?.let { link.put(key, it) }
        }
        return link
    }

    init {
        callback_ = callback
        try {
            val deepLinkPost = JSONObject()

            val rdt = prefHelper_.randomizedDeviceToken
            if(rdt != PrefHelper.NO_STRING_VALUE) {
                deepLinkPost.put(
                    Defines.Jsonkey.RandomizedDeviceToken.key,
                    rdt
                )
            }

            val rbt = prefHelper_.randomizedBundleToken
            if(rbt != PrefHelper.NO_STRING_VALUE) {
                deepLinkPost.put(
                    Defines.Jsonkey.RandomizedBundleToken.key,
                    rbt
                )
            }
            setPost(deepLinkPost)
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestDeepLink Succeeded. Response: ${response.`object`.toString(2)}")

        try {
            val responseJson = response.`object`

            if (responseJson.has(Defines.Jsonkey.LinkClickID.key)) {
                prefHelper_.linkClickID = responseJson.getString(Defines.Jsonkey.LinkClickID.key)
            } else {
                prefHelper_.linkClickID = PrefHelper.NO_STRING_VALUE
            }

            if (responseJson.has(Defines.Jsonkey.Data.key)) {
                val params = responseJson.getString(Defines.Jsonkey.Data.key)
                prefHelper_.sessionParams = params
                // An install from a link keeps that link's params as the first referring params.
                if (isInstallLaunch() && prefHelper_.installParams == PrefHelper.NO_STRING_VALUE &&
                    JSONObject(params).optBoolean(Defines.Jsonkey.Clicked_Branch_Link.key)) {
                    prefHelper_.installParams = params
                }
            } else {
                prefHelper_.sessionParams = PrefHelper.NO_STRING_VALUE
            }

            // With setAutomaticOpenEvents(false), before the callback, so a sendOpen made in it finds the response.
            if (!branch.automaticOpenEvents_) {
                branch.sendOpenAfterDeepLink(this, response.`object`)
            }

            if (callback_ != null) {
                callback_!!.onInitFinished(branch.latestReferringParams, null)
            }

            prefHelper_.appVersion = prefHelper_.appVersion

        } catch (ex: Exception) {
            BranchLogger.w("Caught Exception processing RequestDeepLink response: ${ex.message}")
        }

        onInitSessionCompleted(response, branch)

        if (!responseHandled) {
            branch.sendOpenAfterDeepLink(this, response.`object`)
        }
    }

    override fun handleFailure(statusCode: Int, causeMsg: String) {
        val serverErrorMessage = "Request DeepLink failed with HTTP code: $statusCode. Server says: $causeMsg"
        BranchLogger.e(serverErrorMessage)

        // A failed /v3/deeplink's open has no link_data.
        if (!responseHandled) {
            Branch.getInstance().sendOpenAfterDeepLink(this, null)
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

    override fun getRequestActionName(): String = "deeplink"

    override fun isGetRequest(): Boolean = false

    override fun clearCallbacks() {
        callback_ = null
    }

    override fun shouldRetryOnFail(): Boolean = false

    override fun handleErrors(context: Context): Boolean = !doesAppHasInternetPermission(context)
}