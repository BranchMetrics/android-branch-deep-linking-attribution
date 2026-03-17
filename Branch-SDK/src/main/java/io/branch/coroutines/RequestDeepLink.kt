package io.branch.coroutines

import android.content.Context
import android.net.Uri
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.Defines
import io.branch.referral.PrefHelper
import io.branch.referral.ServerRequestInitSession
import io.branch.referral.ServerResponse
import org.json.JSONException
import org.json.JSONObject

internal class RequestDeepLink(
    context: Context,
    private val uri: Uri?,
    callback: Branch.BranchReferralInitListener?,
    isAutoInitialization: Boolean
) : ServerRequestInitSession(context, Defines.RequestPath.RegisterOpen, isAutoInitialization) {

    init {
        callback_ = callback
        try {
            val openPost = JSONObject()

            // Adding the URI to the payload as the server expects
            uri?.let {
                openPost.put(Defines.Jsonkey.AndroidAppLinkURL.key, it.toString())
            }

            openPost.put(Defines.Jsonkey.RandomizedDeviceToken.key, prefHelper_.randomizedDeviceToken)
            openPost.put(Defines.Jsonkey.RandomizedBundleToken.key, prefHelper_.randomizedBundleToken)
            setPost(openPost)
        } catch (ex: JSONException) {
            BranchLogger.w("Caught JSONException ${ex.message}")
            constructError_ = true
        }
    }

    override fun getRequestUrl(): String {
        return "https://rlogan-go-gateway.eks-stage-puffin-usw2.branch.io/v1/deeplink"
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        super.onRequestSucceeded(response, branch)
        BranchLogger.v("RequestDeepLink Succeeded. Response: ${response.`object`}")

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
            } else {
                prefHelper_.sessionParams = PrefHelper.NO_STRING_VALUE
            }

            if (callback_ != null) {
                callback_!!.onInitFinished(branch.latestReferringParams, null)
            }

            prefHelper_.appVersion = prefHelper_.appVersion

        } catch (ex: Exception) {
            BranchLogger.w("Caught Exception processing RequestDeepLink response: ${ex.message}")
        }

        onInitSessionCompleted(response, branch)
    }

    override fun handleFailure(statusCode: Int, causeMsg: String) {
        val serverErrorMessage = "Request DeepLink failed with HTTP code: $statusCode. Server says: $causeMsg"
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

    override fun getRequestActionName(): String = "deeplink"

    override fun isGetRequest(): Boolean = false

    override fun clearCallbacks() {
        callback_ = null
    }

    override fun shouldRetryOnFail(): Boolean = false

    override fun handleErrors(context: Context): Boolean = !doesAppHasInternetPermission(context)
}