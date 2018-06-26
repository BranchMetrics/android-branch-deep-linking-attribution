package io.branch.referral;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Abstract class defining the structure of a Branch Server request.
 */
public abstract class ServerRequest {
    
    private static final String POST_KEY = "REQ_POST";
    private static final String POST_PATH_KEY = "REQ_POST_PATH";
    
    private JSONObject params_;
    protected String requestPath_;
    protected final PrefHelper prefHelper_;
    private final SystemObserver systemObserver_;
    long queueWaitTime_ = 0;
    private boolean disableAndroidIDFetch_;
    private int waitLockCnt = 0;
    private final Context context_;
    
    // Various process wait locks for Branch server request
    enum PROCESS_WAIT_LOCK {
        FB_APP_LINK_WAIT_LOCK, GAID_FETCH_WAIT_LOCK, INTENT_PENDING_WAIT_LOCK, STRONG_MATCH_PENDING_WAIT_LOCK,
        INSTALL_REFERRER_FETCH_WAIT_LOCK
    }
    
    // Set for holding any active wait locks
    final Set<PROCESS_WAIT_LOCK> locks_;
    
    /*True if there is an error in creating this request such as error with json parameters.*/
    public boolean constructError_ = false;
    
    public enum BRANCH_API_VERSION {
        V1,
        V2
    }
    
    /**
     * <p>Creates an instance of ServerRequest.</p>
     *
     * @param context     Application context.
     * @param requestPath Path to server for this request.
     */
    public ServerRequest(Context context, String requestPath) {
        context_ = context;
        requestPath_ = requestPath;
        prefHelper_ = PrefHelper.getInstance(context);
        systemObserver_ = new SystemObserver(context);
        params_ = new JSONObject();
        disableAndroidIDFetch_ = Branch.isDeviceIDFetchDisabled();
        locks_ = new HashSet<>();
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
        context_ = context;
        requestPath_ = requestPath;
        params_ = post;
        prefHelper_ = PrefHelper.getInstance(context);
        systemObserver_ = new SystemObserver(context);
        disableAndroidIDFetch_ = Branch.isDeviceIDFetchDisabled();
        locks_ = new HashSet<>();
    }
    
    /**
     * <p>Should be implemented by the child class.Specifies any error associated with request.
     * If there are errors request will not be executed.</p>
     *
     * @param context Application context.
     * @return A {@link Boolean} which is set to true if there are errors with this request.
     * Child class is responsible for implementing its own logic for error check and reporting.
     */
    public abstract boolean handleErrors(Context context);
    
    /**
     * <p>Called when execution of this request to server succeeds. Child class should implement
     * its own logic for handling the post request execution.</p>
     *
     * @param response A {@link ServerResponse} object containing server response for this request.
     * @param branch   Current {@link Branch} instance
     */
    public abstract void onRequestSucceeded(ServerResponse response, Branch branch);
    
    /**
     * <p>Called when there is an error on executing this request. Child class should handle the failure
     * accordingly.</p>
     *
     * @param statusCode A {@link Integer} value specifying http return code or any branch specific error defined in {@link BranchError}.
     * @param causeMsg   A {@link String} value specifying cause for the error if any.
     */
    public abstract void handleFailure(int statusCode, String causeMsg);
    
    /**
     * Specify whether the request is a GET or POST. Child class has to implement accordingly.
     *
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
    public boolean shouldRetryOnFail() {
        return false;
    }
    
    /**
     * Specifies whether this request should be persisted to memory in order to re send in the next session
     *
     * @return {@code true} by default. Should be override for request that need not to be persisted
     */
    boolean isPersistable() {
        return true;
    }
    
