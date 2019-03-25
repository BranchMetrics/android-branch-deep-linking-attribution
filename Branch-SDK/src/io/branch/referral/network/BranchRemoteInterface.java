package io.branch.referral.network;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BuildConfig;
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
            return new ServerResponse(tag, BranchError.ERR_BRANCH_KEY_INVALID);
        }

        long reqStartTime = System.currentTimeMillis();
        PrefHelper.Debug("getting " + modifiedUrl);

        try {
            BranchResponse response = doRestfulGet(modifiedUrl);
            return processEntityForJSON(response.responseData, response.responseCode, tag);
        } catch (BranchRemoteException branchError) {
            if (branchError.branchErrorCode == BranchError.ERR_BRANCH_REQ_TIMED_OUT) {
                return new ServerResponse(tag, BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            } else { // All other errors are considered as connectivity error
                return new ServerResponse(tag, BranchError.ERR_BRANCH_NO_CONNECTIVITY);
            }
        } finally {
            // Add total round trip time
            if (Branch.getInstance() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
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
            return new ServerResponse(tag, BranchError.ERR_BRANCH_KEY_INVALID);
        }
        PrefHelper.Debug("posting to " + url);
        PrefHelper.Debug("Post value = " + body.toString());

        try {
            BranchResponse response = doRestfulPost(url, body);
            return processEntityForJSON(response.responseData, response.responseCode, tag);
        } catch (BranchRemoteException branchError) {
            if (branchError.branchErrorCode == BranchError.ERR_BRANCH_REQ_TIMED_OUT) {
                return new ServerResponse(tag, BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            } else { // All other errors are considered as connectivity error
                return new ServerResponse(tag, BranchError.ERR_BRANCH_NO_CONNECTIVITY);
            }
        } finally {
            if (Branch.getInstance() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
            }
        }
    }

    public static final BranchRemoteInterface getDefaultBranchRemoteInterface(Context context) {
        BranchRemoteInterface branchRemoteInterface = null;

        boolean isOkHttpAvailable = false; // TODO : Check for OKHTTP Availability
        if (isOkHttpAvailable) {
            // TODO return default OKHTTP Remote interface here
        } else {
            branchRemoteInterface = new BranchRemoteInterfaceUrlConnection(context);
        }
        return branchRemoteInterface;
    }


    //----------- private methods----------------------------//

    /**
     * <p>Converts resultant output object from Branch Remote server into a {@link ServerResponse} object by
     * reading the content supplied in the raw server response, and creating a {@link JSONObject}
     * that contains the same data. This data is then attached as the post data of the
     * {@link ServerResponse} object returned.</p>
     *
     * @param responseString Branch server response received. A  string form of the input or error stream payload
     * @param statusCode     An {@link Integer} value containing the HTTP response code.
     * @param tag            A {@link String} value containing the tag value to be applied to the
     *                       resultant {@link ServerResponse} object.
     * @return A {@link ServerResponse} object representing the resultant output object from Branch Remote server
     * response in Branch SDK terms.
     * see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1: Status Codes</a>
     */
    private ServerResponse processEntityForJSON(String responseString, int statusCode, String tag) {
        ServerResponse result = new ServerResponse(tag, statusCode);
        PrefHelper.Debug("returned " + responseString);

        if (responseString != null) {
            try {
                JSONObject jsonObj = new JSONObject(responseString);
                result.setPost(jsonObj);
            } catch (JSONException ex) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    result.setPost(jsonArray);
                } catch (JSONException ex2) {
                    PrefHelper.Debug("JSON exception: " + ex2.getMessage());
                }
            }
        }
        return result;
    }

    private boolean addCommonParams(JSONObject post, String branch_key) {
        try {
            if (!post.has(Defines.Jsonkey.UserData.getKey())) { // user data already has the sdk in it as part of v2 request
                post.put(Defines.Jsonkey.SDK.getKey(), "android" + BuildConfig.VERSION_NAME);
            }
            if (!branch_key.equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.BranchKey.getKey(), branch_key);
                return true;
            }
        } catch (JSONException ignore) {
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
                        e.printStackTrace();
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
        private int branchErrorCode = BranchError.ERR_BRANCH_NO_CONNECTIVITY;

        /**
         * Creates BranchRemoteException
         *
         * @param errorCode Error code for operation failure. Should be one of
         *                  {@link BranchError#ERR_BRANCH_REQ_TIMED_OUT} | {@link BranchError#ERR_BRANCH_NO_CONNECTIVITY}
         */
        public BranchRemoteException(int errorCode) {
            branchErrorCode = errorCode;
        }
    }

}
