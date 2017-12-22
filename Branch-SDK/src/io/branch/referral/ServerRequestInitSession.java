package io.branch.referral;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

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
    }

    void onInitSessionCompleted(final ServerResponse response,final Branch branch) {
        if (contentDiscoveryManifest_ != null) {
            contentDiscoveryManifest_.onBranchInitialised(response.getObject());
            if (branch.currentActivityReference_ != null) try {
                ContentDiscoverer.getInstance().onSessionStarted(branch.currentActivityReference_.get(), branch.sessionReferredLink_);
                //Session Referring Link
                final JSONObject response_data = new JSONObject(response.getObject().getString("data"));
                if (response_data.has("validate") && response_data.getBoolean("validate")) {
                    //Launch the Deepview template
                    launchURLInChrome(branch.currentActivityReference_,response_data.getString("~referring_link"));
                }
                final Handler validate_handle = new Handler(Looper.getMainLooper()) {

                    @Override
                    public void handleMessage(Message inputMessage) {
                        try {
                            if (response_data.has("_branch_validate") && response_data.getInt("_branch_validate") == 60514) {
                                ValidateDeeplinkRouting(response_data, branch.currentActivityReference_);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            Message validatemessage = validate_handle.obtainMessage(1);
                            validatemessage.sendToTarget();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.run();

            } catch (Exception ignore) {
            }
        }
    }
    
    void ValidateDeeplinkRouting(final JSONObject validate_json,final WeakReference<Activity> currentActivityReference_) {
        Activity current_activity = currentActivityReference_.get();
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(current_activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(current_activity);
        }
        builder.setTitle("Branch Deeplinking Routing")
                .setMessage("Did the Deeplink route you to the correct content?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Succeeded
                        String launch_link = append_queryparams(validate_json,"g");
                        launchURLInChrome(currentActivityReference_,launch_link);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Failed
                        String launch_link = append_queryparams(validate_json,"r");
                        launchURLInChrome(currentActivityReference_,launch_link);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.sym_def_app_icon)
                .show();
    }

    String append_queryparams(JSONObject blob,String result) {
        String link = "";
        try{
            link = blob.getString("~referring_link");
            link = link.split("\\?")[0];
        } catch (Exception e) {
            Log.e("BRANCH SDK","Failed to get referring link");
        }
        link += "?validate=true";
        link += "&$uri_redirect_mode=2";
        try {
            link += blob.getString("ct").equals("t1")? "&t1="+result: "&t1="+blob.getString("t1");
            link += blob.getString("ct").equals("t2")? "&t2="+result: "&t2="+blob.getString("t2");
            link += blob.getString("ct").equals("t3")? "&t3="+result: "&t3="+blob.getString("t3");
            link += blob.getString("ct").equals("t4")? "&t4="+result: "&t4="+blob.getString("t4");
            link += blob.getString("ct").equals("t5")? "&t5="+result: "&t5="+blob.getString("t5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        link += "&os=android";
        return link;
    }

    void launchURLInChrome(WeakReference<Activity> activity,String url){
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.putExtra(Browser.EXTRA_APPLICATION_ID, activity.get().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setPackage("com.android.chrome");
        try {
            activity.get().startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Chrome is probably not installed
            // Try with the default browser
            i.setPackage(null);
            activity.get().startActivity(i);
        }
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
}
