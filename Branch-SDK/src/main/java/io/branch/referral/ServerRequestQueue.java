package io.branch.referral;

import static io.branch.referral.BranchError.ERR_BRANCH_TASK_TIMEOUT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * <p>The Branch SDK can queue up requests whilst it is waiting for initialization of a session to
 * complete. This allows you to start sending requests to the Branch API as soon as your app is
 * opened.</p>
 */
public class ServerRequestQueue {
    private static final String PREF_KEY = "BNCServerRequestQueue";
    private static final int MAX_ITEMS = 25;
    private static ServerRequestQueue SharedInstance;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private final List<ServerRequest> queue;
    //Object for synchronising operations on server request queue
    private static final Object reqQueueLockObject = new Object();

    private final Semaphore serverSema_ = new Semaphore(1);

    int networkCount_ = 0;

    final ConcurrentHashMap<String, String> instrumentationExtraData_ = new ConcurrentHashMap<>();

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
        BranchLogger.v("Creating ServerRequestQueue " + c);
        sharedPref = c.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        queue = Collections.synchronizedList(new LinkedList<ServerRequest>());
        BranchLogger.v("Created queue " + queue);
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
        synchronized (reqQueueLockObject) {
            BranchLogger.v("Queue operation enqueue. Request: " + request);
            if (request != null) {
                queue.add(request);
                if (getSize() >= MAX_ITEMS) {
                    BranchLogger.v("Queue maxed out. Removing index 1.");
                    queue.remove(1);
                }
            }
        }
    }
    
    /**
     * <p>Gets the queued {@link ServerRequest} object at position with index 0 within the queue
     * without removing it.</p>
     *
     * @return The {@link ServerRequest} object at position with index 0 within the queue.
     */
    ServerRequest peek() {
        ServerRequest req = null;
        synchronized (reqQueueLockObject) {
            try {
                req = queue.get(0);
            } catch (IndexOutOfBoundsException | NoSuchElementException e) {
                BranchLogger.w("Caught Exception ServerRequestQueue peek: " + e.getMessage());
            }
        }
        return req;
    }

    public void printQueue(){
        // Only print the queue if the log level is verbose
        if (BranchLogger.getLoggingLevel().getLevel() == BranchLogger.BranchLogLevel.VERBOSE.getLevel()) {
            synchronized (reqQueueLockObject) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < queue.size(); i++) {
                    stringBuilder.append(queue.get(i)).append(" with locks ").append(queue.get(i).printWaitLocks()).append("\n");
                }
                BranchLogger.v("Queue is: " + stringBuilder);
            }
        }
    }
    
    /**
     * <p>Gets the queued {@link ServerRequest} object at position with index specified in the supplied
     * parameter, within the queue. Like {@link #peek()}, the item is not removed from the queue.</p>
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
                BranchLogger.v("Queue operation peekAt " + req);
            } catch (IndexOutOfBoundsException | NoSuchElementException e) {
                BranchLogger.e("Caught Exception ServerRequestQueue peekAt " + index + ": " + e.getMessage());
            }
        }
        return req;
    }
    
    /**
     * <p>As the method name implies, inserts a {@link ServerRequest} into the queue at the index
     * position specified.</p>
     *
     * @param request The {@link ServerRequest} to insert into the queue.
     * @param index   An {@link Integer} value specifying the index at which to insert the
     *                supplied {@link ServerRequest} object. Fails silently if the index
     *                supplied is invalid.
     */
    void insert(ServerRequest request, int index) {
        synchronized (reqQueueLockObject) {
            try {
                BranchLogger.v("Queue operation insert. Request: " + request + " Size: " + queue.size() + " Index: " + index);
                if (queue.size() < index) {
                    index = queue.size();
                }
                queue.add(index, request);
            } catch (IndexOutOfBoundsException e) {
                BranchLogger.e("Caught IndexOutOfBoundsException " + e.getMessage());
            }
        }
    }
    
    /**
     * <p>As the method name implies, removes the {@link ServerRequest} object, at the position
     * indicated by the {@link Integer} parameter supplied.</p>
     *
     * @param index An {@link Integer} value specifying the index at which to remove the
     *              {@link ServerRequest} object. Fails silently if the index
     *              supplied is invalid.
     * @return The {@link ServerRequest} object being removed.
     */
    @SuppressWarnings("unused")
    public ServerRequest removeAt(int index) {
        ServerRequest req = null;
        synchronized (reqQueueLockObject) {
            try {
                req = queue.remove(index);
            } catch (IndexOutOfBoundsException e) {
                BranchLogger.e("Caught IndexOutOfBoundsException " + e.getMessage());
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
                BranchLogger.v("Queue operation remove. Request: " + request);
                isRemoved = queue.remove(request);
                BranchLogger.v("Queue operation remove. Removed: " + isRemoved);
            } catch (UnsupportedOperationException e) {
                BranchLogger.e("Caught UnsupportedOperationException " + e.getMessage());
            }
        }
        return isRemoved;
    }
    
    /**
     * <p> Clears all pending requests in the queue </p>
     */
    void clear() {
        synchronized (reqQueueLockObject) {
            try {
                BranchLogger.v("Queue operation clear");
                queue.clear();
                BranchLogger.v("Queue cleared.");
            } catch (UnsupportedOperationException e) {
                BranchLogger.e("Caught UnsupportedOperationException " + e.getMessage());
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
                BranchLogger.v("Checking if " + req + " is instanceof ServerRequestInitSession");
                if (req instanceof ServerRequestInitSession) {
                    ServerRequestInitSession r = (ServerRequestInitSession) req;
                    BranchLogger.v(r + " is initiated by client: " + r.initiatedByClient);
                    if (r.initiatedByClient) {
                        return r;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Set Process wait lock to false for any open / install request in the queue
     */
    void unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK lock) {
        synchronized (reqQueueLockObject) {
            for (ServerRequest req : queue) {
                if (req != null) {
                    req.removeProcessWaitLock(lock);
                }
            }
        }
    }

    // We must check that there is no other init request that may read or write these values
    // Then when init request count in the queue is either the last or none, clear.
    public void postInitClear() {
        // Check for any Third party SDK for data handling
        PrefHelper prefHelper_ = Branch.getInstance().getPrefHelper();
        boolean canClear = this.canClearInitData();
        BranchLogger.v("postInitClear " + prefHelper_ + " can clear init data " + canClear);

        if(prefHelper_ != null && canClear) {
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setGoogleSearchInstallIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppStoreReferrer(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppLink(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setPushIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIsFullAppConversion(false);
            prefHelper_.setInitialReferrer(PrefHelper.NO_STRING_VALUE);

            if (prefHelper_.getLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME) == 0) {
                prefHelper_.setLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME, prefHelper_.getLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME));
            }
        }
    }

    void insertRequestAtFront(ServerRequest req) {
        BranchLogger.v("Queue operation insertRequestAtFront " + req + " networkCount_: " + networkCount_);
        if (networkCount_ == 0) {
            this.insert(req, 0);
        } else {
            this.insert(req, 1);
        }
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
        try {
            for (int i = 0; i < this.getSize(); i++) {
                ServerRequest req = this.peekAt(i);
                BranchLogger.v("Queue operation updateAllRequestsInQueue updating: " + req);
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
            BranchLogger.e("Caught JSONException " + e.getMessage());
        }
    }

    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The {@link ServerRequest} to execute
     */
    public void handleNewRequest(ServerRequest req) {
        BranchLogger.d("handleNewRequest " + req);
        // If Tracking is disabled fail all messages with ERR_BRANCH_TRACKING_DISABLED
        if (Branch.getInstance().getTrackingController().isTrackingDisabled() && !req.prepareExecuteWithoutTracking()) {
            String errMsg = "Requested operation cannot be completed since tracking is disabled [" + req.requestPath_.getPath() + "]";
            BranchLogger.d(errMsg);
            req.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, errMsg);
            return;
        }
        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (Branch.getInstance().initState_ != Branch.SESSION_STATE.INITIALISED && !(req instanceof ServerRequestInitSession)) {
            if (requestNeedsSession(req)) {
                BranchLogger.d("handleNewRequest " + req + " needs a session");
                req.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK);
            }
        }

        this.enqueue(req);
        req.onRequestQueued();
    }

    // If there is 1 (currently being removed) or 0 init requests in the queue, clear the init data
    public boolean canClearInitData() {
        int result = 0;
        synchronized (reqQueueLockObject) {
            for(int i = 0; i < queue.size(); i++){
                if(queue.get(i) instanceof ServerRequestInitSession){
                    result++;
                }
            }
        }
        return result <= 1;
    }

    ///-------Instrumentation additional data---------------///

    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param instrumentationData A {@link HashMap} with key value pairs for instrumentation data.
     */
    public void addExtraInstrumentationData(HashMap<String, String> instrumentationData) {
        instrumentationExtraData_.putAll(instrumentationData);
    }

    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param key   A {@link String} Value for instrumentation data key
     * @param value A {@link String} Value for instrumentation data value
     */
    public void addExtraInstrumentationData(String key, String value) {
        instrumentationExtraData_.put(key, value);
    }
}
