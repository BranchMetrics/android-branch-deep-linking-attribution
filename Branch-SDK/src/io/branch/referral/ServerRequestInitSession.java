package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

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
    
    ServerRequestInitSession(Context context, String requestPath) {
        super(context, requestPath);
        context_ = context;
        contentDiscoveryManifest_ = ContentDiscoveryManifest.getInstance(context_);
    }
    
    ServerRequestInitSession(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
        context_ = context;
        contentDiscoveryManifest_ = ContentDiscoveryManifest.getInstance(context_);
    }
    
    @Override
    protected void setPost(JSONObject post) {
        super.setPost(post);
        updateEnvironment(context_, post);
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
            prefHelper_.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppLink(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setPushIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIsAppLinkTriggeredInit(false);
            prefHelper_.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIsFullAppConversion(false);

            if (!TextUtils.isEmpty(prefHelper_.getPushToken()) && ServerRequestQueue.getInstance(context_).containsPushActionCompleted()) {
                Branch.getInstance().handleNewRequest(new ServerRequestPushActionCompleted(context_, prefHelper_.getPushToken(), null));
            }

            // Provide data to Fabric answers
            if (response.getObject() != null && response.getObject().has(Defines.Jsonkey.Data.getKey())) {
                String eventName = (this instanceof ServerRequestRegisterInstall) ? ExtendedAnswerProvider.KIT_EVENT_INSTALL : ExtendedAnswerProvider.KIT_EVENT_OPEN;
                JSONObject linkDataJsonObj = new JSONObject(response.getObject().getString(Defines.Jsonkey.Data.getKey()));
                new ExtendedAnswerProvider().provideData(eventName, linkDataJsonObj, prefHelper_.getIdentityID());
            }
        } catch (JSONException ignore) {
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
    }
    
    /**
     * Update link referrer params like play store referrer params
     * For link clicked installs link click id is updated when install referrer broadcast is received
     * Also update any googleSearchReferrer available with play store referrer broadcast
     *
     * @see InstallListener
     * @see Branch#enablePlayStoreReferrer(long)
     */
    void updateLinkReferrerParams() {
        if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            } catch (JSONException ignore) {
            }
        }
        if (!prefHelper_.getGoogleSearchInstallIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getPost().put(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey(), prefHelper_.getGoogleSearchInstallIdentifier());
            } catch (JSONException ignore) {
            }
        }
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
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            }
            if (!prefHelper_.getGoogleSearchInstallIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey(), prefHelper_.getGoogleSearchInstallIdentifier());
            }
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
}
