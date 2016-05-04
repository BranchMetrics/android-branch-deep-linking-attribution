package io.branch.referral;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
    private boolean loadingHtmlInBackGround_  = false;

    private static final String BRANCH_VIEW_REDIRECT_SCHEME = "branch-cta";
    private static final String BRANCH_VIEW_REDIRECT_ACTION_ACCEPT = "accept";
    private static final String BRANCH_VIEW_REDIRECT_ACTION_CANCEL = "cancel";

    public static final int BRANCH_VIEW_ERR_ALREADY_SHOWING = -200;
    public static final int BRANCH_VIEW_ERR_INVALID_VIEW = -201;
    public static final int BRANCH_VIEW_ERR_TEMP_UNAVAILABLE = -202;
    public static final int BRANCH_VIEW_ERR_REACHED_LIMIT = -203;

    private String parentActivityClassName_;
    private boolean webViewLoadError_;
    private Dialog branchViewDialog_;


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

    public boolean showPendingBranchView(Context appContext) {
        boolean isBranchViewShowed = showBranchView(openOrInstallPendingBranchView_, appContext, null);
        if (isBranchViewShowed) {
            openOrInstallPendingBranchView_ = null;
        }
        return isBranchViewShowed;
    }

    public boolean showBranchView(JSONObject branchViewObj, String actionName, Context appContext, final IBranchViewEvents callback) {
        BranchView branchView = new BranchView(branchViewObj, actionName);
        return showBranchView(branchView, appContext, callback);
    }

    private boolean showBranchView(BranchView branchView, Context appContext, final IBranchViewEvents callback) {
        if (isBranchViewDialogShowing_ || loadingHtmlInBackGround_) {
            if (callback != null) {
                callback.onBranchViewError(BRANCH_VIEW_ERR_ALREADY_SHOWING, "Unable to create a Branch view. A Branch view is already showing", branchView.branchViewAction_);
            }
            return false;
        }

        isBranchViewDialogShowing_ = false;
        isBranchViewAccepted_ = false;

        if (appContext != null && branchView != null) {
            // Check for maximum usage
            if (branchView.isAvailable(appContext)) {
                // Check if the web view Html is present. If html is present load the view directly.
                if (!TextUtils.isEmpty(branchView.webViewHtml_)) {
                    createAndShowBranchView(branchView, appContext, callback);
                }
                // If web view html is not present load the branch view with html obtained from url.
                else {
                    loadingHtmlInBackGround_ = true;
                    new loadBranchViewTask(branchView, appContext, callback).execute();
                }
                return true;
            } // If reached maximum limit
            else {
                if (callback != null) {
                    callback.onBranchViewError(BRANCH_VIEW_ERR_REACHED_LIMIT, "Unable to create this Branch view. Reached maximum usage limit ", branchView.branchViewAction_);
                }
            }

        }
        return false;
    }

    private void createAndShowBranchView(final BranchView branchView, Context appContext, final IBranchViewEvents callback) {
        if (appContext != null && branchView != null) {
            final WebView webView = new WebView(appContext);
            webView.getSettings().setJavaScriptEnabled(true);
            if (Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            webViewLoadError_ = false;
            if (!TextUtils.isEmpty(branchView.webViewHtml_)) {
                webView.loadDataWithBaseURL(null, branchView.webViewHtml_, "text/html", "utf-8", null);
            } else {
                return; // Error no url or Html
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    boolean isHandled = handleUserActionRedirect(url);
                    if (!isHandled) {
                        view.loadUrl(url);
                    } else {
                        if (branchViewDialog_ != null) {
                            branchViewDialog_.dismiss();
                        }
                    }
                    return isHandled;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    webViewLoadError_ = true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    openBranchViewDialog(branchView, callback, webView);
                }
            });
        }
    }

    private void openBranchViewDialog(final BranchView branchView, final IBranchViewEvents callback, WebView webView) {
        if (!webViewLoadError_ && Branch.getInstance() != null && Branch.getInstance().currentActivityReference_ != null) {
            Activity currentActivity = Branch.getInstance().currentActivityReference_.get();
            if (currentActivity != null) {
                branchView.updateUsageCount(currentActivity.getApplicationContext(), branchView.branchViewID_);
                parentActivityClassName_ = currentActivity.getClass().getName();

                RelativeLayout layout = new RelativeLayout(currentActivity);
                layout.setVisibility(View.GONE);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layout.addView(webView, layoutParams);
                layout.setBackgroundColor(Color.TRANSPARENT);

                if(branchViewDialog_ != null && branchViewDialog_.isShowing()){
                    if (callback != null) {
                        callback.onBranchViewError(BRANCH_VIEW_ERR_ALREADY_SHOWING, "Unable to create a Branch view. A Branch view is already showing", branchView.branchViewAction_);
                    }
                    return;
                }
                branchViewDialog_ = new Dialog(currentActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                branchViewDialog_.setContentView(layout);

                layout.setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
                branchViewDialog_.show();
                showViewWithAlphaAnimation(layout);
                showViewWithAlphaAnimation(webView);
                isBranchViewDialogShowing_ = true;
                if (callback != null) {
                    callback.onBranchViewVisible(branchView.branchViewAction_, branchView.branchViewID_);
                }

                branchViewDialog_.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        isBranchViewDialogShowing_ = false;
                        branchViewDialog_ = null;

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
        } else {
            isBranchViewDialogShowing_ = false;
            if (callback != null) {
                callback.onBranchViewError(BRANCH_VIEW_ERR_TEMP_UNAVAILABLE, "Unable to create a Branch view due to a temporary network error", branchView.branchViewAction_);
            }
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


    public boolean markInstallOrOpenBranchViewPending(JSONObject branchViewObj, String action) {
        boolean isMarked = false;
        BranchView branchView = new BranchView(branchViewObj, action);
        if (branchView != null && Branch.getInstance().currentActivityReference_ != null) {
            Activity currentActivity = Branch.getInstance().currentActivityReference_.get();
            if (currentActivity != null && branchView.isAvailable(currentActivity)) {
                openOrInstallPendingBranchView_ = new BranchView(branchViewObj, action);
                isMarked = true;
            }
        }
        return isMarked;
    }

    public boolean isInstallOrOpenBranchViewPending(Context context) {
        return openOrInstallPendingBranchView_ != null && openOrInstallPendingBranchView_.isAvailable(context);
    }


    private class loadBranchViewTask extends AsyncTask<Void, Void, Boolean> {
        private final BranchView branchView;
        private final Context context;
        private final IBranchViewEvents callback;
        private String htmlString;

        public loadBranchViewTask(BranchView branchView, Context context, IBranchViewEvents callback) {
            this.branchView = branchView;
            this.context = context;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            int code = -1;
            try {
                URL url = new URL(branchView.webViewUrl_);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                code = connection.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    InputStream inputStream = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                    branchView.webViewHtml_ = outputStream.toString("UTF-8");
                    outputStream.close();
                    inputStream.close();
                }
            } catch (Exception ignore) {
            }
            return code == HttpURLConnection.HTTP_OK;
        }

        @Override
        protected void onPostExecute(Boolean loadHtml) {
            super.onPostExecute(loadHtml);
            if (loadHtml) {
                createAndShowBranchView(branchView, context, callback);
            } else {
                if (callback != null) {
                    callback.onBranchViewError(BRANCH_VIEW_ERR_TEMP_UNAVAILABLE, "Unable to create a Branch view due to a temporary network error", branchView.branchViewAction_);
                }
            }
            loadingHtmlInBackGround_ = false;
        }
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

    public void onCurrentActivityDestroyed(Activity activity) {
        if (parentActivityClassName_ != null && parentActivityClassName_.equalsIgnoreCase(activity.getClass().getName())) {
            //In case of dialog destroyed due to orientation change dialog won't provide a dismiss event
            isBranchViewDialogShowing_ = false;
        }
    }


}
