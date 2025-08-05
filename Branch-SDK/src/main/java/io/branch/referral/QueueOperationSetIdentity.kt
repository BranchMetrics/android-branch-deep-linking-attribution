package io.branch.referral

import android.content.Context
import io.branch.referral.Branch.BranchReferralInitListener
import io.branch.referral.BranchLogger.e
import io.branch.referral.BranchLogger.v
import io.branch.referral.Defines.RequestPath
import org.json.JSONObject

/**
 * This class implements the setIdentity feature through the legacy queueing system.
 * To maintain queue compatibility and reduce complexity, do not invoke network task as operation is
 * completely client side.
 */
class QueueOperationSetIdentity(
    var context_: Context,
    requestPath: RequestPath?,
    var userId_: String?,
    var callback_: BranchReferralInitListener?
) :
    ServerRequest(context_, requestPath) {
    override fun onPreExecute() {
        //NO-OP
    }

    public override fun doFinalUpdateOnBackgroundThread() {
        //NO-OP
    }

    public override fun doFinalUpdateOnMainThread() {
        try {
            v("doFinalUpdateOnMainThread $this")
            if (userId_ != null && userId_ != prefHelper_.identity) {
                Branch.installDeveloperId = userId_
                prefHelper_.identity = userId_
                v("Identity set to:" + prefHelper_.identity)
            }
        } catch (e: Exception) {
            e("Caught Exception: doFinalUpdateOnMainThread " + this + " " + e.message)
        }
    }


    override fun handleErrors(context: Context): Boolean {
        return false
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        v("onRequestSucceeded $this")
        if (callback_ != null) {
            var latestReferringParams: JSONObject? = null
            try {
                latestReferringParams = branch.firstReferringParams
            } catch (e: Exception) {
                e("Caught exception " + this + " onRequestSucceeded: " + e.message)
            }
            callback_!!.onInitFinished(latestReferringParams, null)
        }
    }

    override fun handleFailure(statusCode: Int, causeMsg: String) {
    }

    override fun isGetRequest(): Boolean {
        return false
    }

    override fun clearCallbacks() {
    }
}
