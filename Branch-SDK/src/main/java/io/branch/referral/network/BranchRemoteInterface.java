package io.branch.referral.network;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerResponse;

/**
 * <p>
 * Abstract class for Branch remote interface. This class provides the abstraction layer for network operations between
 * Branch SDK and remote Branch servers. Extend this class for creating custom network interface.
 * Class provide abstract method for implementing RESTful requests to Branch server
 *
 * see {@link #doRestfulGet(String)}
 * {@link #doRestfulPost(String, JSONObject)}
 * {@link io.branch.referral.network.BranchRemoteInterface.BranchResponse}
 * {@link BranchRemoteException}
 * </p>
 */
public abstract class BranchRemoteInterface {
    /**
     * Key for adding retry numbers for the request. This will help better network issue analysis and debugging.
     * Add the retry number to the GET Request as a query param and as a JSon key value for post
     */
    public static final String RETRY_NUMBER = "retryNumber";

    //----------- Abstract methods-----------------------//

    /**
     * <p>
     * Abstract method to implement the network layer to do a RESTful GET to Branch servers.
     * This method is called whenever Branch SDK want to make a GET request to Branch servers.
     * Please note that this methods always called on the background thread and no need for thread switching for the network operations.
     *
     * @param url The url end point
     * @return {@link io.branch.referral.network.BranchRemoteInterface.BranchResponse} with the get result data and http status code
     * @throws BranchRemoteException Branch remote exception is thrown when there is an error in communicating to the Branch servers
     *                               BranchRemoteException contains the corresponding BranchError code for the error {@link BranchError#ERR_BRANCH_NO_CONNECTIVITY } | {@link BranchError#ERR_BRANCH_REQ_TIMED_OUT}
     *                               see {@link io.branch.referral.network.BranchRemoteInterface.BranchRemoteException}
     *                               {@link io.branch.referral.network.BranchRemoteInterface.BranchResponse}
     *
     *                               NOTE: For better debugging purpose conside adding {@link #RETRY_NUMBER} as a query params if you implement multiple retries for your request
     *                               </p>
     */
    public abstract BranchResponse doRestfulGet(String url) throws BranchRemoteException;

    /**
     * <p>
     * Abstract method to implement the network layer to do a RESTful GET to Branch servers.
     * This method is called whenever Branch SDK want to make a GET request to Branch servers.
     * Please note that this methods always called on the background thread and no need for thread switching to execute network operations.
     *
     * @param url     The url end point
     * @param payload The JSon object payload for the post request
     * @return {@link io.branch.referral.network.BranchRemoteInterface.BranchResponse} with the get result data and http status code
     * @throws BranchRemoteException Branch remote exception is thrown when there is an error in communicating to the Branch servers
     *                               BranchRemoteException contains the corresponding BranchError code for the error {@link BranchError#ERR_BRANCH_NO_CONNECTIVITY } | {@link BranchError#ERR_BRANCH_REQ_TIMED_OUT}
     *                               see {@link io.branch.referral.network.BranchRemoteInterface.BranchRemoteException}
     *                               {@link io.branch.referral.network.BranchRemoteInterface.BranchResponse}
     *
     *                               NOTE: For better debugging purpose conside adding {@link #RETRY_NUMBER} as a JSon keyvalue  if you implement multiple retries for your request
     *                               </p>
     */
    public abstract BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException;

    //--------- public methods-----------------//

    /**
     * Method for handling the RESTful POST operations to Branch Servers. Internally calls abstract method {@link #doRestfulGet(String)}
     *
     * @param url       The url end point
     * @param params    {@link JSONObject with parameters to the GET call}
     * @param tag       {@link String} Tag for identifying the request for analytical or debugging purpose
     * @param branchKey {@link String} Branch key
     * @return {@link ServerResponse} object representing the result of RESTful GET to Branch Server
     */
    public final ServerResponse make_restful_get(String url, JSONObject params, String tag, String branchKey) {
        String modifiedUrl = url;
        params = params != null ? params : new JSONObject();
        if (addCommonParams(params, branchKey)) {
            modifiedUrl += this.convertJSONtoString(params);
        } else {
            return new ServerResponse(tag, BranchError.ERR_BRANCH_KEY_INVALID, "", "Invalid key");
        }

        long reqStartTime = System.currentTimeMillis();
        BranchLogger.v("getting " + modifiedUrl);

        try {
            BranchResponse response = doRestfulGet(modifiedUrl);
            return processEntityForJSON(response, tag, response.requestId);
        } catch (BranchRemoteException branchError) {
            return new ServerResponse(tag, branchError.branchErrorCode, "", branchError.branchErrorMessage);
        } finally {
            // Add total round trip time
            if (Branch.init() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.init().requestQueue_.addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
            }
        }
    }

