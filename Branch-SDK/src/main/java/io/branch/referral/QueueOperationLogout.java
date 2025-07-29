package io.branch.referral;

import android.content.Context;

/**
 * This class implements the logout feature through the legacy queueing system.
 * To maintain queue compatibility and reduce complexity, do not invoke network task as operation is
 * completely client side.
 */
public class QueueOperationLogout extends ServerRequest{
    Branch.LogoutStatusListener callback_;
    Context context_;

    public QueueOperationLogout(Context context, Defines.RequestPath requestPath, Branch.LogoutStatusListener callback) {
        super(context, requestPath);
        context_ = context;
        callback_ = callback;
    }

    @Override
    public void onPreExecute() {
    }

    @Override
    public void doFinalUpdateOnBackgroundThread(){
    }

    @Override
    public void doFinalUpdateOnMainThread() {
        BranchLogger.v("doFinalUpdateOnMainThread " + this);
        PrefHelper prefHelper_ = PrefHelper.getInstance(context_);
        prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
        prefHelper_.clearUserValues();
    }


    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        BranchLogger.v("onRequestSucceeded " + this);
        if (callback_ != null) {
            callback_.onLogoutFinished(true, null);
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
    }
}
