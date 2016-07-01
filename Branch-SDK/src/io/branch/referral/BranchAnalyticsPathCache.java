package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sojanpr on 6/14/16.
 */
class BranchAnalyticsPathCache {
    private static BranchAnalyticsPathCache thisInstance_;
    private JSONObject contentPaths_;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private final String PREF_KEY = "CONTENT_PATHS";

    private BranchAnalyticsPathCache(Context context) {
        sharedPref = context.getSharedPreferences("BNC_ContentPath_Array", Context.MODE_PRIVATE);
        retrieve(context);
    }

    public static BranchAnalyticsPathCache getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new BranchAnalyticsPathCache(context);
        }
        return thisInstance_;
    }

    private void persist() {
        editor = sharedPref.edit();
        editor.putString(PREF_KEY, contentPaths_.toString()).commit();
    }

    private void retrieve(Context context) {
        String jsonStr = sharedPref.getString(PREF_KEY, null);
        if (jsonStr != null) {
            try {
                contentPaths_ = new JSONObject(jsonStr);
            } catch (JSONException ignored) {
                contentPaths_ = new JSONObject();
            }
        } else {
            contentPaths_ = new JSONObject();
        }
    }

    /**
     * Add a content path to the list of content path if not existing already
     *
     * @param contentPath Content path to add
     */
    public void addContentPath(String contentPath) {
        if (!contentPaths_.toString().contains(contentPath)) {
            try {
                contentPaths_.put(contentPath, new JSONArray());
            } catch (JSONException ignore) {
            }
            persist();
        }
    }

    /*
     * Updates the content paths and associated view list for this application
     * @param contentPathArray JsonArray containing the content paths and view list
     * content path json format [{“path”:"content path",“view_list”:[“view1”]}]
     */
    public void updateContentPaths(JSONArray contentPathArray) {
        //
        try {
            for (int i = 0; i < contentPathArray.length(); i++) {
                JSONObject contentPathObj = contentPathArray.getJSONObject(i);
                if (contentPathObj.has(Defines.Jsonkey.Path.getKey())) {
                    String contentPath = contentPathObj.getString(Defines.Jsonkey.Path.getKey());
                    JSONArray viewList = new JSONArray();
                    if (contentPathObj.has(Defines.Jsonkey.ViewList.getKey())) {
                        viewList = contentPathObj.getJSONArray(Defines.Jsonkey.ViewList.getKey());
                    }
                    contentPaths_.put(contentPath, viewList);
                }
            }
            persist();
        } catch (JSONException ignore) {
        }
    }

    public boolean isContentPathAvailable(Activity activity) {
        return (contentPaths_ != null && contentPaths_.toString().contains("/"+activity.getClass().getSimpleName()));
    }

    /**
     * Get the list of preferred view ids for this content path
     *
     * @param contentPath Content path to get the preferred view ids
     * @return A JsonArray with preferred view ids . An empty JsonArray if there is no preferred view ids
     */
    public JSONArray getPreferredViewIdList(String contentPath) {
        JSONArray preferredViewListArray = new JSONArray();
        try {
            if (contentPaths_ != null && contentPaths_.has(contentPath)) {
                preferredViewListArray = contentPaths_.getJSONArray(contentPath);
            }
        } catch (JSONException ignore) {

        }
        return preferredViewListArray;
    }

}
