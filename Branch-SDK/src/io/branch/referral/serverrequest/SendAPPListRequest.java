package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.SystemObserver;
import io.branch.referral.errors.BranchInternetPermissionError;

/**
 * * <p>
 * The server request for sending installed application details to Branch API. Handles request creation and execution.
 * </p>
 */
public class SendAppListRequest extends ServerRequest {

    /**
     * <p>Creates an instance of {@link SendAppListRequest } to get the following details and report them to the
     * Branch API <b>once a week</b>:</p>
     * <p/>
     * <pre style="background:#fff;padding:10px;border:2px solid silver;">
     * int interval = 7 * 24 * 60 * 60;
     * appListingSchedule_ = scheduler.scheduleAtFixedRate(
     * periodicTask, (days * 24 + hours) * 60 * 60, interval, TimeUnit.SECONDS);</pre>
     * <p/>
     * <ul>
     * <li>{@link SystemObserver#getAppKey()}</li>
     * <li>{@link SystemObserver#getOS()}</li>
     * <li>{@link SystemObserver#getDeviceFingerPrintID()}</li>
     * <li>{@link SystemObserver#getListOfApps()}</li>
     * </ul>
     *
     * @param context Current {@link Application} context
     * @see {@link SystemObserver}
     * @see {@link PrefHelper}
     */
    public SendAppListRequest(Context context) {
        super(context, Defines.RequestPath.SendAPPList.getPath());

        SystemObserver sysObserver = new SystemObserver(context);
        JSONObject post = new JSONObject();
        try {
            if (!sysObserver.getOS().equals(SystemObserver.BLANK))
                post.put("os", sysObserver.getOS());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("apps_data", sysObserver.getListOfApps());
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public SendAppListRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        prefHelper_.clearSystemReadStatus();
    }

    @Override
    public void handleFailure(boolean isInitNotStarted) {
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
}
