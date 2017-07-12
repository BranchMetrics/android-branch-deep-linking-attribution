package io.branch.referral.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * Created by sojanpr on 7/12/17.
 * <p>
 * Abstraction for Track event builders. This specifies the structure for basic event tracking builder.
 * Provides concrete implementation for executor method and handles reporting events to Branch
 * </p>
 */
public abstract class TrackEventBuilder<T extends TrackEventBuilder> {
    private final String requestPath;
    protected final String eventName;
    protected final JSONObject customData;
    protected ITrackEventListener callback;

    public TrackEventBuilder(String requestPath, String eventName) {
        this.eventName = eventName;
        this.requestPath = requestPath;
        customData = new JSONObject();
    }

    @SuppressWarnings("unchecked")
    public T addCustomData(String key, String value) {
        try {
            customData.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addCustomData(Map<String, String> data) {
        for (String key : data.keySet()) {
            try {
                customData.put(key, data.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCallback(ITrackEventListener callback) {
        this.callback = callback;
        return (T) this;
    }

    protected abstract JSONObject getRequestBody();

    public boolean track(Context context) {
        boolean isReqQueued = false;
        if (Branch.getInstance() != null) {
            Branch.getInstance().handleNewRequest(new ServerRequestTrackEvent(context));
            isReqQueued = true;
        }
        return isReqQueued;
    }


    private class ServerRequestTrackEvent extends ServerRequest {

        public ServerRequestTrackEvent(Context context) {
            super(context, requestPath);
            setPost(getRequestBody());
        }

        @Override
        public boolean handleErrors(Context context) {
            return false;
        }

        @Override
        public void onRequestSucceeded(ServerResponse response, Branch branch) {
            if (callback != null) {
                callback.onEventTracked();
            }
        }

        @Override
        public void handleFailure(int statusCode, String causeMsg) {
            if (callback != null) {
                callback.onEventTrackingFailed(new BranchError(causeMsg, statusCode));
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
    }

    public interface ITrackEventListener {
        void onEventTracked();

        void onEventTrackingFailed(BranchError branchError);
    }


}
