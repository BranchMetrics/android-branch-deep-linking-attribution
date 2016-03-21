package io.branch.referral;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.util.BranchViewHandler;

/**
 * <p>
 * Abstract for Session init request. All request which do initilaise session should extend from this.
 * </p>
 */
abstract class ServerRequestInitSession extends ServerRequest {
    protected static final String ACTION_OPEN = "open";
    protected static final String ACTION_INSTALL = "install";

    public ServerRequestInitSession(Context context, String requestPath) {
        super(context, requestPath);
    }

    protected ServerRequestInitSession(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
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

    public static boolean isInitSessionAction(String actionName) {
        boolean isInitSessionAction = false;
        if (actionName != null) {
            isInitSessionAction = (actionName.equalsIgnoreCase(ACTION_OPEN) || actionName.equalsIgnoreCase(ACTION_INSTALL));
        }
        return isInitSessionAction;
    }

    public boolean handleBranchViewIfAvailable(ServerResponse resp) {
        //////TODO  Remove before merging to production. Test code for simulating Branch views on Open/Install events
        try {
            JSONObject debugObj = new JSONObject();
            String webViewHtml = "<!DOCTYPE html>" +
                    "<html>" +
                    "<body>" +
                    "<h1>Branch View Test</h1>" +
                    "<p>Branch view Test.</p>" +
                    "\n\n\n<button onclick=\\\"window.location.href='branch-cta://accept'\\\">Accept </button>\n\n\n" +
                    "\t\t<button onclick=\\\"window.location.href='branch-cta://cancel'\\\">Cancel </button>\n" +
                    "</body>" +
                    "</html>";

            resp.getObject().put(Defines.Jsonkey.BranchViewData.getKey(), new JSONObject("{ \"id\":\"id_011\",\"num_of_use\":-1,\"html\": \"" + webViewHtml + "\" }"));
        } catch (JSONException ignore) {

        }
        ///////////////////////

        boolean isBranchViewShowing = false;
        if (resp.getObject() != null && resp.getObject().has(Defines.Jsonkey.BranchViewData.getKey())) {
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
                        BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                    }
                } else {
                    BranchViewHandler.getInstance().markInstallOrOpenBranchViewPending(branchViewJsonObj, actionName);
                }
            } catch (JSONException ignore) {
            }
        }
        return isBranchViewShowing;
    }
}