    /**
     * Method for handling the RESTful POST operations to Branch Servers. Internally calls abstract method {@link #doRestfulPost(String, JSONObject)}
     *
     * @param url       The url end point
     * @param body      {@link JSONObject with parameters to the POST call}
     * @param tag       {@link String} Tag for identifying the request for analytical or debugging purpose
     * @param branchKey {@link String} Branch key
     * @return {@link ServerResponse} object representing the result of RESTful POST to Branch Server
     */
    public final ServerResponse make_restful_post(JSONObject body, String url, String tag, String branchKey) {
        long reqStartTime = System.currentTimeMillis();
        body = body != null ? body : new JSONObject();

        if (!addCommonParams(body, branchKey)) {
            return new ServerResponse(tag, BranchError.ERR_BRANCH_KEY_INVALID, "", "Failed to set common parameters, body: " + body + " key: " + branchKey);
        }
        BranchLogger.v("posting to " + url);
        BranchLogger.v("Post value = " + body.toString());

        try {
            BranchResponse response = doRestfulPost(url, body);
            return processEntityForJSON(response, tag, response.requestId);
        } catch (BranchRemoteException branchError) {
            return new ServerResponse(tag, branchError.branchErrorCode, "",  "Failed network request. " + branchError.branchErrorMessage);
        } finally {
            if (Branch.init() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.init().requestQueue_.addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
            }
        }
    }


    //----------- private methods----------------------------//

    /**
     * <p>Converts resultant output object from Branch Remote server into a {@link ServerResponse} object by
     * reading the content supplied in the raw server response, and creating a {@link JSONObject}
     * that contains the same data. This data is then attached as the post data of the
     * {@link ServerResponse} object returned.</p>
     *
     * @param response Branch server response received containing response data with headers and response code
     * @param tag            A {@link String} value containing the tag value to be applied to the
     *                       resultant {@link ServerResponse} object.
     * @return A {@link ServerResponse} object representing the resultant output object from Branch Remote server
     * response in Branch SDK terms.
     * see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1: Status Codes</a>
     */
    private ServerResponse processEntityForJSON(BranchResponse response, String tag, String requestId) {
        String responseString = response.responseData;

        int statusCode = response.responseCode;

        ServerResponse result = new ServerResponse(tag, statusCode, requestId, "");
        if(!TextUtils.isEmpty(requestId)){
            BranchLogger.v(String.format(Locale.getDefault(), "Server returned: [%s] Status: [%d]; Data: %s", requestId, statusCode, responseString));
        } else {
            BranchLogger.v(String.format("returned %s", responseString));
        }

        if (responseString != null) {
            try {
                JSONObject jsonObj = new JSONObject(responseString);
                result.setPost(jsonObj);
            } catch (JSONException ex) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    result.setPost(jsonArray);
                } catch (JSONException ex2) {
                    if (tag.contains(Defines.Jsonkey.QRCodeTag.getKey())) {
                        try {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put(Defines.Jsonkey.QRCodeResponseString.getKey(), responseString);
                            result.setPost(jsonObj);
                        } catch (JSONException e) {
                            BranchLogger.w("Caught JSONException " + e.getMessage());
                        }
                    } else {
                        BranchLogger.w("Caught JSONException " + ex2.getMessage());
                    }
                }
            }
        }
        return result;
    }

    private boolean addCommonParams(JSONObject post, String branch_key) {
        BranchLogger.v("addCommonParams post: " + post + " key: " + branch_key);
        try {
            if (!post.has(Defines.Jsonkey.UserData.getKey())) { // user data already has the sdk in it as part of v2 request
                post.put(Defines.Jsonkey.SDK.getKey(), "android" + Branch.getSdkVersionNumber());
            }
            if (!branch_key.equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.BranchKey.getKey(), branch_key);
                return true;
            }
        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException " + e.getMessage());
        }
        return false;
    }


    private String convertJSONtoString(JSONObject json) {
        StringBuilder result = new StringBuilder();
        if (json != null) {
            JSONArray names = json.names();
            if (names != null) {
                boolean first = true;
                int size = names.length();
                for (int i = 0; i < size; i++) {
                    try {
                        String key = names.getString(i);

                        if (first) {
                            result.append("?");
                            first = false;
                        } else {
                            result.append("&");
                        }

                        String value = json.getString(key);
                        result.append(key).append("=").append(value);
                    } catch (JSONException e) {
                        BranchLogger.w("Caught JSONException " + e.getMessage());
                        return null;
                    }
                }
            }
        }

        return result.toString();
    }


    //-------------- Supporting classes -----------------------//

    /**
     * <p>
     * Class for providing result of RESTful operation against Branch Remote server
     * </p>
     */
    public static class BranchResponse {
        private final String responseData;
        private final int responseCode;
        String requestId;

        /**
         * Creates a BranchResponse object with response data and status code
         *
         * @param responseData The data returned by branch server. Nullable in case of errors.(Note :please see {@link io.branch.referral.network.BranchRemoteInterface.BranchRemoteException} for a better handling of errors)
         * @param responseCode Standard Http Response code (rfc2616 http error codes)
         */
        public BranchResponse(@Nullable String responseData, int responseCode) {
            this.responseData = responseData;
            this.responseCode = responseCode;
        }
    }

    /**
     * Exception thrown when there is an error while doing a restful operation with Branch Remote server
     * see {@link #doRestfulGet(String)} and {@link #doRestfulPost(String, JSONObject)}
     */
    public static class BranchRemoteException extends Exception {
        private int branchErrorCode;
        private String branchErrorMessage;

        /**
         * Creates BranchRemoteException
         *
         * @param errorCode Error code for operation failure. Should be one of
         *                  {@link BranchError#ERR_BRANCH_REQ_TIMED_OUT} | {@link BranchError#ERR_BRANCH_NO_CONNECTIVITY}
         */
        public BranchRemoteException(int errorCode) {
            branchErrorCode = errorCode;
        }

        public BranchRemoteException(int errorCode, String errorMessage) {
            branchErrorCode = errorCode;
            branchErrorMessage = errorMessage;
        }
    }

}
