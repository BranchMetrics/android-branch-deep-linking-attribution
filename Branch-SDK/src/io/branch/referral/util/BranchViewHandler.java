package io.branch.referral.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;

/**
 * <p>
 * Class for managing Branch Views on application. Keeps track of Branch views and their states
 * Displays the web view and handle the Branch View dialog life cycle.
 * </p>
 * Class for Managing
 */
public class BranchViewHandler {
    private static BranchViewHandler thisInstance_;
    private boolean isBranchViewDialogShowing_;
    private boolean isBranchViewAccepted_;
    private BranchView openOrInstallPendingBranchView_ = null;

    private static final String BRANCH_VIEW_REDIRECT_SCHEME = "branch-cta";
    private static final String BRANCH_VIEW_REDIRECT_ACTION_ACCEPT = "accept";
    private static final String BRANCH_VIEW_REDIRECT_ACTION_CANCEL = "cancel";

    public static final int BRANCH_VIEW_ERR_ALREADY_SHOWING = -200;
    public static final int BRANCH_VIEW_ERR_INVALID_VIEW = -201;


    private BranchViewHandler() {
    }

    /**
     * Get the singleton instance for BranchViewHandler
     *
     * @return {@link BranchViewHandler} instance
     */
    public static BranchViewHandler getInstance() {
        if (thisInstance_ == null) {
            thisInstance_ = new BranchViewHandler();
        }
        return thisInstance_;
    }

    public boolean showPendingBranchView(Activity currentActivity) {
        boolean isBranchViewShowed = showBranchView(openOrInstallPendingBranchView_, currentActivity, null);
        if (isBranchViewShowed) {
            openOrInstallPendingBranchView_ = null;
        }
        return isBranchViewShowed;
    }

    public boolean showBranchView(JSONObject branchViewObj, String actionName, Activity currentActivity, final IBranchViewEvents callback) {
        BranchView branchView = new BranchView(branchViewObj, actionName);
        return showBranchView(branchView, currentActivity, callback);
    }

    private boolean showBranchView(BranchView branchView, Activity currentActivity, final IBranchViewEvents callback) {
        if (isBranchViewDialogShowing_) {
            if (callback != null) {
                callback.onBranchViewError(BRANCH_VIEW_ERR_ALREADY_SHOWING, "Unable to create a Branch view. A Branch view is already showing", branchView.branchViewAction_);
            }
            return false;
        }

        isBranchViewDialogShowing_ = false;
        isBranchViewAccepted_ = false;


        if (currentActivity != null && branchView != null && branchView.isAvailable(currentActivity.getApplicationContext())) {
            createAndShowBranchView(branchView, currentActivity, callback);
        }

        return isBranchViewDialogShowing_;
    }

