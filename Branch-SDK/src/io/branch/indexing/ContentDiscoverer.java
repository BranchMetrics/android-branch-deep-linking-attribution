package io.branch.indexing;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import io.branch.referral.PrefHelper;

/**
 * <p>
 * *Once content discovery is enabled* through Branch, this class is responsible for discovering content entities within an app.
 * It will crawl the view hierarchy and read through text on pages similar to the functionality of a web crawler. The primary
 * uses of the data are for app indexing, content analytics, content recommendation and future content-based products.
 * <p/>
 * Note that this feature can be controlled from the dashboard.
 * </p>
 */
public class ContentDiscoverer {
    private static ContentDiscoverer thisInstance_;

    private Handler handler_;
    private WeakReference<Activity> lastActivityReference_;
    private static final int VIEW_SETTLE_TIME = 1000; /* Time for a view to load its components */
    private String referredUrl_; // The url which opened this app session
    private JSONObject contentEvent_;

    private static final String TIME_STAMP_KEY = "ts";
    private static final String TIME_STAMP_CLOSE_KEY = "tc";
    private static final String NAV_PATH_KEY = "n";
    private static final String REFERRAL_LINK_KEY = "rl";
    private static final String CONTENT_LINK_KEY = "cl";
    private static final String CONTENT_META_DATA_KEY = "cm";
    private static final String VIEW_KEY = "v";
    private static final String CONTENT_DATA_KEY = "cd";
    private static final String CONTENT_KEYS_KEY = "ck";
    private static final String PACKAGE_NAME_KEY = "p";
    private static final String ENTITIES_KEY = "e";

    private final HashHelper hashHelper_;
    private ContentDiscoveryManifest cdManifest_;

    public static ContentDiscoverer getInstance() {
        if (thisInstance_ == null) {
            thisInstance_ = new ContentDiscoverer();
        }
        return thisInstance_;
    }

    private ContentDiscoverer() {
        handler_ = new Handler();
        hashHelper_ = new HashHelper();
    }

    private int discoveredViewInThisSession_ = 0; // Denote the number  of views discovered in this session
    private ArrayList<String> discoveredViewList_ = new ArrayList<>(); // List for saving already discovered views path

    //------------------------- Public methods---------------------------------//

    //make discoverContent a runnable, kill runnable onActivtyStopped

    public void discoverContent(final Activity activity, final String referredUrl) {
        cdManifest_ = ContentDiscoveryManifest.getInstance(activity);
        referredUrl_ = referredUrl;

        //Scan for content only if the app is started by  a link click or if the content path for this view is already cached
        ContentDiscoveryManifest.CDPathProperties pathProperties = cdManifest_.getCDPathProperties(activity);

        if (pathProperties != null) { // Check if view is available in CD manifest
            // Discover content only if element array is not empty json array
            if (!pathProperties.isSkipContentDiscovery()) {
                discoverContent(activity);
            }
        } else if (!TextUtils.isEmpty(referredUrl_)) {
            discoverContent(activity);
        }
    }

