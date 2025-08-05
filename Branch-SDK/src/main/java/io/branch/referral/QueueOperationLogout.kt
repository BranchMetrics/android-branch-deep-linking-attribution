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
        v("onRequestSucceeded $this")
        if (callback_ != null) {
            callback_!!.onLogoutFinished(true, null)
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