    private void createAndShowBranchView(final BranchView branchView, Activity currentActivity, final IBranchViewEvents callback) {
        if (currentActivity != null && branchView != null) {
            branchView.updateUsageCount(currentActivity.getApplicationContext(), branchView.branchViewID_);
            WebView webView = new WebView(currentActivity);

            final RelativeLayout layout = new RelativeLayout(currentActivity);
            layout.setVisibility(View.GONE);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(40, 80, 40, 80);

            layout.addView(webView, layoutParams);
            layout.setBackgroundColor(Color.parseColor("#11FEFEFE"));


            if (Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            if (!TextUtils.isEmpty(branchView.webViewUrl_)) {
                webView.loadUrl(branchView.webViewUrl_);
            } else if (!TextUtils.isEmpty(branchView.webViewHtml_)) {
                webView.loadDataWithBaseURL(null, branchView.webViewHtml_, "text/html", "utf-8", null);
            } else {
                return; // Error no url or Html
            }
            isBranchViewDialogShowing_ = true;
            final Dialog dialog = new Dialog(currentActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(layout);
            dialog.show();

            if (callback != null) {
                callback.onBranchViewVisible(branchView.branchViewAction_, branchView.branchViewID_);
            }

            webView.getSettings().setJavaScriptEnabled(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    boolean isHandled = handleUserActionRedirect(url);
                    if (!isHandled) {
                        view.loadUrl(url);
                    } else {
                        dialog.dismiss();
                    }
                    return isHandled;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    layout.setVisibility(View.VISIBLE);
                    view.setVisibility(View.VISIBLE);
                    dialog.show();
                    showViewWithAlphaAnimation(layout);
                    showViewWithAlphaAnimation(view);
                }
            });

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    isBranchViewDialogShowing_ = false;
                    if (callback != null) {
                        if (isBranchViewAccepted_) {
                            callback.onBranchViewAccepted(branchView.branchViewAction_, branchView.branchViewID_);
                        } else {
                            callback.onBranchViewCancelled(branchView.branchViewAction_, branchView.branchViewID_);
                        }
                    }
                }
            });

        }
    }

    private boolean handleUserActionRedirect(String url) {
        boolean isRedirectionHandled = false;
        try {
            URI uri = new URI(url);
            //Check if this is a App Branch view accept or reject redirect
            if (uri.getScheme().equalsIgnoreCase(BRANCH_VIEW_REDIRECT_SCHEME)) {
                //Check for actions
                if (uri.getHost().equalsIgnoreCase(BRANCH_VIEW_REDIRECT_ACTION_ACCEPT)) {
                    isBranchViewAccepted_ = true;
                    isRedirectionHandled = true;
                } else if (uri.getHost().equalsIgnoreCase(BRANCH_VIEW_REDIRECT_ACTION_CANCEL)) {
                    isBranchViewAccepted_ = false;
                    isRedirectionHandled = true;
                }
            }
        } catch (URISyntaxException ignore) {
        }
        return isRedirectionHandled;
    }

    private void showViewWithAlphaAnimation(View view) {
        AlphaAnimation animation1 = new AlphaAnimation(0.1f, 1.0f);
        animation1.setDuration(500);
        animation1.setStartOffset(10);
        animation1.setInterpolator(new AccelerateInterpolator());
        animation1.setFillAfter(true);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation1);
    }

    private void hideViewWithAlphaAnimation(View view) {
        AlphaAnimation animation1 = new AlphaAnimation(1.0f, 0.0f);
        animation1.setDuration(500);
        animation1.setStartOffset(10);
        animation1.setInterpolator(new DecelerateInterpolator());
        animation1.setFillAfter(true);
        view.setVisibility(View.GONE);
        view.startAnimation(animation1);
    }


    public void markInstallOrOpenBranchViewPending(JSONObject branchViewObj, String action) {
        openOrInstallPendingBranchView_ = new BranchView(branchViewObj, action);
    }

    public boolean isInstallOrOpenBranchViewPending(Context context) {
        return openOrInstallPendingBranchView_ != null && openOrInstallPendingBranchView_.isAvailable(context);
    }

    private class BranchView {
        private String branchViewID_ = "";
        private String branchViewAction_ = "";
        private int num_of_use_ = 1;
        private String webViewUrl_ = "";
        private String webViewHtml_ = "";
        /* This Branch view can be used for any number of times in a session. */
        private static final int USAGE_UNLIMITED = -1;

        private BranchView(JSONObject branchViewJson, String actionName) {
            try {
                branchViewAction_ = actionName;
                if (branchViewJson.has(Defines.Jsonkey.BranchViewID.getKey())) {
                    branchViewID_ = branchViewJson.getString(Defines.Jsonkey.BranchViewID.getKey());
                }
                if (branchViewJson.has(Defines.Jsonkey.BranchViewNumOfUse.getKey())) {
                    num_of_use_ = branchViewJson.getInt(Defines.Jsonkey.BranchViewNumOfUse.getKey());
                }
                if (branchViewJson.has(Defines.Jsonkey.BranchViewUrl.getKey())) {
                    webViewUrl_ = branchViewJson.getString(Defines.Jsonkey.BranchViewUrl.getKey());
                }
                if (branchViewJson.has(Defines.Jsonkey.BranchViewHtml.getKey())) {
                    webViewHtml_ = branchViewJson.getString(Defines.Jsonkey.BranchViewHtml.getKey());
                }
            } catch (Exception ignore) {

            }
        }

        private boolean isAvailable(Context context) {
            int usedCount = PrefHelper.getInstance(context).getBranchViewUsageCount(branchViewID_);
            return ((num_of_use_ > usedCount) || (num_of_use_ == USAGE_UNLIMITED));
        }

        public void updateUsageCount(Context context, String branchViewID) {
            PrefHelper.getInstance(context).updateBranchViewUsageCount(branchViewID);
        }
    }

    /**
     * Interface for calling back methods on Branch view lifecycle events
     */
    public interface IBranchViewEvents {

        /**
         * Called when a Branch view shown
         *
         * @param action       action name associated with the Branch view item
         * @param branchViewID ID for the Branch view displayed
         */
        void onBranchViewVisible(String action, String branchViewID);

        /**
         * Called when user click the positive button on Branch view
         *
         * @param action       action name associated with the App Branch item
         * @param branchViewID ID for the Branch view accepted
         */
        void onBranchViewAccepted(String action, String branchViewID);

        /**
         * Called when user click the negative button app Branch view
         *
         * @param action       action name associated with the Branch view
         * @param branchViewID ID for the Branch view cancelled
         */
        void onBranchViewCancelled(String action, String branchViewID);

        /**
         * Called when there is an error on creating or showing Branch view
         *
         * @param errorCode {@link Integer} with error code for the issue
         * @param errorMsg  {@link String} with value error message
         * @param action    action name for the Branch view failed to display
         */
        void onBranchViewError(int errorCode, String errorMsg, String action);
    }


}
