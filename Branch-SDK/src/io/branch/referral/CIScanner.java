package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * Class for handling content indexing for the views.
 * App views with relevant content are indexed by branch for content analytics.
 * </p>
 */
class CIScanner {
    private static CIScanner thisInstance_;

    private String triggerUri_;
    private Handler handler_;
    private WeakReference<Activity> lastActivityReference_;
    private static final int VIEW_SETTLE_TIME = 2000; /* Time for a view to load its components */
    private static final int APP_LAUNCH_DELAY = 1500; /* Time for an app to do initial pages like splash or logo */
    private String contentNavPath_; // User navigation path for the content.

    private static final String TIME_STAMP_KEY = "ts";
    private static final String ACTION_KEY = "a";
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
    private CIManifest ciManifest_;

    static CIScanner getInstance() {
        if (thisInstance_ == null) {
            thisInstance_ = new CIScanner();
        }
        return thisInstance_;
    }

    private CIScanner() {
        handler_ = new Handler();
        hashHelper_ = new HashHelper();
    }

    //------------------------- Public methods---------------------------------//

    public void scanForContent(final Activity activity, boolean isSessionStart) {
        int viewRenderWait = VIEW_SETTLE_TIME;
        if (isSessionStart) {
            triggerUri_ = null;
        }

        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null && intent.getData() != null && intent.getData().getScheme() != null) {
                triggerUri_ = intent.getData().toString();
                // Add app launch delay if launching on init
                if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                    viewRenderWait = viewRenderWait + APP_LAUNCH_DELAY;
                }
            }
        }
        ciManifest_ = CIManifest.getInstance(activity);
        //Scan for content only if the app is started by  a link click or if the content path for this view is already cached
        CIManifest.CIPathProperties pathProperties = ciManifest_.getCIPathProperties(activity);

        if (triggerUri_ != null || pathProperties != null) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = new WeakReference<>(activity);
            handler_.postDelayed(readContentRunnable, viewRenderWait);
        }
    }


    public void onActivityStopped(Activity activity) {
        if (lastActivityReference_ != null && lastActivityReference_.get() != null
                && lastActivityReference_.get().getClass().getName().equals(activity.getClass().getName())) {
            handler_.removeCallbacks(readContentRunnable);
            lastActivityReference_ = null;
        }
    }

    // ---------------Private methods----------------------//

    private Runnable readContentRunnable = new Runnable() {
        @Override
        public void run() {

            try {
                if (lastActivityReference_ != null && lastActivityReference_.get() != null) {
                    Activity activity = lastActivityReference_.get();
                    JSONObject contentEvent = new JSONObject();
                    contentEvent.put(TIME_STAMP_KEY, System.currentTimeMillis());
                    contentEvent.put(ACTION_KEY, "v");
                    contentEvent.put(NAV_PATH_KEY, contentNavPath_);
                    contentEvent.put(REFERRAL_LINK_KEY, triggerUri_);
                    contentEvent.put(CONTENT_LINK_KEY, "");
                    contentEvent.put(CONTENT_META_DATA_KEY, new JSONObject());
                    contentEvent.put(VIEW_KEY, "/" + activity.getClass().getSimpleName());

                    JSONArray contentDataArray = new JSONArray();
                    contentEvent.put(CONTENT_DATA_KEY, contentDataArray);

                    JSONArray contentKeysArray = new JSONArray();
                    contentEvent.put(CONTENT_KEYS_KEY, contentKeysArray);

                    ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                    boolean isClearText = CIManifest.getInstance(activity).isClearTextRequested();

                    JSONArray filteredElements = ciManifest_.getCIPathProperties(activity).getFilteredElements();
                    if (filteredElements.length() > 0) {
                        readThroughFilteredChildViews(filteredElements, contentDataArray, contentKeysArray, activity, isClearText);
                    } else {
                        readThroughChildViews(rootView, contentDataArray, contentKeysArray, activity.getResources(), isClearText);
                    }

                    // Cache the analytics data for future use
                    PrefHelper.getInstance(activity).saveBranchAnalyticsData(contentEvent);
                    lastActivityReference_ = null;
                    triggerUri_ = null;
                }

            } catch (JSONException ignore) {
            }
        }
//        }
    };


    private void readThroughFilteredChildViews(JSONArray viewIDArray, JSONArray contentDataArray, JSONArray contentKeysArray, Activity activity, boolean isClearText) {
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

    private void readThroughChildViews(ViewGroup viewGroup, JSONArray contentDataArray, JSONArray contentKeysArray, Resources res, boolean isClearText) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView.getVisibility() == View.VISIBLE) {
                if (childView instanceof ViewGroup) {
                    readThroughChildViews((ViewGroup) childView, contentDataArray, contentKeysArray, res, isClearText);
                } else {
                    String viewName = res.getResourceEntryName(childView.getId());
                    updateElementData(viewName, childView, isClearText, contentDataArray, contentKeysArray);
                }
            }
        }
    }

    private void updateElementData(String viewName, View view, boolean isClearText, JSONArray contentDataArray, JSONArray contentKeysArray) {
        String viewVal = null;
        if (view instanceof TextView) {
            TextView txtView = (TextView) view;
            viewVal = null;
            if (txtView.getText() != null) {
                viewVal = txtView.getText().toString().substring(0, Math.min(viewVal.length(), ciManifest_.getMaxTextLen()));
                viewVal = isClearText ? viewVal : hashHelper_.hashContent(viewVal);
            }
            contentDataArray.put(viewVal);
            contentKeysArray.put(viewName);
        }

    }

    public JSONObject getCIDataForCloseRequest(Context context) {
        JSONObject ciObj = null;
        if (PrefHelper.getInstance(context).getBranchAnalyticsData().length() > 0) {
            ciObj = new JSONObject();
            try {
                CIManifest ciManifest = CIManifest.getInstance(context);
                ciObj.put(CIManifest.MANIFEST_VERSION_KEY, ciManifest.getManifestVersion());
                ciObj.put(CIManifest.HASH_MODE_KEY, !ciManifest.isClearTextRequested());
                ciObj.put(PACKAGE_NAME_KEY, context.getPackageName());
                ciObj.put(ENTITIES_KEY, PrefHelper.getInstance(context).getBranchAnalyticsData());
                PrefHelper.getInstance(context).clearBranchAnalyticsData();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return ciObj;
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
                // No need to worry about char set here since CI use this only to check uniqueness
                hashedVal = new String(messageDigest_.digest());
            }
            return hashedVal;
        }
    }

}
