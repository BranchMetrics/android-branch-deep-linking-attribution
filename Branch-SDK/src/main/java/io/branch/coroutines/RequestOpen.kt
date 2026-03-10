package io.branch.referral

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

internal class RequestOpen(
    context: Context,
    callback: Branch.BranchReferralInitListener?,
    isAutoInitialization: Boolean
) : ServerRequestInitSession(context, Defines.RequestPath.RegisterOpen, isAutoInitialization) {

    init {
        callback_ = callback
        try {
            val openPost = JSONObject()
            openPost.put(Defines.Jsonkey.RandomizedDeviceToken.key, prefHelper_.randomizedDeviceToken)
            openPost.put(Defines.Jsonkey.RandomizedBundleToken.key, prefHelper_.randomizedBundleToken)
            setPost(openPost)
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    override fun getRequestUrl(): String {
        return "https://rlogan-go-gateway.eks-stage-puffin-usw2.branch.io/v1/open"
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestOpen Succeeded. Response: ${response.`object`}")

        try {
            val responseJson = response.`object`

            if (responseJson.has(Defines.Jsonkey.LinkClickID.key)) {
                prefHelper_.linkClickID = responseJson.getString(Defines.Jsonkey.LinkClickID.key)
            } else {
                prefHelper_.linkClickID = PrefHelper.NO_STRING_VALUE
            }

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

                if (callback_ != null && !branch.isIDLSession) {
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

        if (callback_ != null && Branch.getInstance()?.isIDLSession == false) {
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