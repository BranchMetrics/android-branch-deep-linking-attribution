package io.branch.referral;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract class defining the structure of a Branch Server request.
 */
abstract class ServerRequest {

    private static final String POST_KEY = "REQ_POST";
    private static final String POST_PATH_KEY = "REQ_POST_PATH";

    private JSONObject post_;
    protected String requestPath_;
    protected PrefHelper prefHelper_;

    /*True if there is an error in creating this request such as error with json parameters.*/
    public boolean constructError_ = false;

    /**
     * <p>Creates an instance of ServerRequest.</p>
     *
     * @param context     Application context.
     * @param requestPath Path to server for this request.
     */
    public ServerRequest(Context context, String requestPath) {
        requestPath_ = requestPath;
        prefHelper_ = PrefHelper.getInstance(context);
    }

    /**
     * <p>Creates an instance of ServerRequest.</p>
     *
     * @param requestPath Path to server for this request.
     * @param post        A {@link JSONObject} containing the post data supplied with the current request
     *                    as key-value pairs.
     * @param context     Application context.
     */
    protected ServerRequest(String requestPath, JSONObject post, Context context) {
        requestPath_ = requestPath;
        post_ = post;
        prefHelper_ = PrefHelper.getInstance(context);

    }

    /**
     * <p>Should be implemented by the child class.Specifies any error associated with request.
     * If there are errors request will not be executed.</p>
     *
     * @return A {@link Boolean} which is set to true if there are errors with this request.
     *         Child class is responsible for implementing its own logic for error check and reporting.
     *
     * @param context   Application context.
     */
    public abstract boolean handleErrors(Context context);

    /**
     * <p>Called when execution of this request to server succeeds. Child class should implement
     *  its own logic for handling the post request execution.</p>
     *
     * @param response A {@link ServerResponse} object containing server response for this request.
     *
     * @param branch Current {@link Branch} instance
     */
    public abstract void onRequestSucceeded(ServerResponse response ,Branch branch);

    /**
     * <p>Called when there is an error on executing this request. Child class should handle the failure
     * accordingly.</p>
     *
     * @param statusCode A {@link Integer} value specifying http return code or any branch specific error defined in {@link BranchError}.
     *
     */
    public abstract void handleFailure(int statusCode);

    /**
     * Specify whether the request is a GET or POST. Child class has to implement accordingly.
     * @return A {@link Boolean} value specifying if this request is a GET or not.
     */
    public abstract boolean isGetRequest();

    /**
     * Clears the callbacks associated to this request.
     */
    public abstract void clearCallbacks();

    /**
     * Specifies whether to retry this request on failure. By default request is not retried on fail.
     * Those request which need to retry on failure should override and handle accordingly
     *
     * @return A {@link Boolean} whose values is true if request needed to retry on failure.
     */
    public boolean shouldRetryOnFail(){
        return false;
    }

    /**
     * Returns true if this request is causing a session initialisation. Only open or Install request causes an init session.
     * Request which initialise new session should override and handle accordingly.
     *
     * @return A {@link Boolean} whose value is true if this request causes a session initialisation.
     */
    public boolean isSessionInitRequest() {
        return false;
    }

    /**
     * <p>Provides the path to server for this request.
     * see {@link Defines.RequestPath} <p>
     *
     * @return Path for this request.
     */
    public final String getRequestPath() {
        return requestPath_;
    }

    /**
     * <p>Provides the complete url for executing this request. URl consist of API base url and request
     * path. Child class need to extend this method if they need to add specific items to the url </p>
     *
     * @return A url for executing this request against the server.
     */
    public String getRequestUrl(){
        return prefHelper_.getAPIBaseUrl() + requestPath_;
    }

    /**
     * <p>Sets a {@link JSONObject} containing the post data supplied with the current request.</p>
     *
     * @param post A {@link JSONObject} containing the post data supplied with the current request
     *             as key-value pairs.
     */
    protected void setPost(JSONObject post){
        post_ = post;
    }

    /**
     * <p>Gets a {@link JSONObject} containing the post data supplied with the current request as
     * key-value pairs.</p>
     *
     * @return A {@link JSONObject} containing the post data supplied with the current request
     *         as key-value pairs.
     */
    public JSONObject getPost() {
        return post_;
    }

