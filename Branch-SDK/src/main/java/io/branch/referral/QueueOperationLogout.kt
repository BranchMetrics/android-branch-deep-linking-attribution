package io.branch.referral

import android.content.Context
import io.branch.referral.Branch.LogoutStatusListener
import io.branch.referral.BranchLogger.e
import io.branch.referral.BranchLogger.v
import io.branch.referral.Defines.RequestPath

/**
 * This class implements the logout feature through the legacy queueing system.
 * To maintain queue compatibility and reduce complexity, do not invoke network task as operation is
 * completely client side.
 */
class QueueOperationLogout(
    var context_: Context,
    requestPath: RequestPath?,
    var callback_: LogoutStatusListener?
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
            val prefHelper_ = PrefHelper.getInstance(context_)
            prefHelper_.identity = PrefHelper.NO_STRING_VALUE
            v("Identity set to: " + prefHelper_.identity)
            prefHelper_.clearUserValues()
        } catch (e: Exception) {
            e("Caught Exception: doFinalUpdateOnMainThread " + this + " " + e.message)
        }
    }

    override fun handleErrors(context: Context): Boolean {
        return false
    }

    override fun onRequestSucceeded(response: ServerResponse, branch: Branch) {
        v("QueueOperationLogout onRequestSucceeded $this")
            //On Logout clear the link cache and all pending requests
            Branch.getInstance().linkCache_.clear();
            Branch.getInstance().requestQueue_.clear();
        if (callback_ != null) {
            callback_!!.onLogoutFinished(true, null)
        }
    }

    override fun handleFailure(statusCode: Int, causeMsg: String) {
        v("QueueOperationLogout handleFailure $this")
        if (callback_ != null) {
            v("QueueOperationLogout handleFailure $this")
            val currentIdentity = prefHelper_.identity
            val error = BranchError("Error in logout method. Current identity value: $currentIdentity $causeMsg", -1)
            callback_!!.onLogoutFinished(true, error)
        }
    }

    override fun isGetRequest(): Boolean {
        return false
    }

    override fun clearCallbacks() {
    }

    // For backwards compat with deprecated disableTracking
    override fun prepareExecuteWithoutTracking(): Boolean {
        return true
    }
}
