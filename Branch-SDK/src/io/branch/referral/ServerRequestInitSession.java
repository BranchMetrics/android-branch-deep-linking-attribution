package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.indexing.ContentDiscoverer;
import io.branch.indexing.ContentDiscoveryManifest;

/**
 * <p>
 * Abstract for Session init request. All request which do initilaise session should extend from this.
 * </p>
 */
abstract class ServerRequestInitSession extends ServerRequest {
    static final String ACTION_OPEN = "open";
    static final String ACTION_INSTALL = "install";
    private final Context context_;
    private final ContentDiscoveryManifest contentDiscoveryManifest_;
    final SystemObserver systemObserver_;
    
    private static final int STATE_FRESH_INSTALL = 0;
    private static final int STATE_UPDATE = 2;
    private static final int STATE_NO_CHANGE = 1;
    private PackageInfo packageInfo;
    
    ServerRequestInitSession(Context context, String requestPath, SystemObserver systemObserver) {
        super(context, requestPath);
        context_ = context;
        systemObserver_ = systemObserver;
        contentDiscoveryManifest_ = ContentDiscoveryManifest.getInstance(context_);
    }
    
    ServerRequestInitSession(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
        context_ = context;
        systemObserver_ = new SystemObserver(context);
        contentDiscoveryManifest_ = ContentDiscoveryManifest.getInstance(context_);
    }
    
    @Override
    protected void setPost(JSONObject post) throws JSONException {
        super.setPost(post);
        if (!systemObserver_.getAppVersion().equals(SystemObserver.BLANK)) {
            post.put(Defines.Jsonkey.AppVersion.getKey(), systemObserver_.getAppVersion());
        }
        post.put(Defines.Jsonkey.FaceBookAppLinkChecked.getKey(), prefHelper_.getIsAppLinkTriggeredInit());
        post.put(Defines.Jsonkey.IsReferrable.getKey(), prefHelper_.getIsReferrable());
        post.put(Defines.Jsonkey.Debug.getKey(), prefHelper_.getExternDebug());
        
        updateInstallStateAndTimestamps(post);
        updateEnvironment(context_, post);
    }
    
    void updateURIScheme() throws JSONException {
        if (getPost() != null) {
            String uriScheme = systemObserver_.getURIScheme();
            if (!uriScheme.equals(SystemObserver.BLANK)) {
                getPost().put(Defines.Jsonkey.URIScheme.getKey(), uriScheme);
            }
        }
    }
    
    /**
     * Check if there is a valid callback to return init session result
     *
     * @return True if a valid call back is present.
     */
    public abstract boolean hasCallBack();
    