    /**
     * Specifies whether this request should add the limit app tracking value
     *
     * @return {@code true} to add the limit app tracking value to the request else false.
     * {@code false} by default. Should override for requests that need limited app tracking value.
     */
    protected boolean shouldUpdateLimitFacebookTracking() {
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
    public String getRequestUrl() {
        return prefHelper_.getAPIBaseUrl() + requestPath_;
    }
    
    /**
     * <p>Sets a {@link JSONObject} containing the post data supplied with the current request.</p>
     *
     * @param post A {@link JSONObject} containing the post data supplied with the current request
     *             as key-value pairs.
     */
    protected void setPost(JSONObject post) throws JSONException {
        params_ = post;
        if (getBranchRemoteAPIVersion() == BRANCH_API_VERSION.V2) {
            try {
                JSONObject userDataObj = new JSONObject();
                params_.put(Defines.Jsonkey.UserData.getKey(), userDataObj);
                DeviceInfo.getInstance(prefHelper_.getExternDebug(), systemObserver_, disableAndroidIDFetch_).updateRequestWithUserData(context_, prefHelper_, userDataObj);
            } catch (JSONException ignore) {
            }
        } else {
            DeviceInfo.getInstance(prefHelper_.getExternDebug(), systemObserver_, disableAndroidIDFetch_).updateRequestWithDeviceParams(params_);
        }
    }
    
    /**
     * <p>Gets a {@link JSONObject} containing the post data supplied with the current request as
     * key-value pairs.</p>
     *
     * @return A {@link JSONObject} containing the post data supplied with the current request
     * as key-value pairs.
     */
    public JSONObject getPost() {
        return params_;
    }
    
    /**
     * <p>
     * Specifies whether this request need to be updated with Google Ads Id and LAT value
     * By default update GAds params update is turned off. Override this on request which need to have GAds params
     * </p>
     *
     * @return A {@link Boolean} with value true if this reuest need GAds params
     */
    public boolean isGAdsParamsRequired() {
        return false;
    }
    
    /**
     * <p>Gets a {@link JSONObject} containing the post data supplied with the current request as
     * key-value pairs appended with the instrumentation data.</p>
     * <p>
     * * @param instrumentationData {@link ConcurrentHashMap} with instrumentation values
     *
     * @return A {@link JSONObject} containing the post data supplied with the current request
     * as key-value pairs and the instrumentation meta data.
     */
    public JSONObject getPostWithInstrumentationValues(ConcurrentHashMap<String, String> instrumentationData) {
        JSONObject extendedPost = new JSONObject();
        try {
            //Add original parameters
            if (params_ != null) {
                JSONObject originalParams = new JSONObject(params_.toString());
                Iterator<String> keys = originalParams.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    extendedPost.put(key, originalParams.get(key));
                }
            }
            // Append instrumentation metadata
            if (instrumentationData.size() > 0) {
                JSONObject instrObj = new JSONObject();
                Set<String> keys = instrumentationData.keySet();
                try {
                    for (String key : keys) {
                        instrObj.put(key, instrumentationData.get(key));
                        instrumentationData.remove(key);
                    }
                    extendedPost.put(Defines.Jsonkey.Branch_Instrumentation.getKey(), instrObj);
                } catch (JSONException ignore) {
                }
            }
        } catch (JSONException ignore) {
        } catch (ConcurrentModificationException ex) {
            extendedPost = params_;
        }
        return extendedPost;
    }
    
    /**
     * Returns a JsonObject with the parameters that needed to be set with the get request.
     *
     * @return A {@link JSONObject} representation of get request parameters.
     */
    public JSONObject getGetParams() {
        return params_;
    }
    
    /**
     * Adds a param and its value to the get request
     *
     * @param paramKey   A {@link String} value for the get param key
     * @param paramValue A {@link String} value for the get param value
     */
    protected void addGetParam(String paramKey, String paramValue) {
        try {
            params_.put(paramKey, paramValue);
        } catch (JSONException ignore) {
        }
    }
    
