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

    /**
     * <p>Connect to server debug endpoint.</p>
     */
    public void connectToDebug() {
        // If session is not initialised no need to try to connect to debug
        if (prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)) {
            callback_.finished(new ServerResponse(PrefHelper.REQ_TAG_DEBUG_CONNECT, RemoteInterface.NO_CONNECTIVITY_STATUS));
        } else {
            try {
                String urlExtend = "v1/debug/connect";
                JSONObject post = new JSONObject();
                post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                if (sysObserver_.getBluetoothPresent()) {
                    post.put("device_name", BluetoothAdapter.getDefaultAdapter().getName());
                } else {
                    post.put("device_name", sysObserver_.getPhoneModel());
                }
                post.put(Defines.Jsonkey.OS.getKey(), sysObserver_.getOS());
                post.put(Defines.Jsonkey.OSVersion.getKey(), sysObserver_.getOSVersion());
                post.put(Defines.Jsonkey.Model.getKey(), sysObserver_.getPhoneModel());
                post.put("is_simulator", sysObserver_.isSimulator());
                post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                if (!prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                }
                callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_CONNECT, prefHelper_.getTimeout(), true));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * <p>Disconnect from the server debug interface.</p>
     */
    public void disconnectFromDebug() {
        // If device is not finger printed then return error
        if (prefHelper_.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE)) {
            callback_.finished(new ServerResponse(PrefHelper.REQ_TAG_DEBUG_CONNECT, RemoteInterface.NO_CONNECTIVITY_STATUS));
        } else {
            try {
                String urlExtend = "v1/debug/disconnect";
                JSONObject post = new JSONObject();
                post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                if (!prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                }
                if (!prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                }
                callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_DISCONNECT, prefHelper_.getTimeout(), true));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * <p>Log messages to the server's debug interface.</p>
     *
     * @param log A {@link String} variable containing information to log.
     */
    public void sendLog(String log) {
        // If device is not finger printed then return error
        if (prefHelper_.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE)) {
            callback_.finished(new ServerResponse(PrefHelper.REQ_TAG_DEBUG_CONNECT, RemoteInterface.NO_CONNECTIVITY_STATUS));
        } else {
            try {
                String urlExtend = "v1/debug/log";
                JSONObject post = new JSONObject();
                post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                if (!prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                }
                if (!prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                }
                post.put("log", log);
                callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, PrefHelper.REQ_TAG_DEBUG_LOG, prefHelper_.getTimeout(), true));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    public SystemObserver getSystemObserver() {
        return sysObserver_;
    }
}
