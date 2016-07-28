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
 * Class for representing a Branch content Intelligence Manifest
 * Parses and persist the CI manifest. Manifest is retrieved each time class is instantiated.
 * </p>
 */
class CIManifest {
    private static CIManifest thisInstance_;


    private JSONObject CIManifestObject_;
    private String manifestVersion_;
    private boolean hashContent_ = true;
    private int maxTextLen_ = 0;
    private int maxPacketSize_ = 0;
    private boolean isBncDevice_ = false;
    private boolean isCIEnabled_ = false;
    private JSONArray contentPaths_;

    public static final String MANIFEST_VERSION_KEY = "mv";
    public static final String HASH_MODE_KEY = "h";
    private static final String MANIFEST_KEY = "m";
    private static final String PATH_KEY = "p";
    private static final String ELEMENT_KEY = "e";
    private static final String BRANCH_DEVICE_KEY = "branch_device";
    private static final String MAX_TEXT_LEN_KEY = "mtl";
    private static final String MAX_PACKET_SIZE_KEY = "mps";
    private static final String CONTENT_INTELLIGENCE_KEY = "ci";


    private SharedPreferences sharedPref;
    private final String PREF_KEY = "CI_MANIFEST";

    private CIManifest(Context context) {
        sharedPref = context.getSharedPreferences("BNC_ContentPath_Array", Context.MODE_PRIVATE);
        retrieve(context);
    }

    public static CIManifest getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new CIManifest(context);
        }
        return thisInstance_;
    }

    private void persist() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_KEY, CIManifestObject_.toString()).apply();
    }

    private void retrieve(Context context) {
        String jsonStr = sharedPref.getString(PREF_KEY, null);
        if (jsonStr != null) {
            try {
                CIManifestObject_ = new JSONObject(jsonStr);
                if (CIManifestObject_.has(MANIFEST_VERSION_KEY)) {
                    manifestVersion_ = CIManifestObject_.getString(MANIFEST_VERSION_KEY);
                }
                if (CIManifestObject_.has(MANIFEST_KEY)) {
                    contentPaths_ = CIManifestObject_.getJSONArray(MANIFEST_KEY);
                }
            } catch (JSONException ignored) {
                CIManifestObject_ = new JSONObject();
            }
        } else {
            CIManifestObject_ = new JSONObject();
        }
    }

    public void onBranchInitialised(JSONObject branchInitResp) {
        if (branchInitResp.has(CONTENT_INTELLIGENCE_KEY)) {
            isCIEnabled_ = true;
            try {
                JSONObject CIObj = branchInitResp.getJSONObject(CONTENT_INTELLIGENCE_KEY);
                if (CIObj.has(BRANCH_DEVICE_KEY)) {
                    isBncDevice_ = CIObj.getBoolean(BRANCH_DEVICE_KEY);
                }
                if (CIObj.has(MANIFEST_VERSION_KEY)) {
                    manifestVersion_ = CIObj.getString(MANIFEST_VERSION_KEY);
                }
                if (CIObj.has(HASH_MODE_KEY)) {
                    hashContent_ = CIObj.getBoolean(HASH_MODE_KEY);
                }
                if (CIObj.has(MANIFEST_KEY)) {
                    JSONArray newContentPaths = CIObj.getJSONArray(MANIFEST_KEY);
                    if (contentPaths_ == null) {
                        contentPaths_ = new JSONArray();
                        for (int i = 0; i < newContentPaths.length(); i++) {
                            contentPaths_.put(newContentPaths.getJSONObject(i));
                        }
                    }
                }
                if (CIObj.has(MAX_TEXT_LEN_KEY)) {
                    maxTextLen_ = CIObj.getInt(MAX_TEXT_LEN_KEY);
                }
                if (CIObj.has(MAX_PACKET_SIZE_KEY)) {
                    maxPacketSize_ = CIObj.getInt(MAX_PACKET_SIZE_KEY);
                }
                CIManifestObject_.put(MANIFEST_VERSION_KEY, manifestVersion_);
                CIManifestObject_.put(MANIFEST_KEY, contentPaths_);
                persist();
            } catch (JSONException ignore) {

            }
        } else {
            isCIEnabled_ = false;
        }
    }

    public CIPathProperties getCIPathProperties(Activity activity) {
        CIPathProperties pathProperties = null;
        if (contentPaths_ != null) {
            String viewPath = "/" + activity.getClass().getSimpleName();
            try {
                for (int i = 0; i < contentPaths_.length(); i++) {
                    JSONObject pathObj = contentPaths_.getJSONObject(i);
                    if (pathObj.has(PATH_KEY) && pathObj.getString(PATH_KEY).equals(viewPath)) {
                        pathProperties = new CIPathProperties(pathObj);
                        break;
                    }
                }
            } catch (JSONException ignore) {

            }
        }
        return pathProperties;
    }

    public boolean isCIEnabled() {
        return isCIEnabled_;
    }

    public boolean isClearTextRequested() {
        return hashContent_;
    }

    public boolean isBncDevice() {
        return isBncDevice_;
    }

    public int getMaxTextLen() {
        return maxTextLen_;
    }

    public int getMaxPacketSize() {
        return maxPacketSize_;
    }

    class CIPathProperties extends JSONObject {
        final JSONObject pathInfo_;

        CIPathProperties(JSONObject pathInfo) {
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

    public String getManifestVersion() {
        return manifestVersion_;
    }
}