    @Override
    public boolean isGAdsParamsRequired() {
        return true; //Session start requests need GAds params
    }
    
    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }
    
    public abstract String getRequestActionName();
    
    static boolean isInitSessionAction(String actionName) {
        boolean isInitSessionAction = false;
        if (actionName != null) {
            isInitSessionAction = (actionName.equalsIgnoreCase(ACTION_OPEN) || actionName.equalsIgnoreCase(ACTION_INSTALL));
        }
        return isInitSessionAction;
    }
    
    boolean handleBranchViewIfAvailable(ServerResponse resp) {
        boolean isBranchViewShowing = false;
        if (resp != null && resp.getObject() != null && resp.getObject().has(Defines.Jsonkey.BranchViewData.getKey())) {
            try {
                JSONObject branchViewJsonObj = resp.getObject().getJSONObject(Defines.Jsonkey.BranchViewData.getKey());
                String actionName = getRequestActionName();
                if ((Branch.getInstance().currentActivityReference_ != null && Branch.getInstance().currentActivityReference_.get() != null)) {
                    Activity currentActivity = Branch.getInstance().currentActivityReference_.get();
                    boolean isActivityEnabledForBranchView = true;
                    if (currentActivity instanceof Branch.IBranchViewControl) {
                        isActivityEnabledForBranchView = !((Branch.IBranchViewControl) currentActivity).skipBranchViewsOnThisActivity();
                    }
                    if (isActivityEnabledForBranchView) {
                        isBranchViewShowing = BranchViewHandler.getInstance().showBranchView(branchViewJsonObj, actionName, currentActivity, Branch.getInstance());
                    } else {
                        isBranchViewShowing = BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                    }
                } else {
                    isBranchViewShowing = BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                }
            } catch (JSONException ignore) {
            }
        }
        return isBranchViewShowing;
    }
    
    @Override
    
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        // Check for any Third party SDK for data handling
        try {
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setGoogleSearchInstallIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setGooglePlayReferrer(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppLink(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setPushIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIsAppLinkTriggeredInit(false);
            prefHelper_.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIsFullAppConversion(false);
            // Provide data to Fabric answers
            if (response.getObject() != null && response.getObject().has(Defines.Jsonkey.Data.getKey())) {
                JSONObject linkDataJsonObj = new JSONObject(response.getObject().getString(Defines.Jsonkey.Data.getKey()));
                if (linkDataJsonObj.optBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                    String eventName = (this instanceof ServerRequestRegisterInstall) ? ExtendedAnswerProvider.KIT_EVENT_INSTALL : ExtendedAnswerProvider.KIT_EVENT_OPEN;
                    new ExtendedAnswerProvider().provideData(eventName, linkDataJsonObj, prefHelper_.getIdentityID());
                }
            }
        } catch (JSONException ignore) {
        }
        
        if (prefHelper_.getLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME) == 0) {
            prefHelper_.setLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME, prefHelper_.getLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME));
        }
    }
    
    void onInitSessionCompleted(ServerResponse response, Branch branch) {
        if (contentDiscoveryManifest_ != null) {
            contentDiscoveryManifest_.onBranchInitialised(response.getObject());
            if (branch.currentActivityReference_ != null) {
                try {
                    ContentDiscoverer.getInstance().onSessionStarted(branch.currentActivityReference_.get(), branch.sessionReferredLink_);
                } catch (Exception ignore) {
                }
            }
        }
        branch.updateSkipURLFormats();
    }
    
    /**
     * Update link referrer params like play store referrer params
     * For link clicked installs link click id is updated when install referrer broadcast is received
     * Also update any googleSearchReferrer available with play store referrer broadcast
     *
     * @see InstallListener
     * @see Branch#setPlayStoreReferrerCheckTimeout(long)
     */
    void updateLinkReferrerParams() {
        // Add link identifier if present
        String linkIdentifier = prefHelper_.getLinkClickIdentifier();
        if (!linkIdentifier.equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.LinkIdentifier.getKey(), linkIdentifier);
            } catch (JSONException ignore) {
            }
        }
        // Add Google search install referrer if present
        String googleSearchInstallIdentifier = prefHelper_.getGoogleSearchInstallIdentifier();
        if (!googleSearchInstallIdentifier.equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey(), googleSearchInstallIdentifier);
            } catch (JSONException ignore) {
            }
        }
        // Add Google play raw referrer if present
        String googlePlayReferrer = prefHelper_.getGooglePlayReferrer();
        if (!googlePlayReferrer.equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.GooglePlayInstallReferrer.getKey(), googlePlayReferrer);
            } catch (JSONException ignore) {
            }
        }
        // Check for Conversion from instant app to full app
        if (prefHelper_.isFullAppConversion()) {
            try {
                getPost().put(Defines.Jsonkey.AndroidAppLinkURL.getKey(), prefHelper_.getAppLink());
                getPost().put(Defines.Jsonkey.IsFullAppConv.getKey(), true);
            } catch (JSONException ignore) {
            }
        }
    }
    
    @Override
    public void onPreExecute() {
        JSONObject post = getPost();
        try {
            if (!prefHelper_.getAppLink().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.AndroidAppLinkURL.getKey(), prefHelper_.getAppLink());
            }
            if (!prefHelper_.getPushIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.AndroidPushIdentifier.getKey(), prefHelper_.getPushIdentifier());
            }
            // External URI or Extras if exist
            if (!prefHelper_.getExternalIntentUri().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.External_Intent_URI.getKey(), prefHelper_.getExternalIntentUri());
            }
            if (!prefHelper_.getExternalIntentExtra().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.External_Intent_Extra.getKey(), prefHelper_.getExternalIntentExtra());
            }
            
            if (contentDiscoveryManifest_ != null) {
                JSONObject cdObj = new JSONObject();
                cdObj.put(ContentDiscoveryManifest.MANIFEST_VERSION_KEY, contentDiscoveryManifest_.getManifestVersion());
                cdObj.put(ContentDiscoveryManifest.PACKAGE_NAME_KEY, context_.getPackageName());
                post.put(ContentDiscoveryManifest.CONTENT_DISCOVER_KEY, cdObj);
            }
        } catch (JSONException ignore) {
            
        }
    }
    
    /*
     * Method to update the install or update state along with the timestamps.
     * PRS NOTE
     * The Original install time will have a the very first install time only if the app allows preference back up.
     * Apps that need to distinguish between a fresh install and re-install need to allow backing up of preferences.
     * Previous install time stamp always carry the last last known update time. the value will be zero for any fresh install and will be the last update time with successive opens.
     * Previous install time will have a value less than last update time ever since the app is updated.
     *
     *  ------------------------------------------------------------
     * |   update_state_install    | lut <= fit, fit = oit, put = 0 |
     *  --------------------------- --------------------------------
     * |   update_state_reinstall  | oit < fit, put = 0             |
     *  --------------------------- --------------------------------
     * |   update_state_update     | lut > fit, put < lut           |
     *  --------------------------- --------------------------------
     * |   update_state_no_update  | lut == put                     |
     *  --------------------------- --------------------------------
     * @param post Post body for init request which need to be updated
     * @throws JSONException when there is any exception on adding time stamps or update state
     */
    private void updateInstallStateAndTimestamps(JSONObject post) throws JSONException {
        int installOrUpdateState = STATE_NO_CHANGE;
        String currAppVersion = systemObserver_.getAppVersion();
        long updateBufferTime = 1 * (24 * 60 * 60 * 1000); // Update buffer time is a day.
        PackageInfo packageInfo = null;
        try {
            packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (PrefHelper.NO_STRING_VALUE.equals(prefHelper_.getAppVersion())) {
            // Default, just register an install
            installOrUpdateState = STATE_FRESH_INSTALL;
            // if no app version is in storage, this must be the first time Branch is here. 24 hour buffer for updating as an update state
            if (packageInfo != null && (packageInfo.lastUpdateTime - packageInfo.firstInstallTime) >= updateBufferTime) {
                installOrUpdateState = STATE_UPDATE;
            }
        } else if (!prefHelper_.getAppVersion().equals(currAppVersion)) {
            // if the current app version doesn't match the stored, it's an update
            installOrUpdateState = STATE_UPDATE;
        }
        
        post.put(Defines.Jsonkey.Update.getKey(), installOrUpdateState);
        if (packageInfo != null) {
            post.put(Defines.Jsonkey.FirstInstallTime.getKey(), packageInfo.firstInstallTime);
            post.put(Defines.Jsonkey.LastUpdateTime.getKey(), packageInfo.lastUpdateTime);
            long originalInstallTime = prefHelper_.getLong(PrefHelper.KEY_ORIGINAL_INSTALL_TIME);
            if (originalInstallTime == 0) {
                originalInstallTime = packageInfo.firstInstallTime;
                prefHelper_.setLong(PrefHelper.KEY_ORIGINAL_INSTALL_TIME, packageInfo.firstInstallTime);
            }
            post.put(Defines.Jsonkey.OriginalInstallTime.getKey(), originalInstallTime);
            
            long lastKnownUpdateTime = prefHelper_.getLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME);
            if (lastKnownUpdateTime < packageInfo.lastUpdateTime) {
                prefHelper_.setLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME, lastKnownUpdateTime);
                prefHelper_.setLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME, packageInfo.lastUpdateTime);
            }
            post.put(Defines.Jsonkey.PreviousUpdateTime.getKey(), prefHelper_.getLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME));
        }
    }
}