    /**
     * <p>Gets a {@link JSONObject} containing the values of {@link ServerRequest#TAG_KEY} and
     * {@link ServerRequest#POST_KEY} as currently configured.</p>
     *
     * @return A {@link JSONObject} containing the values of {@link ServerRequest#TAG_KEY} and
     *         {@link ServerRequest#POST_KEY} as currently configured, or <i>null</i> if
     *         one or both of those values have not yet been set.
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(POST_KEY, post_);
            json.put(POST_PATH_KEY,requestPath_);
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    /**
     * <p>Converts a {@link JSONObject} object containing keys stored as key-value pairs into
     * a {@link ServerRequest}.</p>
     *
     * @param json    A {@link JSONObject} object containing post data stored as key-value pairs
     * @param context Application context.
     * @return A {@link ServerRequest} object with the {@link #POST_KEY} and {@link #TAG_KEY}
     * values set if not null; this can be one or the other. If both values in the
     * supplied {@link JSONObject} are null, null is returned instead of an object.
     */
    public static ServerRequest fromJSON(JSONObject json, Context context) {
        JSONObject post = null;
        String requestPath = "";
        try {
            if (json.has(POST_KEY)) {
                post = json.getJSONObject(POST_KEY);
            }
        } catch (JSONException e) {
            // it's OK for post to be null
        }

        try {
            if (json.has(POST_PATH_KEY)) {
                requestPath = json.getString(POST_PATH_KEY);
            }
        } catch (JSONException e) {
            // it's OK for post to be null
        }

        if (requestPath != null && requestPath.length() > 0) {
            return getExtendedServerRequest(requestPath, post, context);
        }
        return null;
    }

    /**
     * <p>Factory method for creating the specific server requests objects. Creates requests according
     * to the request path.</p>
     *
     * @param requestPath Path for the server request. see {@link Defines.RequestPath}
     * @param post        A {@link JSONObject} object containing post data stored as key-value pairs.
     * @param context     Application context.
     * @return A {@link ServerRequest} object for the given Post data.
     */
    private static ServerRequest getExtendedServerRequest(String requestPath, JSONObject post, Context context) {
        ServerRequest extendedReq = null;

        if (requestPath.equalsIgnoreCase(Defines.RequestPath.CompletedAction.getPath())) {
            extendedReq = new ServerRequestActionCompleted(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.ApplyReferralCode.getPath())) {
            extendedReq = new ServerRequestApplyReferralCode(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.GetURL.getPath())) {
            extendedReq = new ServerRequestCreateUrl(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.GetReferralCode.getPath())) {
            extendedReq = new ServerRequestGetReferralCode(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.Referrals.getPath())) {
            extendedReq = new ServerRequestGetReferralCount(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.GetCreditHistory.getPath())) {
            extendedReq = new ServerRequestGetRewardHistory(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.GetCredits.getPath())) {
            extendedReq = new ServerRequestGetRewards(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.IdentifyUser.getPath())) {
            extendedReq = new ServerRequestIdentifyUserRequest(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.Logout.getPath())) {
            extendedReq = new ServerRequestLogout(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.RedeemRewards.getPath())) {
            extendedReq = new ServerRequestRedeemRewards(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.RegisterClose.getPath())) {
            extendedReq = new ServerRequestRegisterClose(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.RegisterInstall.getPath())) {
            extendedReq = new ServerRequestRegisterInstall(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.RegisterOpen.getPath())) {
            extendedReq = new ServerRequestRegisterOpen(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.SendAPPList.getPath())) {
            extendedReq = new ServerRequestSendAppList(requestPath, post, context);
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.ValidateReferralCode.getPath())) {
            extendedReq = new ServerRequestValidateReferralCode(requestPath, post, context);
        }

        return extendedReq;
    }

    /**
     * Updates the google ads parameters. This should be called only from a background thread since it involves GADS method invocation using reflection
     *
     * @param sysObserver {@link SystemObserver} instance.
     */
    public void updateGAdsParams(SystemObserver sysObserver) {
        try {
            String advertisingId = sysObserver.getAdvertisingId();
            if (advertisingId != null && getPost() != null) {
                getPost().put(Defines.Jsonkey.GoogleAdvertisingID.getKey(), advertisingId);
            }
            int latVal = sysObserver.getLATValue();
            if (getPost() != null) {
                getPost().put(Defines.Jsonkey.LATVal.getKey(), latVal);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /*
     * Checks if this Application has internet permissions.
     *
     * @param context Application context.
     *
     * @return True if application has internet permission.
     */
    protected boolean doesAppHasInternetPermission(Context context){
        int result = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
