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
 * * Note that this behavior can be controlled from the dashboard.
 * </p>
 */
public class ContentDiscoveryManifest {
    private static ContentDiscoveryManifest thisInstance_;

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
    static final String HASH_MODE_KEY = "h";
    private static final String MANIFEST_KEY = "m";
    private static final String PATH_KEY = "p";
    private static final String FILTERED_KEYS = "ck";
    private static final String MAX_TEXT_LEN_KEY = "mtl";
    private static final String MAX_VIEW_HISTORY_LENGTH = "mhl";
    private static final String MAX_PACKET_SIZE_KEY = "mps";
    public static final String CONTENT_DISCOVER_KEY = "cd";
    private static final String DISCOVERY_REPEAT_INTERVAL = "dri";
    private static final String MAX_DISCOVERY_REPEAT = "mdr";
    private static final String LOCAL_INDEX_CTRL = "lic";
    private static LocalIndexControl localIndexControl;
    static final int DEF_MAX_DISCOVERY_REPEAT = 15; // Default Maximum number for discovery repeat
    static final int DRI_MINIMUM_THRESHOLD = 500; // Minimum value for Discovery repeat interval
    
    
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
    
    private void persist(JSONObject cdManifestObject) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_KEY, cdManifestObject.toString()).apply();
    }
    
    private void retrieve(Context context) {
        String jsonStr = sharedPref.getString(PREF_KEY, null);
        if (jsonStr != null) {
            try {
                JSONObject cdManifestObject = new JSONObject(jsonStr);
                updateManifestFromJson(cdManifestObject);
            } catch (JSONException ignored) {
            }
        }
    }
    
    public void onBranchInitialised(JSONObject branchInitResp) {
        if (branchInitResp.has(CONTENT_DISCOVER_KEY)) {
            isCDEnabled_ = true;
            try {
                JSONObject cdObj = branchInitResp.optJSONObject(CONTENT_DISCOVER_KEY);
                if (cdObj != null) {
                    updateManifestFromJson(cdObj);
                    JSONObject cdManifestObject = new JSONObject();
                    cdManifestObject.put(MANIFEST_VERSION_KEY, manifestVersion_);
                    cdManifestObject.put(MANIFEST_KEY, contentPaths_);
                    cdManifestObject.put(MAX_VIEW_HISTORY_LENGTH, maxViewHistoryLength_);
                    cdManifestObject.put(MAX_TEXT_LEN_KEY, maxTextLen_);
                    cdManifestObject.put(MAX_PACKET_SIZE_KEY, maxPacketSize_);
                    cdManifestObject.put(LOCAL_INDEX_CTRL, cdObj.optJSONObject(LOCAL_INDEX_CTRL));
                    persist(cdManifestObject);
                }
            } catch (JSONException ignore) {
            }
        } else {
            isCDEnabled_ = false;
        }
    }

    private void updateManifestFromJson(JSONObject cdObj) {
        manifestVersion_ = cdObj.optString(MANIFEST_VERSION_KEY);
        maxViewHistoryLength_ = cdObj.optInt(MAX_VIEW_HISTORY_LENGTH, 1);
        contentPaths_ = cdObj.optJSONArray(MANIFEST_KEY);
        maxTextLen_ = cdObj.optInt(MAX_TEXT_LEN_KEY, 0);
        maxPacketSize_ = cdObj.optInt(MAX_PACKET_SIZE_KEY);
        localIndexControl = new LocalIndexControl(cdObj.optJSONObject(LOCAL_INDEX_CTRL));
    }
    
    CDPathProperties getCDPathProperties(Activity activity) {
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
    
    boolean isCDEnabled() {
        return isCDEnabled_;
    }
    
    int getMaxTextLen() {
        return maxTextLen_;
    }
    
    int getMaxPacketSize() {
        return maxPacketSize_;
    }
    
    int getMaxViewHistorySize() {
        return maxViewHistoryLength_;
    }

    boolean isLocalIndexingEnabled() {
        return localIndexControl.isLocalIndexingEnabled;
    }

    boolean isLocalAutoIndexingEnabled() {
        return localIndexControl.isAutoLocalIndexingEnabled;
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
        private int discoveryRepeatInterval_;
        private int maxDiscoveryRepeat_;
        
        int getDiscoveryRepeatInterval() {
            return discoveryRepeatInterval_;
        }
        
        int getMaxDiscoveryRepeatNumber() {
            return maxDiscoveryRepeat_;
        }

        CDPathProperties(JSONObject pathInfo) {
            pathInfo_ = pathInfo;
            maxDiscoveryRepeat_ = DEF_MAX_DISCOVERY_REPEAT;
            if (pathInfo.has(HASH_MODE_KEY)) {
                try {
                    isClearText_ = !pathInfo.getBoolean(HASH_MODE_KEY);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (pathInfo.has(DISCOVERY_REPEAT_INTERVAL)) {
                    discoveryRepeatInterval_ = pathInfo.getInt(DISCOVERY_REPEAT_INTERVAL);
                }
                if (pathInfo.has(MAX_DISCOVERY_REPEAT)) {
                    maxDiscoveryRepeat_ = pathInfo.getInt(MAX_DISCOVERY_REPEAT);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        JSONArray getFilteredElements() {
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
        
        boolean isClearTextRequested() {
            return isClearText_;
        }
        
        boolean isSkipContentDiscovery() {
            JSONArray filteredElements = getFilteredElements();
            return filteredElements != null && filteredElements.length() == 0;
        }
        
    }


    private static class LocalIndexControl {
        private static final String ENABLE_LOCAL_INDEXING = "enable_li";
        private static final String ENABLE_AUTO_LOCAL_INDEXING = "enable_auto_li";
        private boolean isLocalIndexingEnabled;
        private boolean isAutoLocalIndexingEnabled;

        public LocalIndexControl(JSONObject jsonObject) {
            isLocalIndexingEnabled = true;
            isAutoLocalIndexingEnabled = false;
            isLocalIndexingEnabled = jsonObject != null && jsonObject.optBoolean(ENABLE_LOCAL_INDEXING, true);
            isAutoLocalIndexingEnabled = jsonObject != null && jsonObject.optBoolean(ENABLE_AUTO_LOCAL_INDEXING, false);
        }

    }
}
