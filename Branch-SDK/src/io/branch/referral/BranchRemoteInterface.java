package io.branch.referral;

import android.content.Context;

import org.json.JSONObject;

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
     * <p>Required, but empty constructor method.</p>
     * <p>Use {@link #BranchRemoteInterface(Context)} instead, as it instantiates the class
     * {@link PrefHelper} and {@link SystemObserver} handles for the class.</p>
     */
    public BranchRemoteInterface() {
    }

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
     * <p>Create custom URL, and return the server response for use elsewhere within the app.</p>
     *
     * @param post A {@link JSONObject} containing post data key-value-pairs.
     * @return A {@link ServerResponse} object containing the Branch API response to the
     * request.
     */
    public ServerResponse createCustomUrlSync(JSONObject post) {
        String urlExtend = "v1/url";
        return make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, Defines.RequestPath.GetURL.getPath(), prefHelper_.getTimeout());
    }

    public SystemObserver getSystemObserver() {
        return sysObserver_;
    }
}