    public void onActivityStopped(Activity activity) {
        if (lastActivityReference_ != null && lastActivityReference_.get() != null
                && lastActivityReference_.get().getClass().getName().equals(activity.getClass().getName())) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = null;
        }
        updateLastViewTimeStampIfNeeded();
    }

    public void onSessionStarted(final Activity activity, String referredUrl) {
        discoveredViewList_ = new ArrayList<>();
        discoverContent(activity, referredUrl);
    }

    // ---------------Private methods----------------------//

    private void discoverContent(Activity activity) {
        if (discoveredViewList_.size() < cdManifest_.getMaxViewHistorySize()) { // check if max discovery views reached
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = new WeakReference<>(activity);
            handler_.postDelayed(readContentRunnable, VIEW_SETTLE_TIME);
        }
    }

    private void updateLastViewTimeStampIfNeeded() {
        try {
            if (contentEvent_ != null) {
                contentEvent_.put(TIME_STAMP_CLOSE_KEY, System.currentTimeMillis());
            }
        } catch (JSONException ignore) {
        }
    }

    private Runnable readContentRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (cdManifest_.isCDEnabled() && lastActivityReference_ != null && lastActivityReference_.get() != null) {
                    Activity activity = lastActivityReference_.get();
                    contentEvent_ = new JSONObject();
                    contentEvent_.put(TIME_STAMP_KEY, System.currentTimeMillis());
                    if (!TextUtils.isEmpty(referredUrl_)) {
                        contentEvent_.put(REFERRAL_LINK_KEY, referredUrl_);
                    }
                    String viewName = "/" + activity.getClass().getSimpleName();
                    contentEvent_.put(VIEW_KEY, viewName);

                    //ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                    //ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
                    ViewGroup rootView = (ViewGroup) activity.getCurrentFocus();
                    //ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);

                    if (rootView != null) {
                        ContentDiscoveryManifest.CDPathProperties cdPathProperties = cdManifest_.getCDPathProperties(activity);
                        boolean isClearText = cdPathProperties != null && cdPathProperties.isClearTextRequested();
                        JSONArray filteredElements = null;
                        if (cdPathProperties != null) {
                            isClearText = cdPathProperties.isClearTextRequested();
                            contentEvent_.put(ContentDiscoveryManifest.HASH_MODE_KEY, !isClearText);
                            filteredElements = cdPathProperties.getFilteredElements();
                        }
                        if (filteredElements != null && filteredElements.length() > 0) { // If filtered views available get filtered views and values
                            JSONArray contentKeysArray = new JSONArray();
                            contentEvent_.put(CONTENT_KEYS_KEY, contentKeysArray);

                            JSONArray contentDataArray = new JSONArray();
                            contentEvent_.put(CONTENT_DATA_KEY, contentDataArray);
                            discoverFilteredViewContents(filteredElements, contentDataArray, contentKeysArray, activity, isClearText);
                        } else { // If filter is absent discover all text field keys
                            if (!discoveredViewList_.contains(viewName)) {  // Check if the view is already discovered. If already discovered no need to get view content keys again
                                JSONArray contentKeysArray = new JSONArray();
                                contentEvent_.put(CONTENT_KEYS_KEY, contentKeysArray);
                                discoverViewContents(rootView, null, contentKeysArray, activity.getResources(), isClearText);
                            }
                        }
                        discoveredViewList_.add(viewName);

                        // Cache the analytics data for future use
                        PrefHelper.getInstance(activity).saveBranchAnalyticsData(contentEvent_);

                        if (cdManifest_.getDiscoveryRepeatTime() != -1) {
                            handler_.postDelayed(readContentRunnable, cdManifest_.getDiscoveryRepeatTime());
                        } else {
                            lastActivityReference_ = null;
                        }
                    }
                }

            } catch (JSONException ignore) {
            }
        }
    };


    private void discoverFilteredViewContents(JSONArray viewIDArray, JSONArray contentDataArray, JSONArray contentKeysArray, Activity activity, boolean isClearText) {
        try {
            for (int i = 0; i < viewIDArray.length(); i++) {
                String viewName = viewIDArray.getString(i);
                int id = activity.getResources().getIdentifier(viewIDArray.getString(i), "id", activity.getPackageName());
                View childView = activity.findViewById(id);
                updateElementData(viewName, childView, isClearText, contentDataArray, contentKeysArray);
            }
        } catch (JSONException ignore) {

        }
    }

    private void discoverViewContents(ViewGroup viewGroup, JSONArray contentDataArray, JSONArray contentKeysArray, Resources res, boolean isClearText) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView.getVisibility() == View.VISIBLE) if (childView instanceof ViewGroup) {
                discoverViewContents((ViewGroup) childView, contentDataArray, contentKeysArray, res, isClearText);
            } else {
                String viewName = res.getResourceEntryName(childView.getId());
                updateElementData(viewName, childView, isClearText, contentDataArray, contentKeysArray);
            }
        }
    }

    private void updateElementData(String viewName, View view, boolean isClearText, JSONArray contentDataArray, JSONArray contentKeysArray) {
        String viewVal;
        if (view instanceof TextView) {
            TextView txtView = (TextView) view;
            if (contentDataArray != null) { // Will be non null in case of discovering the values
                viewVal = null;
                if (txtView.getText() != null) {
                    viewVal = txtView.getText().toString().substring(0, Math.min(txtView.getText().toString().length(), cdManifest_.getMaxTextLen()));
                    viewVal = isClearText ? viewVal : hashHelper_.hashContent(viewVal);
                }
                contentDataArray.put(viewVal);
            }
            contentKeysArray.put(viewName);
        }

    }

    public JSONObject getContentDiscoverDataForCloseRequest(Context context) {
        JSONObject cdObj = null;
        JSONObject branchAnalyticalData = PrefHelper.getInstance(context).getBranchAnalyticsData();
        if (branchAnalyticalData.length() > 0 && branchAnalyticalData.toString().length() < cdManifest_.getMaxPacketSize()) {
            cdObj = new JSONObject();
            try {
                ContentDiscoveryManifest cdManifest = ContentDiscoveryManifest.getInstance(context);
                cdObj.put(ContentDiscoveryManifest.MANIFEST_VERSION_KEY, cdManifest.getManifestVersion())
                        .put(ENTITIES_KEY, branchAnalyticalData);
                if (context != null) {
                    cdObj.put(PACKAGE_NAME_KEY, context.getPackageName());
                    cdObj.put(PACKAGE_NAME_KEY, context.getPackageName());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        PrefHelper.getInstance(context).clearBranchAnalyticsData();
        return cdObj;
    }


    /**
     * Helper class for
     */
    private class HashHelper {
        MessageDigest messageDigest_;

        public HashHelper() {
            try {
                messageDigest_ = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ignore) {
            }
        }

        public String hashContent(String content) {
            String hashedVal = "";
            if (messageDigest_ != null) {
                messageDigest_.reset();
                messageDigest_.update(content.getBytes());
                // No need to worry about char set here since CD use this only to check uniqueness
                hashedVal = new String(messageDigest_.digest());
            }
            return hashedVal;
        }
    }

}