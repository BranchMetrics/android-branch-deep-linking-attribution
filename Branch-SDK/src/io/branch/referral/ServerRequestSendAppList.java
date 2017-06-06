package io.branch.referral;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for sending installed application details to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestSendAppList extends ServerRequest {

    /**
     * <p>Creates an instance of {@link ServerRequestSendAppList } to get the following details and report them to the
     * Branch API <b>once a week</b>:</p>
     * <pre style="background:#fff;padding:10px;border:2px solid silver;">
     * int interval = 7 * 24 * 60 * 60;
     * appListingSchedule_ = scheduler.scheduleAtFixedRate(
     * periodicTask, (days * 24 + hours) * 60 * 60, interval, TimeUnit.SECONDS);</pre>
     * <ul>
     * <li>{@link PrefHelper#getAppKey()}</li>
     * <li>{@link SystemObserver#getOS()}</li>
     * <li>{@link PrefHelper#getDeviceFingerPrintID()}</li>
     * <li>{@link SystemObserver#getListOfApps()}</li>
     * </ul>
     *
     * @param context Current {@link Application} context
     * @see {@link SystemObserver}
     * @see {@link PrefHelper}
     */
    public ServerRequestSendAppList(Context context) {
        super(context, Defines.RequestPath.SendAPPList.getPath());

        SystemObserver sysObserver = new SystemObserver(context);
        JSONObject post = new JSONObject();
        try {
            post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            post.put("apps_data", sysObserver.getListOfApps());
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ServerRequestSendAppList(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        prefHelper_.clearSystemReadStatus();
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        //No implementation on purpose
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            Log.i("BranchSDK", "Trouble executing your request. Please add 'android.permission.INTERNET' in your applications manifest file");
            return true;
        }
        return false;
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
