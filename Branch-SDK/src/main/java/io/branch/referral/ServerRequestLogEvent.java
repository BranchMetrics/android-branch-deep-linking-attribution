package io.branch.referral;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;

/**
 * * <p>
 * The server request for logging events. Handles request creation and execution.
 * </p>
 */
public class ServerRequestLogEvent extends ServerRequest {

    public ServerRequestLogEvent(Context context, Defines.RequestPath requestPath, final String eventName,
                          final HashMap<String, Object> topLevelProperties, final JSONObject standardProperties,
                                 final JSONObject customProperties, final List<BranchUniversalObject> buoList) {
        super(context, requestPath);
        JSONObject reqBody = new JSONObject();
        try {
            reqBody.put(Defines.Jsonkey.Name.getKey(), eventName);
            if (customProperties.length() > 0) {
                reqBody.put(Defines.Jsonkey.CustomData.getKey(), customProperties);
            }

            if (standardProperties.length() > 0) {
                reqBody.put(Defines.Jsonkey.EventData.getKey(), standardProperties);
            }

            if (topLevelProperties.size() > 0) {
                for (Map.Entry<String, Object> entry : topLevelProperties.entrySet()) {
                    reqBody.put(entry.getKey(), entry.getValue());
                }
            }

            if (buoList.size() > 0) {
                JSONArray contentItemsArray = new JSONArray();
                reqBody.put(Defines.Jsonkey.ContentItems.getKey(), contentItemsArray);
                for (BranchUniversalObject buo : buoList) {
                    contentItemsArray.put(buo.convertToJson());
                }
            }
            setPost(reqBody);
        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException " + e.getMessage());
        }
        updateEnvironment(context, reqBody);
    }

    @Override
    protected void setPost(JSONObject post) throws JSONException {
        super.setPost(post);
        prefHelper_.loadPartnerParams(post);
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
    }

    @Override
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V2; //This is a v2 event
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }

    public boolean shouldRetryOnFail() {
        return true; // Branch event need to be retried on failure.
    }
}
