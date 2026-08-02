package io.branch.referral;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestGetLATD extends ServerRequest {

    /**
     * @deprecated This listener moved to {@link Branch.BranchLastAttributedTouchDataListener}. This
     * alias is kept only for source compatibility with 5.x integrations that implemented the nested
     * {@code ServerRequestGetLATD.BranchLastAttributedTouchDataListener}; it is <b>not</b> binary
     * compatible, so a prebuilt app must be recompiled. Use {@link Branch.BranchLastAttributedTouchDataListener}
     * directly in new code.
     */
    @Deprecated
    public interface BranchLastAttributedTouchDataListener
            extends Branch.BranchLastAttributedTouchDataListener {
    }

    // defaultAttributionWindow is the "default" for the SDK's side, server interprets it as 30 days
    protected static final int defaultAttributionWindow = -1;
    private int attributionWindow;
    private Branch.BranchLastAttributedTouchDataListener callback;

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath) {
        this(context, requestPath, PrefHelper.getInstance(context).getLATDAttributionWindow());
    }

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath, int attributionWindow) {
        super(context, requestPath);
        this.attributionWindow = attributionWindow;
        JSONObject reqBody = new JSONObject();
        try {
            setPost(reqBody);

            // Cover branch_sdk_request_timestamp / branch_sdk_request_unique_id in the signature.
            addClientRequestParameters();

            // Layer 2 + 3: HMAC signature + smart nonce for LATD requests
            if (Branch.getInstance() != null && Branch.getInstance().getFraudDefenseProvider() != null) {
                try {
                    BranchSecureSDKProvider provider = Branch.getInstance().getFraudDefenseProvider();
                    JSONObject signatureFields = provider.addSignatureAndNonceForParams(getPost());
                    if (signatureFields != null) {
                        SecureContextApplier.apply(signatureFields, getPost());
                        BranchLogger.v("Fraud defense signature fields added to LATD request");
                    }
                } catch (Exception e) {
                    BranchLogger.w("Fraud defense signature failed for LATD: " + e.getMessage());
                }
            }
        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException " + e.getMessage());
        }
        updateEnvironment(context, reqBody);
    }

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath, Branch.BranchLastAttributedTouchDataListener callback) {
        this(context, requestPath);
        this.callback = callback;
    }

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath, Branch.BranchLastAttributedTouchDataListener callback, int attributionWindow) {
        this(context, requestPath, attributionWindow);
        this.callback = callback;
    }

    protected int getAttributionWindow() {
        return attributionWindow;
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        if (callback != null) {
            callback.onDataFetched(response.getObject(), null);
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback != null) {
            callback.onDataFetched(null, new BranchError(causeMsg, statusCode));
        }
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        callback = null;
    }

    @Override
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V1_LATD;
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }
}
