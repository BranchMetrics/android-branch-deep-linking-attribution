package io.branch.referral;

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

/**
 * <p>
 * *Once content discovery is enabled* through Branch, this class is responsible for discovering content entities within an app.
 * It will crawl the view hierarchy and read through text on pages similar to the functionality of a web crawler. The primary
 * uses of the data are for app indexing, content analytics, content recommendation and future content-based products.
 * <p/>
 * Note that this feature can be controlled from the dashboard.
 * </p>
 */
class ContentDiscoverer {
    private static ContentDiscoverer thisInstance_;

    private Handler handler_;
    private WeakReference<Activity> lastActivityReference_;
    private static final int VIEW_SETTLE_TIME = 1000; /* Time for a view to load its components */
    //private static final int APP_LAUNCH_DELAY = 2500; /* Time for an app to do initial pages like splash or logo */
    private String contentNavPath_; // User navigation path for the content.
    private String referredUrl_; // The url which opened this app session

    private static final String TIME_STAMP_KEY = "ts";
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

    static ContentDiscoverer getInstance() {
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

    public void discoverContent(final Activity activity, String referredUrl) {
        cdManifest_ = ContentDiscoveryManifest.getInstance(activity);
        referredUrl_ = referredUrl;

        //Scan for content only if the app is started by  a link click or if the content path for this view is already cached
        ContentDiscoveryManifest.CDPathProperties pathProperties = cdManifest_.getCDPathProperties(activity);

        if (pathProperties != null) { // Check if view is available in CD manifest
            // Discover content only if element array is not empty json array
            if (pathProperties.getFilteredElements() == null || pathProperties.getFilteredElements().length() > 0) {
                discoverContent(activity);
            }
        } else if (!TextUtils.isEmpty(referredUrl_)) {
            discoverContent(activity);
        } else if (cdManifest_.isDebugEnabled()) {
            discoverContent(activity);
        }
    }

    private void discoverContent(Activity activity) {
        if (discoveredViewList_.size() < cdManifest_.getMaxViewHistorySize()) { // check if max discovery views reached
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = new WeakReference<>(activity);
            handler_.postDelayed(readContentRunnable, VIEW_SETTLE_TIME);
        }
    }


    public void onActivityStopped(Activity activity) {
        if (lastActivityReference_ != null && lastActivityReference_.get() != null
                && lastActivityReference_.get().getClass().getName().equals(activity.getClass().getName())) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = null;
        }
    }

    public void onSessionStarted(final Activity activity, String referredUrl) {
        discoveredViewList_ = new ArrayList<>();
        discoverContent(activity, referredUrl);
    }

    // ---------------Private methods----------------------//

    private Runnable readContentRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                if (cdManifest_.isCDEnabled() && lastActivityReference_ != null && lastActivityReference_.get() != null) {
                    Activity activity = lastActivityReference_.get();
                    JSONObject contentEvent = new JSONObject();
                    contentEvent.put(TIME_STAMP_KEY, System.currentTimeMillis());
                    contentEvent.put(NAV_PATH_KEY, contentNavPath_);
                    if (!TextUtils.isEmpty(referredUrl_)) {
                        contentEvent.put(REFERRAL_LINK_KEY, referredUrl_);
                    }
                    String viewName = "/" + activity.getClass().getSimpleName();
                    contentEvent.put(VIEW_KEY, viewName);

                    // Check if the view is already discovered. If already discovered non need to get view content again
                    if (discoveredViewList_.contains(viewName) == false) {


                        JSONArray contentKeysArray = new JSONArray();
                        contentEvent.put(CONTENT_KEYS_KEY, contentKeysArray);

                        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);

                        ContentDiscoveryManifest.CDPathProperties cdPathProperties = cdManifest_.getCDPathProperties(activity);
                        boolean isClearText = false;
                        JSONArray filteredElements = null;
                        if (cdPathProperties != null) {
                            isClearText = cdPathProperties.isClearTextRequested();
                            contentEvent.put(ContentDiscoveryManifest.HASH_MODE_KEY, !isClearText);
                            filteredElements = cdPathProperties.getFilteredElements();
                        }
                        if (cdManifest_.isDebugEnabled()) { // In debug mode discove all TextView keys and values
                            JSONArray contentDataArray = new JSONArray();
                            contentEvent.put(CONTENT_DATA_KEY, contentDataArray);
                            discoverViewContents(rootView, null, contentKeysArray, activity.getResources(), isClearText);
                        } else if (filteredElements != null && filteredElements.length() > 0) { // If filtered views available get filtered views and values
                            JSONArray contentDataArray = new JSONArray();
                            contentEvent.put(CONTENT_DATA_KEY, contentDataArray);
                            discoverFilteredViewContents(filteredElements, contentDataArray, contentKeysArray, activity, isClearText);
                        } else { // If filter is absent discover all text field keys
                            discoverViewContents(rootView, null, contentKeysArray, activity.getResources(), isClearText);
                        }
                        discoveredViewList_.add(viewName);
                    }

                    // Cache the analytics data for future use
                    PrefHelper.getInstance(activity).saveBranchAnalyticsData(contentEvent);
                    lastActivityReference_ = null;
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
        if (PrefHelper.getInstance(context).getBranchAnalyticsData().length() > 0) {
            cdObj = new JSONObject();
            try {
                ContentDiscoveryManifest cdManifest = ContentDiscoveryManifest.getInstance(context);
                cdObj.put(ContentDiscoveryManifest.MANIFEST_VERSION_KEY, cdManifest.getManifestVersion());
                cdObj.put(ENTITIES_KEY, PrefHelper.getInstance(context).getBranchAnalyticsData());
                PrefHelper.getInstance(context).clearBranchAnalyticsData();
                if (DeviceInfo.getInstance() != null) {
                    cdObj.put(PACKAGE_NAME_KEY, DeviceInfo.getInstance().getPackageName());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
