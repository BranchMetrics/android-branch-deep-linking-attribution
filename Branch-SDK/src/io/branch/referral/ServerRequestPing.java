package io.branch.referral;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * * <p>
 * The server request for pinging the API to process the queue.
 * </p>
 */
public class ServerRequestPing extends ServerRequest {

    /**
     * <p>Configures a server request to process the request queue.</p>
     *
     * @param context Current {@link Application} context
     */
    public ServerRequestPing(Context context) {
        super(context, null);
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            PrefHelper.Debug("BranchSDK", "Trouble executing your request. Please add 'android.permission.INTERNET' in your applications manifest file");
            return true;
        }
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        //No implementation on purpose
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        //No implementation on purpose
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        //No implementation on purpose
    }
}
