package io.branch.referral;

import android.content.Context;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import io.branch.referral.indexing.BranchUniversalObject;

/**
 * * <p>
 * The server request for registering a view event of a specific content specified by user given attributes
 * </p>
 */
class ServerRequestRegisterView extends ServerRequest {

    BranchUniversalObject.MarkViewStatusListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestRegisterView} to notify Branch on a content view event.</p>
     *
     * @param branchUniversalObject An instance of {@link BranchUniversalObject} to mar as viewed
     */
    public ServerRequestRegisterView(Context context, BranchUniversalObject branchUniversalObject, SystemObserver sysObserver, BranchUniversalObject.MarkViewStatusListener callback) {
        super(context, Defines.RequestPath.RegisterView.getPath());
        callback_ = callback;
        JSONObject registerViewPost;
        try {
            registerViewPost = createContentViewJson(branchUniversalObject, sysObserver);
            setPost(registerViewPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        if (callback_ != null) {
            callback_.onMarkViewFinished(true, null);
        }
    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            callback_.onMarkViewFinished(false, new BranchError("Unable to register content view", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onMarkViewFinished(false, new BranchError("Unable to register content view", BranchError.ERR_NO_INTERNET_PERMISSION));
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public boolean isUpdateGAParams() {
        return true; // Since register view needs GA params
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }

    @Override
    public String getRequestUrl() {
        return "http://54.153.121.111:5000/v0.1/register_view/";
    }

    /**
     * Creates a Json with given parameters for register view.
     *
     * @param universalObject An instance of {@link BranchUniversalObject} to create the Json
     * @return A {@link JSONObject} for post data for register view request
     * @throws JSONException {@link JSONException} on any Json errors
     */
    private JSONObject createContentViewJson(BranchUniversalObject universalObject,
                                             SystemObserver sysObserver) throws JSONException {

        JSONObject contentObject = new JSONObject();


        String os_Info = "Android " + Build.VERSION.SDK_INT;
        String sessionID = prefHelper_.getSessionID();

        contentObject.put(Defines.Jsonkey.OS.getKey(), os_Info);
        contentObject.put(Defines.Jsonkey.SessionID.getKey(), sessionID);

        String uniqueId = sysObserver.getUniqueID(prefHelper_.getExternDebug());
        if (!uniqueId.equals(SystemObserver.BLANK)) {
            contentObject.put(Defines.Jsonkey.HardwareID.getKey(), uniqueId);
        }
        contentObject.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());

        JSONObject paramsObj = new JSONObject();

        paramsObj.put(Defines.Jsonkey.ContentTitle.getKey(), universalObject.getTitle());
        paramsObj.put(Defines.Jsonkey.CanonicalIdentifier.getKey(), universalObject.getCanonicalIdentifier());
        paramsObj.put(Defines.Jsonkey.ContentKeyWords.getKey(), universalObject.getKeyWords());
        paramsObj.put(Defines.Jsonkey.PublicallyIndexable.getKey(), universalObject.isPublicallyIndexable());

        String desc = universalObject.getDescription();
        if(desc != null && desc.trim().length() > 0 ) {
            paramsObj.put(Defines.Jsonkey.ContentDesc.getKey(), desc);
        }
        String imageUrl = universalObject.getImageUrl();
        if(imageUrl != null && imageUrl.trim().length() > 0 ) {
            paramsObj.put(Defines.Jsonkey.ContentImgUrl.getKey(), imageUrl);
        }
        String contentType = universalObject.getType();
        if(contentType != null && contentType.trim().length() > 0 ) {
            paramsObj.put(Defines.Jsonkey.ContentType.getKey(), contentType);
        }
        long expiryTime = universalObject.getExpirationTime();
        if(expiryTime > 0) {
            paramsObj.put(Defines.Jsonkey.ContentExpiryTime.getKey(), universalObject.getExpirationTime());
        }

        contentObject.put(Defines.Jsonkey.Params.getKey(), paramsObj);

        HashMap metaData = universalObject.getMetaData();
        Set extraKeys = metaData.keySet();
        JSONObject metaDataObject = new JSONObject();
        for (Object key : extraKeys) {
            metaDataObject.put((String) key, metaData.get(key));
        }
        contentObject.put(Defines.Jsonkey.Metadata.getKey(), metaDataObject);

        return contentObject;
    }
}
