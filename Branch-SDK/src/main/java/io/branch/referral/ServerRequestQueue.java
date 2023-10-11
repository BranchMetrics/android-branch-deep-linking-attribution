package io.branch.referral;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import io.branch.channels.ServerRequestChannelKt;
import io.branch.data.ServerRequestResponsePair;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * <p>The Branch SDK can queue up requests whilst it is waiting for initialization of a session to
 * complete. This allows you to start sending requests to the Branch API as soon as your app is
 * opened.</p>
 */
public class ServerRequestQueue {
    private static final String PREF_KEY = "BNCServerRequestQueue";
    private static final int MAX_ITEMS = 25;
    private static volatile ServerRequestQueue SharedInstance;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    public final List<ServerRequest> queue;
    //Object for synchronising operations on server request queue
    private static final Object reqQueueLockObject = new Object();

    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link ServerRequestQueue}.</p>
     *
     * @param c A {@link Context} from which this call was made.
     * @return An initialised {@link ServerRequestQueue} object, either fetched from a
     * pre-initialised instance within the singleton class, or a newly instantiated
     * object where one was not already requested during the current app lifecycle.
     */
    public static ServerRequestQueue getInstance(Context c) {
        if (SharedInstance == null) {
            synchronized (ServerRequestQueue.class) {
                if (SharedInstance == null) {
                    SharedInstance = new ServerRequestQueue(c);
                }
            }
        }
        return SharedInstance;
    }

    // Package Private
    static void shutDown() {
        synchronized (reqQueueLockObject) {
            SharedInstance = null;
        }
    }
    
    /**
     * <p>The main constructor of the ServerRequestQueue class is private because the class uses the
     * Singleton pattern.</p>
     *
     * @param c A {@link Context} from which this call was made.
     */
    @SuppressLint("CommitPrefEdits")
    private ServerRequestQueue(Context c) {
        sharedPref = c.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        queue = retrieve(c);
    }
    
    private void persist(String s) {
        BranchLogger.v("ServerRequestQueue persist begin from " + s);
        try {
            JSONArray jsonArr = new JSONArray();
            synchronized (reqQueueLockObject) {
                for (ServerRequest aQueue : queue) {
                    if (aQueue.isPersistable()) {
                        JSONObject json = aQueue.toJSON();
                        if (json != null) {
                            jsonArr.put(json);
                        }
                    }
                }
            }

            //BranchLogger.v("ServerRequestQueue persist " + jsonArr);
            BranchLogger.v("ServerRequestQueue persist " + Arrays.toString(queue.toArray()));
            editor.putString(PREF_KEY, jsonArr.toString()).apply();
        }
        catch (Exception ex) {
            String msg = ex.getMessage();

            BranchLogger.v("Failed to persist queue " + ex + " "+ (msg == null ? "" : msg));
        }
        BranchLogger.v("ServerRequestQueue persist end");
    }
    
    private List<ServerRequest> retrieve(Context context) {
        String jsonStr = sharedPref.getString(PREF_KEY, null);
        List<ServerRequest> result = Collections.synchronizedList(new LinkedList<ServerRequest>());
        synchronized (reqQueueLockObject) {
            if (jsonStr != null) {
                try {
                    JSONArray jsonArr = new JSONArray(jsonStr);
                    for (int i = 0, size = Math.min(jsonArr.length(), MAX_ITEMS); i < size; i++) {
                        JSONObject json = jsonArr.getJSONObject(i);
                        ServerRequest req = ServerRequest.fromJSON(json, context);
                        if (req != null) {
                            result.add(req);
                        }
                    }
                }
                catch (JSONException e) {
                    BranchLogger.d(e.getMessage());
                }
            }
        }

        BranchLogger.v("ServerRequestQueue retrieve " + result);
        return result;
    }
    
    /**
     * <p>Gets the number of {@link ServerRequest} objects currently queued up for submission to
     * the Branch API.</p>
     *
     * @return An {@link Integer} value indicating the current size of the {@link List} object
     * that forms the logical queue for the class.
     */
    public int getSize() {
        synchronized (reqQueueLockObject) {
            return queue.size();
        }
    }
    
    /**
     * <p>Adds a {@link ServerRequest} object to the queue.</p>
     *
     * @param request The {@link ServerRequest} object to add to the queue.
     */
    void enqueue(ServerRequest request) {
        BranchLogger.v("ServerRequestQueue enqueue " + request);
        synchronized (reqQueueLockObject) {
            if (request != null) {
                queue.add(request);
                if (getSize() >= MAX_ITEMS) {
                    queue.remove(1);
                }
                persist("enqueue " + request);
            }
        }
    }
    
    /**
     * <p>Gets the queued {@link ServerRequest} object at position with index specified in the supplied
     * parameter, within the queue. The item is not removed from the queue.</p>
     *
     * @param index An {@link Integer} that specifies the position within the queue from which to
     *              pull the {@link ServerRequest} object.
     * @return The {@link ServerRequest} object at the specified index. Returns null if no
     * request exists at that position, or if the index supplied is not valid, for
     * instance if {@link #getSize()} is 6 and index 6 is called.
     */
    ServerRequest peekAt(int index) {
        ServerRequest req = null;
        synchronized (reqQueueLockObject) {
            try {
                req = queue.get(index);
                BranchLogger.v("ServerRequestQueue peakAt " + index + " returned " + req);
            } catch (IndexOutOfBoundsException | NoSuchElementException e) {
                BranchLogger.d(e.getMessage());
            }
        }
        return req;
    }

    /**
     * <p>As the method name implies, removes {@link ServerRequest} supplied in the parameter if it
     * is present in the queue.</p>
     *
     * @param request The {@link ServerRequest} object to be removed from the queue.
     * @return A {@link Boolean} whose value is true if the object is removed.
     */
    public boolean remove(ServerRequest request) {
        boolean isRemoved = false;
        synchronized (reqQueueLockObject) {
            try {
                isRemoved = queue.remove(request);
                BranchLogger.v("ServerRequestQueue removed " + request + " " + isRemoved);
                persist("remove " + request);
            } catch (UnsupportedOperationException e) {
                BranchLogger.d(e.getMessage());
            }
        }
        return isRemoved;
    }

    /**
     * If the request is currently in the queue, remove it from that position
     * and insert at destination to prevent duplicates
     * Else, just insert at that position
     * @param request
     * @param destination
     */
    public void move(ServerRequest request, int destination){
        BranchLogger.v("ServerRequestQueue move " + request + " " + destination);
        synchronized (reqQueueLockObject) {
            try {
                int index = -1;
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i) == request) {
                        index = i;
                        break;
                    }
                }

                // We found the request we want to move in the queue
                if (index > -1) {
                    queue.remove(index);
                }

                queue.add(destination, request);

                persist("move " + request);
            }
            catch (Exception e) {
                BranchLogger.v(e.getMessage());
            }
        }
    }
    
    /**
     * <p> Clears all pending requests in the queue </p>
     */
    void clear() {
        BranchLogger.v("ServerRequestQueue clear");
        synchronized (reqQueueLockObject) {
            try {
                queue.clear();
                persist("clear");
            } catch (UnsupportedOperationException e) {
                BranchLogger.d(e.getMessage());
            }
        }
    }
    
    /**
     * <p>Determines whether the queue contains an install/register request.</p>
     *
     * @return A {@link Boolean} value indicating whether or not the queue contains an
     * install/register request. <i>True</i> if the queue contains a close request,
     * <i>False</i> if not.
     */
    ServerRequestInitSession getSelfInitRequest() {
        synchronized (reqQueueLockObject) {
            for (ServerRequest req : queue) {
                if (req instanceof ServerRequestInitSession) {
                    ServerRequestInitSession r = (ServerRequestInitSession) req;
                    if (r.initiatedByClient) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    // Determine if a Request needs a Session to proceed.
    private boolean requestNeedsSession(ServerRequest request) {
        if (request instanceof ServerRequestInitSession) {
            return false;
        }
        else if (request instanceof ServerRequestCreateUrl) {
            return false;
        }

        // All other Request Types need a session.
        return true;
    }

    // Determine if a Session is available for a Request to proceed.
    //todo, check for this in handlenewrequest
    private boolean isSessionAvailableForRequest() {
        return (hasSession() && hasRandomizedDeviceToken());
    }

    private boolean hasSession() {
        return !Branch.getInstance().prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE);
    }

    private boolean hasRandomizedDeviceToken() {
        return !Branch.getInstance().prefHelper_.getRandomizedDeviceToken().equals(PrefHelper.NO_STRING_VALUE);
    }

    boolean hasUser() {
        return !Branch.getInstance().prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE);
    }

    void updateAllRequestsInQueue() {
        BranchLogger.v("ServerRequestQueue updateAllRequestsInQueue");
        try {
            for (int i = 0; i < this.getSize(); i++) {
                ServerRequest req = this.peekAt(i);
                if (req != null) {
                    JSONObject reqJson = req.getPost();
                    if (reqJson != null) {
                        if (reqJson.has(Defines.Jsonkey.SessionID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.SessionID.getKey(), Branch.getInstance().prefHelper_.getSessionID());
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedBundleToken.getKey())) {
                            req.getPost().put(Defines.Jsonkey.RandomizedBundleToken.getKey(), Branch.getInstance().prefHelper_.getRandomizedBundleToken());
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedDeviceToken.getKey())) {
                            req.getPost().put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), Branch.getInstance().prefHelper_.getRandomizedDeviceToken());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        BranchLogger.v("ServerRequestQueue updateAllRequestsInQueue end");
    }

    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The {@link ServerRequest} to execute
     */
    public void handleNewRequest(ServerRequest req) {
        BranchLogger.d("ServerRequestQueue handleNewRequest " + req + " on thread " + Thread.currentThread().getName());
        // If Tracking is disabled fail all messages with ERR_BRANCH_TRACKING_DISABLED
        if (Branch.getInstance().getTrackingController().isTrackingDisabled() && !req.prepareExecuteWithoutTracking()) {
            BranchLogger.d("Requested operation cannot be completed since tracking is disabled [" + req.requestPath_.getPath() + "]");

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                 @Override
                 public void run() {
                     req.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, "");
                 }
             });
            return;
        }

        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (Branch.getInstance().initState_ != Branch.SESSION_STATE.INITIALISED && !(req instanceof ServerRequestInitSession)) {
            if (requestNeedsSession(req)) {
                BranchLogger.d("handleNewRequest " + req + " needs a session");
            }
        }

        // If the request we are about to handle is an init
        // Move it to the front, and persist state
        // Then go through the queue in order
        if(req instanceof ServerRequestInitSession){
            move(req, 0);
            processQueue();
            return;
        }

        // Add to the queue and persist the state locally
        enqueue(req);

        // If the session is already initialized,
        // Or, it doesn't need a session
        // Then execute immediately
        if(Branch.getInstance().initState_ == Branch.SESSION_STATE.INITIALISED
        || !requestNeedsSession(req)){
            executeRequest(req);
        }
    }

    /**
     * Process all recorded ServerRequests now that the session is ready
     */
    public void processQueue(){
        BranchLogger.v("ServerRequestQueue processQueue " + Arrays.toString(queue.toArray()));
        synchronized (reqQueueLockObject) {
            for (ServerRequest req : queue) {
                executeRequest(req);
            }
        }
    }

    private void executeRequest(ServerRequest req) {
        ServerRequestChannelKt.execute(req, new Continuation<ServerRequestResponsePair>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                BranchLogger.v("ServerRequestQueue executeRequest resumeWith " + o + " " + Thread.currentThread().getName());

                if (o != null && o instanceof ServerRequestResponsePair) {
                    ServerRequestResponsePair serverRequestResponsePair = (ServerRequestResponsePair) o;

                    ServerRequest serverRequest = serverRequestResponsePair.getServerRequest();
                    ServerResponse serverResponse = serverRequestResponsePair.getServerResponse();

                    ServerRequestQueue.this.onRequestComplete(serverRequest, serverResponse);
                }
                else {
                    BranchLogger.v("ServerRequestQueue expected ServerRequestResponsePair, was " + o);
                    if(o instanceof Result.Failure){
                        BranchLogger.v("logging stack");
                        Result.Failure failure = (Result.Failure) o;
                        BranchLogger.v(Arrays.toString(failure.exception.getStackTrace()));
                    }
                }
            }
        });
    }

    /**
     * Method for handling the completion of a request, success or failure
     * @param serverRequest
     * @param serverResponse
     */
    public void onRequestComplete(ServerRequest serverRequest, ServerResponse serverResponse) {
        BranchLogger.v("ServerRequestQueue onRequestComplete " + serverRequest + " " + serverResponse + " on thread " + Thread.currentThread().getName());
        if (serverResponse == null) {
            serverRequest.handleFailure(BranchError.ERR_BRANCH_INVALID_REQUEST, "Null response.");
        }
        else {
            @Nullable final JSONObject respJson = serverResponse.getObject();

            if (respJson == null) {
                serverRequest.handleFailure(500, "Null response json.");
            }
            else {
                int status = serverResponse.getStatusCode();

                if (status == 200) {
                    serverRequest.onRequestSucceeded(serverResponse, Branch.getInstance());
                }
                // We received an error code from the service
                else {
                    // On a bad request or in case of a conflict notify with call back and remove the request.
                    if ((status == 400 || status == 409) && serverRequest instanceof ServerRequestCreateUrl) {
                        ((ServerRequestCreateUrl) serverRequest).handleDuplicateURLError();
                    }
                    else {
                        serverRequest.handleFailure(status, serverResponse.getFailReason());
                    }
                }
            }
        }
    }

    public void onPostExecuteInner(ServerRequest serverRequest, ServerResponse serverResponse) {
        BranchLogger.v("ServerRequestQueue onPostExecuteInner " + serverRequest + " " + serverResponse);

        if(serverResponse != null) {
            int status = serverResponse.getStatusCode();

            // These write our stateful results from our service's response
            if (status == 200) {
                onRequestSuccessInternal(serverRequest, serverResponse);
            }
            else {
                onRequestFailedInternal(serverRequest, serverResponse, status);
            }

            ServerRequestQueue.this.remove(serverRequest);
        }
    }

    private void onRequestSuccessInternal(ServerRequest serverRequest, ServerResponse serverResponse) {
        BranchLogger.v("ServerRequestQueue onRequestSuccess " + serverRequest + " " + serverResponse);
        @Nullable final JSONObject respJson = serverResponse.getObject();

        if (serverRequest instanceof ServerRequestCreateUrl && respJson != null) {
            try {
                // cache the link
                BranchLinkData postBody = ((ServerRequestCreateUrl) serverRequest).getLinkPost();
                final String url = respJson.getString("url");
                Branch.getInstance().linkCache_.put(postBody, url);
            }
            catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        if (serverRequest instanceof ServerRequestInitSession || serverRequest instanceof ServerRequestIdentifyUserRequest) {
            // If this request changes a session update the session-id to queued requests.
            boolean updateRequestsInQueue = false;
            if (!Branch.getInstance().isTrackingDisabled() && respJson != null) {
                // Update PII data only if tracking is enabled
                try {
                    if (respJson.has(Defines.Jsonkey.SessionID.getKey())) {
                        Branch.getInstance().prefHelper_.setSessionID(respJson.getString(Defines.Jsonkey.SessionID.getKey()));
                        updateRequestsInQueue = true;
                    }
                    if (respJson.has(Defines.Jsonkey.RandomizedBundleToken.getKey())) {
                        String new_Randomized_Bundle_Token = respJson.getString(Defines.Jsonkey.RandomizedBundleToken.getKey());
                        if (!Branch.getInstance().prefHelper_.getRandomizedBundleToken().equals(new_Randomized_Bundle_Token)) {
                            //On setting a new Randomized Bundle Token clear the link cache
                            Branch.getInstance().linkCache_.clear();
                            Branch.getInstance().prefHelper_.setRandomizedBundleToken(new_Randomized_Bundle_Token);
                            updateRequestsInQueue = true;
                        }
                    }
                    if (respJson.has(Defines.Jsonkey.RandomizedDeviceToken.getKey())) {
                        Branch.getInstance().prefHelper_.setRandomizedDeviceToken(respJson.getString(Defines.Jsonkey.RandomizedDeviceToken.getKey()));
                        updateRequestsInQueue = true;
                    }
                    if (updateRequestsInQueue) {
                        updateAllRequestsInQueue();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            if (serverRequest instanceof ServerRequestInitSession) {
                Branch.getInstance().setInitState(Branch.SESSION_STATE.INITIALISED);

                // Count down the latch holding getLatestReferringParamsSync
                if (Branch.getInstance().getLatestReferringParamsLatch != null) {
                    Branch.getInstance().getLatestReferringParamsLatch.countDown();
                }
                // Count down the latch holding getFirstReferringParamsSync
                if (Branch.getInstance().getFirstReferringParamsLatch != null) {
                    Branch.getInstance().getFirstReferringParamsLatch.countDown();
                }
            }
        }
    }

    //TODO, refactor this
    void onRequestFailedInternal(ServerRequest serverRequest, ServerResponse serverResponse, int status) {
        BranchLogger.v("ServerRequestQueue onRequestFailed " + serverRequest + " " + serverResponse);
        // If failed request is an initialisation request (but not in the intra-app linking scenario) then mark session as not initialised
        if (serverRequest instanceof ServerRequestInitSession && PrefHelper.NO_STRING_VALUE.equals(Branch.getInstance().prefHelper_.getSessionParams())) {
            Branch.getInstance().setInitState(Branch.SESSION_STATE.UNINITIALISED);
        }

        boolean unretryableErrorCode = (400 <= status && status <= 451) || status == BranchError.ERR_BRANCH_TRACKING_DISABLED;
        // If it has an un-retryable error code, or it should not retry on fail, or the current retry count exceeds the max
        // remove it from the queue
        if (unretryableErrorCode || !serverRequest.shouldRetryOnFail() || (serverRequest.currentRetryCount >= Branch.getInstance().prefHelper_.getNoConnectionRetryMax())) {
            Branch.getInstance().requestQueue_.remove(serverRequest);
        } else {
            // failure has already been handled
            serverRequest.clearCallbacks();
        }
    }
}
