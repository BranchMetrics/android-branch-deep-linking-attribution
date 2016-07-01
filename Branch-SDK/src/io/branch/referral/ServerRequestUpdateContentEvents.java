package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for registering an app open event to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestUpdateContentEvents extends ServerRequest {

    /**
     * <p>Create an instance of {@link ServerRequestRegisterInstall} to notify Branch API on app open event.</p>
     *
     * @param context     Current {@link Application} context
     */
    public ServerRequestUpdateContentEvents(Context context) {
        super(context, Defines.RequestPath.ContentEvent.getPath());


        JSONObject openPost = new JSONObject();
        try {
            openPost.put(Defines.Jsonkey.AppIdentifier.getKey(), context.getPackageName());
            openPost.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            openPost.put(Defines.Jsonkey.ContentEvents.getKey(), prefHelper_.getBranchAnalyticsData());
            setPost(openPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }

    public ServerRequestUpdateContentEvents(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        prefHelper_.clearBranchAnalyticsData();
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
