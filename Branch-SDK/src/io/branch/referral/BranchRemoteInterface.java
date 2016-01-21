package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BranchRemoteInterface extends RemoteInterface {

    /**
     * <p>A {@link SystemObserver} object that is used throughout the class to report on the current
     * system (in the case of this SDK, an Android Device - phone, phablet, tablet, wearable?)
     * state and changeable attributes.</p>
     *
     * @see SystemObserver
     */
    private SystemObserver sysObserver_;

    /**
     * <p>A class-level {@link NetworkCallback} instance.</p>
     *
     * @see NetworkCallback
     */
    private NetworkCallback callback_;

    /**
     * <p>Required, but empty constructor method.</p>
     *
     * <p>Use {@link #BranchRemoteInterface(Context)} instead, as it instantiates the class
     * {@link PrefHelper} and {@link SystemObserver} handles for the class.</p>
     */
    public BranchRemoteInterface() {}

    /**
     * <p>The main constructor of the BranchRemoteInterface class.</p>
     *
     * @param context A {@link Context} from which this call was made.
     */
    public BranchRemoteInterface(Context context) {
        super(context);
        sysObserver_ = new SystemObserver(context);
    }

    /**
     * <p>Sets a callback listener to handle network events received during this app session.</p>
     *
     * @param callback A {@link NetworkCallback} object instance that will be triggered for
     *                 each network event that occurs during this app session.
     */
    public void setNetworkCallbackListener(NetworkCallback callback) {
        callback_ = callback;
    }

    /**
     * <p>Create custom URL, and return the server response for use elsewhere within the app.</p>
     *
     * @param post A {@link JSONObject} containing post data key-value-pairs.
     * @return A {@link ServerResponse} object containing the Branch API response to the
     * request.
     */
    public ServerResponse createCustomUrlSync(JSONObject post) {
        String urlExtend = "v1/url";
        BranchLinkData linkData = null;
        if (post instanceof BranchLinkData) {
            linkData = (BranchLinkData) post;
        }

        return make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, Defines.RequestPath.GetURL.getPath(), prefHelper_.getTimeout(), linkData);
    }

    public SystemObserver getSystemObserver() {
        return sysObserver_;
    }
}
