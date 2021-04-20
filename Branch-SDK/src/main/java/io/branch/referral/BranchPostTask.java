package io.branch.referral;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import static io.branch.referral.BranchError.ERR_BRANCH_REQ_TIMED_OUT;

/**
 * Asynchronous task handling execution of server requests. Execute the network task on background
 * thread and request are  executed in sequential manner. Handles the request execution in
 * Synchronous-Asynchronous pattern. Should be invoked only form main thread and  the results are
 * published in the main thread.
 */
public class BranchPostTask extends BranchAsyncTask<Void, Void, ServerResponse> {
    ServerRequest thisReq_;
    final CountDownLatch latch_;
    @NonNull private final Branch branch;

    public BranchPostTask(@NonNull Branch branch, ServerRequest request, CountDownLatch latch) {
        super();
        this.branch = branch;
        thisReq_ = request;
        latch_ = latch;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        thisReq_.onPreExecute();
        thisReq_.doFinalUpdateOnMainThread();
    }

    @Override
    protected ServerResponse doInBackground(Void... voids) {
        // update queue wait time
        branch.addExtraInstrumentationData(thisReq_.getRequestPath() + "-" + Defines.Jsonkey.Queue_Wait_Time.getKey(), String.valueOf(thisReq_.getQueueWaitTime()));
        thisReq_.doFinalUpdateOnBackgroundThread();
        if (branch.isTrackingDisabled() && !thisReq_.prepareExecuteWithoutTracking()) {
            return new ServerResponse(thisReq_.getRequestPath(), BranchError.ERR_BRANCH_TRACKING_DISABLED, "");
        }
        String branchKey = branch.prefHelper_.getBranchKey();
        ServerResponse result;
        if (thisReq_.isGetRequest()) {
            result = branch.getBranchRemoteInterface().make_restful_get(thisReq_.getRequestUrl(), thisReq_.getGetParams(), thisReq_.getRequestPath(), branchKey);
        } else {
            result = branch.getBranchRemoteInterface().make_restful_post(thisReq_.getPostWithInstrumentationValues(branch.instrumentationExtraData_), thisReq_.getRequestUrl(), thisReq_.getRequestPath(), branchKey);
        }
        if (latch_ != null) {
            latch_.countDown();
        }
        return result;
    }

    @Override
    protected void onPostExecute(ServerResponse serverResponse) {
        super.onPostExecute(serverResponse);
        if (latch_ != null) {
            latch_.countDown();
        }
        if (serverResponse == null) {
            thisReq_.handleFailure(BranchError.ERR_BRANCH_INVALID_REQUEST, "Null response.");
            return;
        }

        int status = serverResponse.getStatusCode();
        if (status == 200) {
            onRequestSuccess(serverResponse);
        } else {
            onRequestFailed(serverResponse, status);
        }
        branch.networkCount_ = 0;

        branch.processNextQueueItem();
    }

    private void onRequestSuccess(ServerResponse serverResponse) {
        // If the request succeeded
        @Nullable final JSONObject respJson = serverResponse.getObject();
        if (respJson == null) {
            thisReq_.handleFailure(500, "Null response json.");
        }

        if (thisReq_ instanceof ServerRequestCreateUrl && respJson != null) {
            try {
                // cache the link
                BranchLinkData postBody = ((ServerRequestCreateUrl) thisReq_).getLinkPost();
                final String url = respJson.getString("url");
                branch.linkCache_.put(postBody, url);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        } else if (thisReq_ instanceof ServerRequestLogout) {
            //On Logout clear the link cache and all pending requests
            branch.linkCache_.clear();
            branch.requestQueue_.clear();
        }


        if (thisReq_ instanceof ServerRequestInitSession || thisReq_ instanceof ServerRequestIdentifyUserRequest) {
            // If this request changes a session update the session-id to queued requests.
            boolean updateRequestsInQueue = false;
            if (!branch.isTrackingDisabled() && respJson != null) {
                // Update PII data only if tracking is disabled
                try {
                    if (respJson.has(Defines.Jsonkey.SessionID.getKey())) {
                        branch.prefHelper_.setSessionID(respJson.getString(Defines.Jsonkey.SessionID.getKey()));
                        updateRequestsInQueue = true;
                    }
                    if (respJson.has(Defines.Jsonkey.IdentityID.getKey())) {
                        String new_Identity_Id = respJson.getString(Defines.Jsonkey.IdentityID.getKey());
                        if (!branch.prefHelper_.getIdentityID().equals(new_Identity_Id)) {
                            //On setting a new identity Id clear the link cache
                            branch.linkCache_.clear();
                            branch.prefHelper_.setIdentityID(new_Identity_Id);
                            updateRequestsInQueue = true;
                        }
                    }
                    if (respJson.has(Defines.Jsonkey.DeviceFingerprintID.getKey())) {
                        branch.prefHelper_.setDeviceFingerPrintID(respJson.getString(Defines.Jsonkey.DeviceFingerprintID.getKey()));
                        updateRequestsInQueue = true;
                    }
                    if (updateRequestsInQueue) {
                        branch.updateAllRequestsInQueue();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            if (thisReq_ instanceof ServerRequestInitSession) {
                branch.setInitState(Branch.SESSION_STATE.INITIALISED);
                if (!((ServerRequestInitSession) thisReq_).handleBranchViewIfAvailable((serverResponse))) {
                    branch.checkForAutoDeepLinkConfiguration();
                }
                // Count down the latch holding getLatestReferringParamsSync
                if (branch.getLatestReferringParamsLatch != null) {
                    branch.getLatestReferringParamsLatch.countDown();
                }
                // Count down the latch holding getFirstReferringParamsSync
                if (branch.getFirstReferringParamsLatch != null) {
                    branch.getFirstReferringParamsLatch.countDown();
                }
            }
        }

        if (respJson != null) {
            thisReq_.onRequestSucceeded(serverResponse, branch);
            branch.requestQueue_.remove(thisReq_);
        } else if (thisReq_.shouldRetryOnFail()) {
            // already called handleFailure above
            thisReq_.clearCallbacks();
        } else {
            branch.requestQueue_.remove(thisReq_);
        }
    }

    private void onRequestFailed(ServerResponse serverResponse, int status) {
        // If failed request is an initialisation request (but not in the intra-app linking scenario) then mark session as not initialised
        if (thisReq_ instanceof ServerRequestInitSession && PrefHelper.NO_STRING_VALUE.equals(branch.prefHelper_.getSessionParams())) {
            branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
        }

        // On a bad request or in case of a conflict notify with call back and remove the request.
        if ((status == 400 || status == 409) && thisReq_ instanceof ServerRequestCreateUrl) {
            ((ServerRequestCreateUrl) thisReq_).handleDuplicateURLError();
        } else {
            //On Network error or Branch is down fail all the pending requests in the queue except
            //for request which need to be replayed on failure.
            branch.networkCount_ = 0;
            thisReq_.handleFailure(status, serverResponse.getFailReason());
        }

        if (status == 400 || !thisReq_.shouldRetryOnFail()) {
            branch.requestQueue_.remove(thisReq_);
        } else {
            // failure has already been handled
            // todo does it make sense to retry the request without a callback? (e.g. CPID, LATD)
            thisReq_.clearCallbacks();
        }
    }

    @Override
    protected void onCancelled(ServerResponse v) {
        super.onCancelled();
        onPostExecute(new ServerResponse(thisReq_.getRequestPath(), ERR_BRANCH_REQ_TIMED_OUT, ""));
    }
}
