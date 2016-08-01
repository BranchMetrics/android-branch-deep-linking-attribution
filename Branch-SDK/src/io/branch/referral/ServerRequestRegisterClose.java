package io.branch.referral;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for closing any open session. Handles request creation and execution.
 * </p>
 */
class ServerRequestRegisterClose extends ServerRequest {

    /**
     * <p>Perform the state-safe actions required to terminate any open session, and report the
     * closed application event to the Branch API.</p>
     *
     * @param context Current {@link Application} context
     */
    public ServerRequestRegisterClose(Context context) {
        super(context, Defines.RequestPath.RegisterClose.getPath());
        JSONObject closePost = new JSONObject();
        try {
            closePost.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            closePost.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
            closePost.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                closePost.put(Defines.Jsonkey.LinkClickID.getKey(), prefHelper_.getLinkClickID());
            }
            JSONObject ciObject = ContentDiscoverer.getInstance().getContentDiscoverDataForCloseRequest(context);
            if (ciObject != null) {
                closePost.put(Defines.Jsonkey.ContentDiscovery.getKey(), ciObject);
            }
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                String packageName = info.packageName;
                if (!TextUtils.isEmpty(info.versionName)) {
                    closePost.put(Defines.Jsonkey.AppVersion.getKey(), info.versionName);
                }
            } catch (PackageManager.NameNotFoundException ignore) {
            }
            setPost(closePost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ServerRequestRegisterClose(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
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
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        // Clear the latest session params on close
        prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
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
