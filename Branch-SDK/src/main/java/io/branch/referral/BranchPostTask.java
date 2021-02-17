package io.branch.referral;

import androidx.annotation.NonNull;

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
        PrefHelper.Debug("benas onPostExecute " + thisReq_.getClass().getSimpleName() + ", " + serverResponse.getObject().toString());
        if (latch_ != null) {
            latch_.countDown();
        }
        if (serverResponse == null) return;

        try {
            int status = serverResponse.getStatusCode();
            branch.hasNetwork_ = true;
            if (status == 200) {
                // If the request succeeded
                branch.hasNetwork_ = true;

                if (thisReq_ instanceof ServerRequestCreateUrl) {
                    //On create  new url cache the url.
                    if (serverResponse.getObject() != null) {
                        // cache the link
                        final String url = serverResponse.getObject().getString("url");
                        BranchLinkData postBody = ((ServerRequestCreateUrl) thisReq_).getLinkPost();
                        postBody.remove(Defines.Jsonkey.Metadata.getKey());
                        postBody.remove(Defines.Jsonkey.GoogleAdvertisingID.getKey());
                        postBody.remove(Defines.Jsonkey.LATVal.getKey());
                        postBody.remove(Defines.Jsonkey.AdvertisingIDs.getKey());
                        branch.linkCache_.put(postBody, url);
                    }
                } else if (thisReq_ instanceof ServerRequestLogout) {
                    //On Logout clear the link cache and all pending requests
                    branch.linkCache_.clear();
                    branch.requestQueue_.clear();
                }


                if (thisReq_ instanceof ServerRequestInitSession || thisReq_ instanceof ServerRequestIdentifyUserRequest) {
                    // If this request changes a session update the session-id to queued requests.
                    JSONObject respJson = serverResponse.getObject();
                    if (respJson != null) {
                        boolean updateRequestsInQueue = false;
                        if (!branch.isTrackingDisabled()) { // Update PII data only if tracking is disabled
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
                        }

                        if (updateRequestsInQueue) {
                            branch.updateAllRequestsInQueue();
                        }

                        if (thisReq_ instanceof ServerRequestInitSession) {
                            branch.setInitState(Branch.SESSION_STATE.INITIALISED);
                            thisReq_.onRequestSucceeded(serverResponse, branch);
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
                        } else {
                            // For setting identity just call only request succeeded
                            thisReq_.onRequestSucceeded(serverResponse, branch);
                        }
                    }
                } else {
                    //Publish success to listeners
                    thisReq_.onRequestSucceeded(serverResponse, branch);
                }
            } else {
                //If failed request is an initialisation request then mark session not initialised
                if (thisReq_ instanceof ServerRequestInitSession) {
                    branch.setInitState(Branch.SESSION_STATE.UNINITIALISED);
                }

                // On a bad request or in case of a conflict notify with call back and remove the request.
                if (status == 400 || status == 409 && thisReq_ instanceof ServerRequestCreateUrl) {
                    ((ServerRequestCreateUrl) thisReq_).handleDuplicateURLError();
                } else {
                    //On Network error or Branch is down fail all the pending requests in the queue except
                    //for request which need to be replayed on failure.
                    branch.hasNetwork_ = false;
                    branch.networkCount_ = 0;
                    thisReq_.handleFailure(status, serverResponse.getFailReason());
                }
            }
            branch.requestQueue_.remove(thisReq_);
            branch.networkCount_ = 0;

            if (branch.hasNetwork_ && branch.initState_ != Branch.SESSION_STATE.UNINITIALISED) {
                branch.processNextQueueItem();
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onCancelled(ServerResponse v) {
        super.onCancelled();
        PrefHelper.Debug("benas onCancelled " + thisReq_.getClass().getSimpleName());
        onPostExecute(new ServerResponse(thisReq_.getRequestPath(), ERR_BRANCH_REQ_TIMED_OUT, ""));
    }
}
