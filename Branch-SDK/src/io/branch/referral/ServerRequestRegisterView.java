package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import io.branch.referral.indexing.RegisterViewBuilder;

/**
 * * <p>
 * The server request for registering a view event of a specific content specified by user given attributes
 * </p>
 */
class ServerRequestRegisterView extends ServerRequest {

    RegisterViewBuilder.RegisterViewStatusListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestRegisterView} to notify Branch on a content view event.</p>
     *
     * @param currentActivity current Activity
     * @param builder         An instance of {@link RegisterViewBuilder} to create the register view request
     */
    public ServerRequestRegisterView(Activity currentActivity, RegisterViewBuilder builder, ArrayList<String> activityStack) {
        super(currentActivity.getApplicationContext(), Defines.RequestPath.RegisterView.getPath());

        callback_ = builder.getCallback();
        JSONObject registerViewPost;
        try {
            registerViewPost = createContentViewJson(currentActivity, builder, activityStack);
            setPost(registerViewPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        if (callback_ != null) {
            callback_.onRegisterViewFinished(true, null);
        }
    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            callback_.onRegisterViewFinished(false, new BranchError("Unable to register content view", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onRegisterViewFinished(false, new BranchError("Unable to register content view", BranchError.ERR_NO_INTERNET_PERMISSION));
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
        callback_ = null;
    }


    /**
     * Creates a Json with given parameters for register view.
     *
     * @param activity      Current Activity
     * @param builder       An instance of {@link RegisterViewBuilder} to create the Json
     * @param ActivityStack Activity stack for creating the path of the activity
     * @return A {@link JSONObject} for post data for register view request
     * @throws JSONException {@link JSONException} on any Json errors
     */
    private JSONObject createContentViewJson(Activity activity, RegisterViewBuilder builder
            , ArrayList<String> ActivityStack) throws JSONException {

        JSONObject contentObject = new JSONObject();

        String os_Info = "Android " + Build.VERSION.SDK_INT;
        String sessionID = prefHelper_.getSessionID();
        Context applicationContext = activity.getApplicationContext();

        String urlString = activity.getApplicationContext().getPackageName() + ":" + activity.getClass().getSimpleName();
        String date = String.valueOf(System.currentTimeMillis());
        String path = "";
        for (String pathContent : ActivityStack) {
            path += pathContent + "\\";
        }

        contentObject.put(Defines.Jsonkey.OS.getKey(), os_Info);
        contentObject.put(Defines.Jsonkey.SessionID.getKey(), sessionID);
        contentObject.put(Defines.Jsonkey.ContentScreen.getKey(), urlString);
        contentObject.put(Defines.Jsonkey.EventTime.getKey(), date);
        contentObject.put(Defines.Jsonkey.ContentPath.getKey(), path);
        String appVersion;
        try {
            appVersion = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), PackageManager.MATCH_DEFAULT_ONLY).versionName;
            contentObject.put(Defines.Jsonkey.AppVersion.getKey(), appVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        JSONObject extrasObj = new JSONObject();
        if (activity.getIntent().getExtras() != null) {
            Set<String> extraKeys = activity.getIntent().getExtras().keySet();
            for (String extraKey : extraKeys) {
                Object value = activity.getIntent().getExtras().get(extraKey);
                if (value instanceof String
                        || value instanceof Integer
                        || value instanceof Boolean) {
                    extrasObj.put(extraKey, value.toString());
                }
            }
        }
        if (builder != null) {
            extrasObj.put(Defines.Jsonkey.ContentID.getKey(), builder.getContentId());
            extrasObj.put(Defines.Jsonkey.ContentTitle.getKey(), builder.getContentTitle());
            extrasObj.put(Defines.Jsonkey.ContentDesc.getKey(), builder.getContentDesc());
            extrasObj.put(Defines.Jsonkey.ContentImgUrl.getKey(), builder.getContentImgUrl());
        }
        contentObject.put(Defines.Jsonkey.Params.getKey(), extrasObj);

        if (builder != null && builder.getAdditionalParams() != null) {
            HashMap extras = builder.getAdditionalParams();
            Set extraKeys = extras.keySet();
            JSONObject reportExtraObj = new JSONObject();
            for (Object key : extraKeys) {
                reportExtraObj.put((String) key, extras.get(key));
            }
            contentObject.put(Defines.Jsonkey.Extra.getKey(), reportExtraObj);
        }

        return contentObject;
    }
}
