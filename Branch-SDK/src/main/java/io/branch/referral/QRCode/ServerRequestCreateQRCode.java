package io.branch.referral.QRCode;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

public class ServerRequestCreateQRCode extends ServerRequest {

    private JSONObject params_;
    final Defines.RequestPath requestPath_;
    private long queueWaitTime_ = 0;
    private final Context context_;
    private BranchQRCode.BranchQRCodeRequestHandler callback_;

    /**
     * <p>Creates an instance of ServerRequest.</p>
     *  @param requestPath Path to server for this request.
     * @param post        A {@link JSONObject} containing the post data supplied with the current request
     *                    as key-value pairs.
     * @param context     Application context.
     * @param callback A {@link } callback instance that will trigger
     */
    protected ServerRequestCreateQRCode(Defines.RequestPath requestPath, JSONObject post, Context context, BranchQRCode.BranchQRCodeRequestHandler callback) {
        super(Defines.RequestPath.QRCode, post, context);
        context_ = context;
        requestPath_ = requestPath;
        params_ = post;
        callback_ = callback;
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        callback_.onDataReceived(response);
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        Exception e = new Exception("Failed server request: " + statusCode + causeMsg);
        callback_.onFailure(e);
    }

    @Override
    /**
     * Called when request is added to the queue
     */
    public void onRequestQueued() {
        queueWaitTime_ = System.currentTimeMillis();
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }

    @Override
    public boolean prepareExecuteWithoutTracking() {
        return true;
    }
}
