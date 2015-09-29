package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

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
     * @param builder An instance of {@link RegisterViewBuilder} to create the register view request
     */
    public ServerRequestRegisterView(RegisterViewBuilder builder, SystemObserver sysObserver) {
        super(builder.getContainerActivity().getApplicationContext(), Defines.RequestPath.RegisterView.getPath());
        callback_ = builder.getCallback();
        JSONObject registerViewPost;
        try {
            registerViewPost = createContentViewJson(builder, sysObserver);
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
    public boolean isUpdateGAParams() {
        return true; // Since register view needs GA params
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }

    @Override
    public String getRequestUrl() {
        return "http://54.153.121.111:5000/v0.1/register_view/";
    }

    /**
     * Creates a Json with given parameters for register view.
     *
     * @param builder An instance of {@link RegisterViewBuilder} to create the Json
     * @return A {@link JSONObject} for post data for register view request
     * @throws JSONException {@link JSONException} on any Json errors
     */
    private JSONObject createContentViewJson(RegisterViewBuilder builder,
                                             SystemObserver sysObserver) throws JSONException {

        JSONObject contentObject = new JSONObject();
        JSONObject extrasObj = new JSONObject();

        String os_Info = "Android " + Build.VERSION.SDK_INT;
        String sessionID = prefHelper_.getSessionID();
        if (builder.getContainerActivity() != null) {
            Activity activity = builder.getContainerActivity();
            Context applicationContext = activity.getApplicationContext();
            String urlString = applicationContext.getPackageName() + ":" + activity.getClass().getSimpleName();
            contentObject.put(Defines.Jsonkey.ContentScreen.getKey(), urlString);
            String appVersion;
            try {
                appVersion = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), PackageManager.MATCH_DEFAULT_ONLY).versionName;
                contentObject.put(Defines.Jsonkey.AppVersion.getKey(), appVersion);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (activity.getIntent() != null &&
                    activity.getIntent().getExtras() != null) {
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
        }

        String date = String.valueOf(System.currentTimeMillis());
        contentObject.put(Defines.Jsonkey.OS.getKey(), os_Info);
        contentObject.put(Defines.Jsonkey.SessionID.getKey(), sessionID);

        contentObject.put(Defines.Jsonkey.EventTime.getKey(), date);
        contentObject.put(Defines.Jsonkey.ContentPath.getKey(), builder.getContentPath());

        String uniqId = sysObserver.getUniqueID(prefHelper_.getExternDebug());
        if (!uniqId.equals(SystemObserver.BLANK)) {
            contentObject.put(Defines.Jsonkey.HardwareID.getKey(), uniqId);
        }

        extrasObj.put(Defines.Jsonkey.ContentTitle.getKey(), builder.getContentTitle());
        extrasObj.put(Defines.Jsonkey.ContentDesc.getKey(), builder.getContentDesc());
        extrasObj.put(Defines.Jsonkey.ContentImgUrl.getKey(), builder.getContentImgUrl());
        if (builder.getContentId() != null && builder.getContentId().length() > 0) {
            extrasObj.put(Defines.Jsonkey.ContentID.getKey(), builder.getContentId());
        }

        contentObject.put(Defines.Jsonkey.Params.getKey(), extrasObj);

        if (builder.getAdditionalParams() != null) {
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
