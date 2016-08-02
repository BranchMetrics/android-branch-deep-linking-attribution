package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

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
class ContentDiscoveryManifest {
    private static ContentDiscoveryManifest thisInstance_;

    /* JsonObject representation for the CD manifest */
    private JSONObject cdManifestObject_;
    /* Manifest version number */
    private String manifestVersion_;
    /* Specifies whether the content values should be hashed or not*/
    private boolean hashContent_ = true;
    /* Max length for an individual text item */
    private int maxTextLen_ = 0;
    /* Maximum size of CD data payload per requests updating CD data to server */
    private int maxPacketSize_ = 0;
    /* Specifies if the current device is a Branch Device */
    private boolean isUserDevice = true;
    /* Specifies if CD is enabled for this session */
    private boolean isCDEnabled_ = false;
    /* Json Array for the content path object and the filtered views for this application */
    private JSONArray contentPaths_;

    public static final String MANIFEST_VERSION_KEY = "mv";
    public static final String HASH_MODE_KEY = "h";
    private static final String MANIFEST_KEY = "m";
    private static final String PATH_KEY = "p";
    private static final String ELEMENT_KEY = "e";
    private static final String USER_DEVICE_KEY = "user_device";
    private static final String MAX_TEXT_LEN_KEY = "mtl";
    private static final String MAX_PACKET_SIZE_KEY = "mps";
    private static final String CONTENT_DISCOVER_KEY = "cd";


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
                if (cdObj.has(USER_DEVICE_KEY)) {
                    isUserDevice = cdObj.getBoolean(USER_DEVICE_KEY);
                }
                if (cdObj.has(MANIFEST_VERSION_KEY)) {
                    manifestVersion_ = cdObj.getString(MANIFEST_VERSION_KEY);
                }
                if (cdObj.has(HASH_MODE_KEY)) {
                    hashContent_ = cdObj.getBoolean(HASH_MODE_KEY);
                }
                if (cdObj.has(MANIFEST_KEY)) {
                    JSONArray newContentPaths = cdObj.getJSONArray(MANIFEST_KEY);
                    if (contentPaths_ == null) {
                        contentPaths_ = new JSONArray();
                        for (int i = 0; i < newContentPaths.length(); i++) {
                            contentPaths_.put(newContentPaths.getJSONObject(i));
                        }
                    }
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

    public boolean isClearTextRequested() {
        return !hashContent_;
    }

    public boolean isUserDevice() {
        return isUserDevice;
    }

    public int getMaxTextLen() {
        return maxTextLen_;
    }

    public int getMaxPacketSize() {
        return maxPacketSize_;
    }

    public String getManifestVersion() {
        return manifestVersion_;
    }


    class CDPathProperties extends JSONObject {
        final JSONObject pathInfo_;

        CDPathProperties(JSONObject pathInfo) {
            pathInfo_ = pathInfo;
        }

        public JSONArray getFilteredElements() {
            JSONArray elementArray = new JSONArray();
            if (pathInfo_.has(ELEMENT_KEY)) {
                try {
                    elementArray = pathInfo_.getJSONArray(ELEMENT_KEY);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return elementArray;
        }

    }
}