    /**
     * <p>Gets a {@link JSONObject} corresponding to the {@link ServerRequest} and
     * {@link ServerRequest#POST_KEY} as currently configured.</p>
     *
     * @return A {@link JSONObject} corresponding to the values of {@link ServerRequest} and
     * {@link ServerRequest#POST_KEY} as currently configured, or <i>null</i> if
     * one or both of those values have not yet been set.
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(POST_KEY, params_);
            json.put(POST_PATH_KEY, requestPath_);
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
     * @return A {@link ServerRequest} object with the {@link #POST_KEY}
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
        } else if (requestPath.equalsIgnoreCase(Defines.RequestPath.GetURL.getPath())) {
            extendedReq = new ServerRequestCreateUrl(requestPath, post, context);
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
        }
        
        return extendedReq;
    }
    
    boolean skipOnTimeOut = false;
    
    /**
     * Updates the google ads parameters. This should be called only from a background thread since it involves GADS method invocation using reflection
     */
    private void updateGAdsParams() {
        BRANCH_API_VERSION version = getBranchRemoteAPIVersion();
        if (!TextUtils.isEmpty(systemObserver_.GAIDString_)) {
            try {
                if (version == BRANCH_API_VERSION.V2) {
                    JSONObject userDataObj = params_.optJSONObject(Defines.Jsonkey.UserData.getKey());
                    if (userDataObj != null) {
                        userDataObj.put(Defines.Jsonkey.AAID.getKey(), systemObserver_.GAIDString_);
                        userDataObj.put(Defines.Jsonkey.LimitedAdTracking.getKey(), systemObserver_.LATVal_);
                        userDataObj.remove(Defines.Jsonkey.UnidentifiedDevice.getKey());
                    }
                } else {
                    params_.put(Defines.Jsonkey.GoogleAdvertisingID.getKey(), systemObserver_.GAIDString_);
                    params_.put(Defines.Jsonkey.LATVal.getKey(), systemObserver_.LATVal_);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else { // Add unidentified_device when neither aaid nor AndoridID present
            if (version == BRANCH_API_VERSION.V2) {
                try {
                    if (version == BRANCH_API_VERSION.V2) {
                        JSONObject userDataObj = params_.optJSONObject(Defines.Jsonkey.UserData.getKey());
                        if (userDataObj != null) {
                            if (!userDataObj.has(Defines.Jsonkey.AndroidID.getKey())) {
                                userDataObj.put(Defines.Jsonkey.UnidentifiedDevice.getKey(), true);
                            }
                        }
                    }
                } catch (JSONException ignore) {
                
                }
            }
        }
    }
    
    private void updateDeviceInfo() {
        BRANCH_API_VERSION version = getBranchRemoteAPIVersion();
        if (version == BRANCH_API_VERSION.V2) {
            JSONObject userDataObj = params_.optJSONObject(Defines.Jsonkey.UserData.getKey());
            if (userDataObj != null) {
                try {
                    userDataObj.put(Defines.Jsonkey.DeveloperIdentity.getKey(), prefHelper_.getIdentity());
                    userDataObj.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                } catch (JSONException ignore) {
                }
            }
        }
    }
    
    
    /**
     * Update the additional metadata provided using {@link Branch#setRequestMetadata(String, String)} to the requests.
     */
    private void updateRequestMetadata() {
        // Take event level metadata, merge with top level metadata
        // event level metadata takes precedence
        try {
            JSONObject metadata = new JSONObject();
            Iterator<String> i = prefHelper_.getRequestMetadata().keys();
            while (i.hasNext()) {
                String k = i.next();
                metadata.put(k, prefHelper_.getRequestMetadata().get(k));
            }
            JSONObject originalMetadata = params_.optJSONObject(Defines.Jsonkey.Metadata.getKey());
            if (originalMetadata != null) {
                Iterator<String> postIter = originalMetadata.keys();
                while (postIter.hasNext()) {
                    String key = postIter.next();
                    // override keys from above
                    metadata.put(key, originalMetadata.get(key));
                }
            }
            // Install metadata need to be send only with Install request
            if ((this instanceof ServerRequestRegisterInstall) && prefHelper_.getInstallMetadata().length() > 0) {
                params_.putOpt(Defines.Jsonkey.InstallMetadata.getKey(), prefHelper_.getInstallMetadata());
            }
            params_.put(Defines.Jsonkey.Metadata.getKey(), metadata);
        } catch (JSONException e) {
            Log.e("BranchSDK", "Could not merge metadata, ignoring user metadata.");
        }
    }
    
    /*
     * Update the the limit app tracking value to the request
     */
    private void updateLimitFacebookTracking() {
        JSONObject updateJson = getBranchRemoteAPIVersion() == BRANCH_API_VERSION.V1 ? params_ : params_.optJSONObject(Defines.Jsonkey.UserData.getKey());
        if (updateJson != null) {
            boolean isLimitFacebookTracking = prefHelper_.isAppTrackingLimited(); // Currently only FB app tracking
            if (isLimitFacebookTracking) {
                try {
                    updateJson.putOpt(Defines.Jsonkey.limitFacebookTracking.getKey(), isLimitFacebookTracking);
                } catch (JSONException ignore) {
                }
            }
        }
    }
    
    void doFinalUpdateOnMainThread() {
        updateRequestMetadata();
        if (shouldUpdateLimitFacebookTracking()) {
            updateLimitFacebookTracking();
        }
    }
    
    void doFinalUpdateOnBackgroundThread() {
        if (this instanceof ServerRequestInitSession) {
            ((ServerRequestInitSession) this).updateLinkReferrerParams();
        }
        
        // Update the dynamic device info params
        updateDeviceInfo();
        //Google ADs ID  and LAT value are updated using reflection. These method need background thread
        //So updating them for install and open on background thread.
        if (isGAdsParamsRequired() && !BranchUtil.isTestModeEnabled(context_)) {
            updateGAdsParams();
        }
    }
    
    /*
     * Checks if this Application has internet permissions.
     *
     * @param context Application context.
     *
     * @return True if application has internet permission.
     */
    
    protected boolean doesAppHasInternetPermission(Context context) {
        int result = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Called when request is added to teh queue
     */
    public void onRequestQueued() {
        queueWaitTime_ = System.currentTimeMillis();
    }
    
    /**
     * Returns the amount of time this request was in queque
     *
     * @return {@link Integer} with value of queued time in milli sec
     */
    public long getQueueWaitTime() {
        long waitTime = 0;
        if (queueWaitTime_ > 0) {
            waitTime = System.currentTimeMillis() - queueWaitTime_;
        }
        return waitTime;
    }
    
    /**
     * <p>
     * Set the specified process wait lock for this request. This request will not be blocked from
     * Execution until the waiting process finishes     *
     * </p>
     *
     * @param lock {@link PROCESS_WAIT_LOCK} type of lock
     */
    public void addProcessWaitLock(PROCESS_WAIT_LOCK lock) {
        if (lock != null) {
            locks_.add(lock);
        }
    }
    
    /**
     * Unlock the specified lock from the request. Call this when the locked process finishes
     *
     * @param lock {@link PROCESS_WAIT_LOCK} type of lock
     */
    public void removeProcessWaitLock(PROCESS_WAIT_LOCK lock) {
        locks_.remove(lock);
    }
    
    
    /**
     * Check if this request is waiting on any operation to finish before processing
     *
     * @return True if this request if any pre processing operation pending
     */
    public boolean isWaitingOnProcessToFinish() {
        return locks_.size() > 0;
    }
    
    /**
     * Called on UI thread just before executing a request. Do any final updates to the request here
     */
    public void onPreExecute() {
    
    }
    
    protected void updateEnvironment(Context context, JSONObject post) {
        try {
            String environment = isPackageInstalled(context) ? Defines.Jsonkey.NativeApp.getKey() : Defines.Jsonkey.InstantApp.getKey();
            if (getBranchRemoteAPIVersion() == BRANCH_API_VERSION.V2) {
                JSONObject userData = post.optJSONObject(Defines.Jsonkey.UserData.getKey());
                if (userData != null) {
                    userData.put(Defines.Jsonkey.Environment.getKey(), environment);
                }
            } else {
                post.put(Defines.Jsonkey.Environment.getKey(), environment);
            }
        } catch (Exception ignore) {
        }
    }
    
    private static boolean isPackageInstalled(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            return false;
        }
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list != null && list.size() > 0);
    }
    
    /**
     * Returns the Branch API version
     *
     * @return {@link BRANCH_API_VERSION} specifying remote Branch API version
     */
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V1;  // Default is v1
    }
    
    public void reportTrackingDisabledError() {
        PrefHelper.Debug("BranchSDK", "Requested operation cannot be completed since tracking is disabled [" + requestPath_ + "]");
        handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, "");
    }
    
    /**
     * Method to notify that this request is being executed when tracking is disabled.
     * Remove all PII data from the request added to the request
     *
     * @return {@code true} if the request needed to be executed in tracking disabled mode
     */
    protected boolean prepareExecuteWithoutTracking() {
        // Default return false. Return true for request need to be executed when tracking is disabled
        return false;
    }
    
    
}
