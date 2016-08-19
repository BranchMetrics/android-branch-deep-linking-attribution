package io.branch.indexing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sojanpr on 6/14/16.
 * <p>
 * The content discovery manifest is the instruction set for how the Branch SDK will *optionally* and automatically discover content
 * within your app. It parses the configuration from the server, which will tell the client whether it's eligible for content discovery.
 * This manifest is then used to inform the ContentDiscover class's behavior.
 * <p/>
 * Note that this behavior can be controlled from the dashboard.
 * </p>
 */
public class ContentDiscoveryManifest {
    private static ContentDiscoveryManifest thisInstance_;

    /* JsonObject representation for the CD manifest */
    private JSONObject cdManifestObject_;
    /* Manifest version number */
    private String manifestVersion_;
    /* Max length for an individual text item */
    private int maxTextLen_ = 0;
    /* Max num of views to do content discovery in a session */
    private int maxViewHistoryLength_ = 1;
    /* Maximum size of CD data payload per requests updating CD data to server */
    private int maxPacketSize_ = 0;
    /* Specifies if CD is enabled for this session */
    private boolean isCDEnabled_ = false;
    /* Json Array for the content path object and the filtered views for this application */
    private JSONArray contentPaths_;


    public static final String MANIFEST_VERSION_KEY = "mv";
    public static final String PACKAGE_NAME_KEY = "pn";
    public static final String HASH_MODE_KEY = "h";
    private static final String MANIFEST_KEY = "m";
    private static final String PATH_KEY = "p";
    private static final String FILTERED_KEYS = "ck";
    private static final String MAX_TEXT_LEN_KEY = "mtl";
    private static final String MAX_VIEW_HISTORY_LENGTH = "mhl";
    private static final String MAX_PACKET_SIZE_KEY = "mps";
    public static final String CONTENT_DISCOVER_KEY = "cd";

    private SharedPreferences sharedPref;
    private final String PREF_KEY = "BNC_CD_MANIFEST";

    private ContentDiscoveryManifest(Context context) {
        sharedPref = context.getSharedPreferences("bnc_content_discovery_manifest_storage", Context.MODE_PRIVATE);
        retrieve(context);
    }

    public static ContentDiscoveryManifest getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new ContentDiscoveryManifest(context);
        }
        return thisInstance_;
    }

    private void persist() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_KEY, cdManifestObject_.toString()).apply();
    }

    private void retrieve(Context context) {
        String jsonStr = sharedPref.getString(PREF_KEY, null);
        if (jsonStr != null) {
            try {
                cdManifestObject_ = new JSONObject(jsonStr);
                if (cdManifestObject_.has(MANIFEST_VERSION_KEY)) {
                    manifestVersion_ = cdManifestObject_.getString(MANIFEST_VERSION_KEY);
                }
                if (cdManifestObject_.has(MANIFEST_KEY)) {
                    contentPaths_ = cdManifestObject_.getJSONArray(MANIFEST_KEY);
                }
            } catch (JSONException ignored) {
                cdManifestObject_ = new JSONObject();
            }
        } else {
            cdManifestObject_ = new JSONObject();
        }
    }

    public void onBranchInitialised(JSONObject branchInitResp) {
        if (branchInitResp.has(CONTENT_DISCOVER_KEY)) {
            isCDEnabled_ = true;
            try {
                JSONObject cdObj = branchInitResp.getJSONObject(CONTENT_DISCOVER_KEY);

                if (cdObj.has(MANIFEST_VERSION_KEY)) {
                    manifestVersion_ = cdObj.getString(MANIFEST_VERSION_KEY);
                }
                if (cdObj.has(MAX_VIEW_HISTORY_LENGTH)) {
                    maxViewHistoryLength_ = cdObj.getInt(MAX_VIEW_HISTORY_LENGTH);
                }

                if (cdObj.has(MANIFEST_KEY)) {
                    contentPaths_ = cdObj.getJSONArray(MANIFEST_KEY);
                }
                if (cdObj.has(MAX_TEXT_LEN_KEY)) {
                    maxTextLen_ = cdObj.getInt(MAX_TEXT_LEN_KEY);
                }
                if (cdObj.has(MAX_PACKET_SIZE_KEY)) {
                    maxPacketSize_ = cdObj.getInt(MAX_PACKET_SIZE_KEY);
                }
                cdManifestObject_.put(MANIFEST_VERSION_KEY, manifestVersion_);
                cdManifestObject_.put(MANIFEST_KEY, contentPaths_);
                persist();
            } catch (JSONException ignore) {

            }
        } else {
            isCDEnabled_ = false;
        }
    }

    public CDPathProperties getCDPathProperties(Activity activity) {
        CDPathProperties pathProperties = null;
        if (contentPaths_ != null) {
            String viewPath = "/" + activity.getClass().getSimpleName();
            try {
                for (int i = 0; i < contentPaths_.length(); i++) {
                    JSONObject pathObj = contentPaths_.getJSONObject(i);
                    if (pathObj.has(PATH_KEY) && pathObj.getString(PATH_KEY).equals(viewPath)) {
                        pathProperties = new CDPathProperties(pathObj);
                        break;
                    }
                }
            } catch (JSONException ignore) {

            }
        }
        return pathProperties;
    }

    public boolean isCDEnabled() {
        return isCDEnabled_;
    }


    public int getMaxTextLen() {
        return maxTextLen_;
    }

    public int getMaxPacketSize() {
        return maxPacketSize_;
    }

    public int getMaxViewHistorySize() {
        return maxViewHistoryLength_;
    }

    public String getManifestVersion() {
        if (TextUtils.isEmpty(manifestVersion_)) {
            return "-1";
        }
        return manifestVersion_;
    }

    class CDPathProperties {
        final JSONObject pathInfo_;
        private boolean isClearText_;

        CDPathProperties(JSONObject pathInfo) {
            pathInfo_ = pathInfo;
            if (pathInfo.has(HASH_MODE_KEY)) {
                try {
                    isClearText_ = !pathInfo.getBoolean(HASH_MODE_KEY);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        public JSONArray getFilteredElements() {
            JSONArray elementArray = null;
            if (pathInfo_.has(FILTERED_KEYS)) {
                try {
                    elementArray = pathInfo_.getJSONArray(FILTERED_KEYS);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return elementArray;
        }

        public boolean isClearTextRequested() {
            return isClearText_;
        }

        public boolean isSkipContentDiscovery() {
            JSONArray filteredElements = getFilteredElements();
            return filteredElements != null && filteredElements.length() == 0;
        }

    }
}
