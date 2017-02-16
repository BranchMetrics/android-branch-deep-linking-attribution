package io.branch.referral;

import android.content.Context;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchEvent;

/**
 * * <p>
 * The server request for registering a view event of a specific content specified by user given attributes
 * </p>
 */
class ServerRequestRegisterView extends ServerRequest {

    BranchUniversalObject.RegisterViewStatusListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestRegisterView} to notify Branch on a content view event.</p>
     *
     * @param branchUniversalObject An instance of {@link BranchUniversalObject} to mar as viewed
     */
    public ServerRequestRegisterView(Context context, BranchUniversalObject branchUniversalObject, SystemObserver sysObserver, BranchUniversalObject.RegisterViewStatusListener callback) {
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
            callback_.onRegisterViewFinished(true, null);
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            callback_.onRegisterViewFinished(false, new BranchError("Unable to register content view. " + causeMsg, statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
                callback_.onRegisterViewFinished(false, new BranchError("Unable to register content view", BranchError.ERR_NO_INTERNET_PERMISSION));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
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

        contentObject.put(Defines.Jsonkey.SessionID.getKey(), sessionID);
        contentObject.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());

        String uniqueId;
        if (DeviceInfo.getInstance() != null) {
            uniqueId = DeviceInfo.getInstance().getHardwareID();
        } else {
            uniqueId = sysObserver.getUniqueID(prefHelper_.getExternDebug());
        }
        if (!uniqueId.equals(SystemObserver.BLANK) && sysObserver.hasRealHardwareId()) {
            contentObject.put(Defines.Jsonkey.HardwareID.getKey(), uniqueId);
        }

        String appVersion = sysObserver.getAppVersion();
        if (!appVersion.equals(SystemObserver.BLANK)) {
            contentObject.put(Defines.Jsonkey.AppVersion.getKey(), appVersion);
        }

        JSONObject paramsObj = new JSONObject();

        paramsObj.put(Defines.Jsonkey.ContentKeyWords.getKey(), universalObject.getKeywordsJsonArray());
        paramsObj.put(Defines.Jsonkey.PublicallyIndexable.getKey(), universalObject.isPublicallyIndexable());
        if (universalObject.getPrice() > 0) {
            paramsObj.put(BranchEvent.PURCHASE_AMOUNT, universalObject.getPrice());
            paramsObj.put(BranchEvent.PURCHASE_CURRENCY, universalObject.getCurrencyType());
        }

        String canonicalId = universalObject.getCanonicalIdentifier();
        if (canonicalId != null && canonicalId.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.CanonicalIdentifier.getKey(), canonicalId);
        }
        String canonicalUrl = universalObject.getCanonicalUrl();
        if (canonicalUrl != null && canonicalUrl.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.CanonicalUrl.getKey(), canonicalUrl);
        }
        String title = universalObject.getTitle();
        if (title != null && title.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.ContentTitle.getKey(), universalObject.getTitle());
        }
        String desc = universalObject.getDescription();
        if (desc != null && desc.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.ContentDesc.getKey(), desc);
        }
        String imageUrl = universalObject.getImageUrl();
        if (imageUrl != null && imageUrl.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.ContentImgUrl.getKey(), imageUrl);
        }
        String contentType = universalObject.getType();
        if (contentType != null && contentType.trim().length() > 0) {
            paramsObj.put(Defines.Jsonkey.ContentType.getKey(), contentType);
        }
        long expiryTime = universalObject.getExpirationTime();
        if (expiryTime > 0) {
            paramsObj.put(Defines.Jsonkey.ContentExpiryTime.getKey(), universalObject.getExpirationTime());
        }

        contentObject.put(Defines.Jsonkey.Params.getKey(), paramsObj);

        HashMap metaData = universalObject.getMetadata();
        Set extraKeys = metaData.keySet();
        JSONObject metaDataObject = new JSONObject();
        for (Object key : extraKeys) {
            metaDataObject.put((String) key, metaData.get(key));
        }
        contentObject.put(Defines.Jsonkey.Metadata.getKey(), metaDataObject);

        return contentObject;
    }

    @Override
    public boolean isGAdsParamsRequired() {
        return true;
    }
}
