package io.branch.referral;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * <p>This class assists with RESTful calls to the Branch API, by using
 * {@link HttpsURLConnection} object, and handling all restful calls via one of its GET or POST capable
 * methods.</p>
 */
class RemoteInterface {
    public static final String BRANCH_KEY = "branch_key";
    public static final int NO_CONNECTIVITY_STATUS = -1009;
    public static final int NO_BRANCH_KEY_STATUS = -1234;

    static final String SDK_VERSION = "2.4.2";
    private static final int DEFAULT_TIMEOUT = 3000;

    /**
     * Required, default constructor for the class.
     */
    public RemoteInterface() {
    }

    /**
     * <p>A {@link PrefHelper} object that is used throughout the class to allow access to read and
     * write preferences related to the SDK.</p>
     *
     * @see PrefHelper
     */
    protected PrefHelper prefHelper_;

    public RemoteInterface(Context context) {
        prefHelper_ = PrefHelper.getInstance(context);
    }


    /**
     * <p>Converts {@link HttpsURLConnection} resultant output object into a {@link ServerResponse} object by
     * reading the content supplied in the raw server response, and creating a {@link JSONObject}
     * that contains the same data. This data is then attached as the post data of the
     * {@link ServerResponse} object returned.</p>
     *
     * @param inStream   A generic {@link InputStream} returned as a result of a HTTP connection.
     * @param statusCode An {@link Integer} value containing the HTTP response code.
     * @param tag        A {@link String} value containing the tag value to be applied to the
     *                   resultant {@link ServerResponse} object.
     * @param log        A {@link Boolean} value indicating whether or not to log the raw
     *                   content lines via the debug interface.
     * @return A {@link ServerResponse} object representing the {@link HttpsURLConnection}
     * response in Branch SDK terms.
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1: Status Codes</a>
     */
    private ServerResponse processEntityForJSON(InputStream inStream, int statusCode, String tag, boolean log) {
        ServerResponse result = new ServerResponse(tag, statusCode);
        try {
            if (inStream != null) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(inStream));

                String line = rd.readLine();
                if (log) PrefHelper.Debug("BranchSDK", "returned " + line);

                if (line != null) {
                    try {
                        JSONObject jsonObj = new JSONObject(line);
                        result.setPost(jsonObj);
                    } catch (JSONException ex) {
                        try {
                            JSONArray jsonArray = new JSONArray(line);
                            result.setPost(jsonArray);
                        } catch (JSONException ex2) {
                            if (log)
                                PrefHelper.Debug(getClass().getSimpleName(), "JSON exception: " + ex2.getMessage());
                        }
                    }
                }
            }
        } catch (IOException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
        }
        return result;
    }

    /**
     * <p>Make a RESTful GET request, by calling {@link #make_restful_get(String, JSONObject, String, int, int, boolean)}
     * with the logging {@link Boolean} parameter pre-populated.</p>
     *
     * @param url     A {@link String} URL to request from.
     * @param params  A {@link JSONObject} with additional parameters and their values to send with the get request.
     * @param tag     A {@link String} tag for logging/analytics purposes.
     * @param timeout An {@link Integer} value containing the number of milliseconds to wait
     *                before considering a server request to have timed out.
     * @return A {@link ServerResponse} object containing the result of the RESTful request.
     */
    public ServerResponse make_restful_get(String url, JSONObject params, String tag, int timeout) {
        return make_restful_get(url, params, tag, timeout, 0, true);
    }

    private boolean addCommonParams(JSONObject post, int retryNumber) {
        try {
            String branch_key = prefHelper_.getBranchKey();

            post.put("sdk", "android" + SDK_VERSION);
            post.put("retryNumber", retryNumber);
            if (!branch_key.equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(BRANCH_KEY, prefHelper_.getBranchKey());
                return true;
            }
        } catch (JSONException ignore) {
        }
        return false;
    }

    /**
     * <p>The main RESTful GET method; the other one ({@link #make_restful_get(String, JSONObject, String, int)}) calls this one
     * with a pre-populated logging parameter.</p>
     *
     * @param baseUrl A {@link String} URL to request from.
     * @param params  A {@link JSONObject} with additional parameters and their values to send with the get request.
     * @param tag     A {@link String} tag for logging/analytics purposes.
     * @param timeout An {@link Integer} value containing the number of milliseconds to wait
     *                before considering a server request to have timed out.
     * @param log     A {@link Boolean} value that specifies whether debug logging should be
     *                enabled for this request or not.
     * @return A {@link ServerResponse} object containing the result of the RESTful request.
     */
    private ServerResponse make_restful_get(String baseUrl, JSONObject params, String tag, int timeout, int retryNumber, boolean log) {
        String modifiedUrl = baseUrl;
        JSONObject getParameters = new JSONObject();
        HttpsURLConnection connection = null;
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (addCommonParams(getParameters, retryNumber)) {
            if (params != null) {
                Iterator keys = params.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    try {
                        getParameters.put(key, params.getString(key));
                    } catch (JSONException ignore) {
                    }
                }
            }
            modifiedUrl += this.convertJSONtoString(getParameters);
        } else {
            return new ServerResponse(tag, NO_BRANCH_KEY_STATUS);
        }

        long reqStartTime = System.currentTimeMillis();
        try {
            if (log) PrefHelper.Debug("BranchSDK", "getting " + modifiedUrl);
            URL urlObject = new URL(modifiedUrl);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int lrtt = (int) (System.currentTimeMillis() - reqStartTime);
            if (Branch.getInstance() != null) {
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Last_Round_Trip_Time.getKey(), String.valueOf(lrtt));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode >= 500 &&
                    retryNumber < prefHelper_.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper_.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return make_restful_get(baseUrl, params, tag, timeout, retryNumber, log);
            } else {
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        return processEntityForJSON(connection.getErrorStream(),
                                responseCode, tag, log);
                    } else {
                        return processEntityForJSON(connection.getInputStream(),
                                responseCode, tag, log);
                    }
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    if (log) {
                        PrefHelper.Debug("BranchSDK", "A resource conflict occurred with this request " + tag);
                    }
                    return processEntityForJSON(null, responseCode, tag, log);
                }
            }
        } catch (SocketException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
            return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
        } catch (SocketTimeoutException ex) {
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < prefHelper_.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper_.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return make_restful_get(baseUrl, params, tag, timeout, retryNumber, log);
            } else {
                return new ServerResponse(tag, BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            }
        } catch (UnknownHostException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
            return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
        } catch (IOException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
            return new ServerResponse(tag, 500);
        } finally {
            // Add total round trip time
            if (Branch.getInstance() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

    }
    //endregion

    /**
     * <p>Makes a RESTful POST call with logging enabled, without an associated data dictionary;
     * passed as null.</p>
     *
     * @param body    A {@link JSONObject} containing the main data body/payload of the request.
     * @param url     A {@link String} URL to request from.
     * @param tag     A {@link String} tag for logging/analytics purposes.
     * @param timeout An {@link Integer} value containing the number of milliseconds to wait
     *                before considering a server request to have timed out.
     * @return A {@link ServerResponse} object representing the {@link HttpsURLConnection}
     * response in Branch SDK terms.
     */
    public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout) {
        return make_restful_post(body, url, tag, timeout, 0, true);
    }


    /**
     * <p>Makes a RESTful POST call without an associated data dictionary; passed as null.</p>
     *
     * @param body    A {@link JSONObject} containing the main data body/payload of the request.
     * @param url     A {@link String} URL to request from.
     * @param tag     A {@link String} tag for logging/analytics purposes.
     * @param timeout An {@link Integer} value containing the number of milliseconds to wait
     *                before considering a server request to have timed out.
     * @param log     A {@link Boolean} value that specifies whether debug logging should be
     *                enabled for this request or not.
     * @return A {@link ServerResponse} object representing the {@link HttpsURLConnection}
     * response in Branch SDK terms.
     */
    public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout, boolean log) {
        return make_restful_post(body, url, tag, timeout, 0, log);
    }

    /**
     * <p>The main RESTful POST method. The others call this one with pre-populated parameters.</p>
     *
     * @param body    A {@link JSONObject} containing the main data body/payload of the request.
     * @param url     A {@link String} URL to request from.
     * @param tag     A {@link String} tag for logging/analytics purposes.
     * @param timeout An {@link Integer} value containing the number of milliseconds to wait
     *                before considering a server request to have timed out.
     * @param log     A {@link Boolean} value that specifies whether debug logging should be
     *                enabled for this request or not.
     * @return A {@link ServerResponse} object representing the {@link HttpsURLConnection}
     * response in Branch SDK terms.
     */
    private ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout,
                                             int retryNumber, boolean log) {
        HttpsURLConnection connection = null;
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        JSONObject bodyCopy = new JSONObject();
        long reqStartTime = System.currentTimeMillis();
        try {

            Iterator<?> keys = body.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                try {
                    bodyCopy.put(key, body.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (!addCommonParams(bodyCopy, retryNumber)) {
                return new ServerResponse(tag, NO_BRANCH_KEY_STATUS);
            }
            if (log) {
                PrefHelper.Debug("BranchSDK", "posting to " + url);
                PrefHelper.Debug("BranchSDK", "Post value = " + bodyCopy.toString(4));
            }
            URL urlObject = new URL(url);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            int lrtt = (int) (System.currentTimeMillis() - reqStartTime);
            if (Branch.getInstance() != null) {
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Last_Round_Trip_Time.getKey(), String.valueOf(lrtt));
            }
            outputStreamWriter.write(bodyCopy.toString());
            outputStreamWriter.flush();

            int responseCode = connection.getResponseCode();
            if (responseCode >= HttpsURLConnection.HTTP_INTERNAL_ERROR
                    && retryNumber < prefHelper_.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper_.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return make_restful_post(bodyCopy, url, tag, timeout, retryNumber, log);
            } else {
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        return processEntityForJSON(connection.getErrorStream(), responseCode, tag, log);
                    } else {
                        return processEntityForJSON(connection.getInputStream(), responseCode, tag, log);
                    }
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    if (log) {
                        PrefHelper.Debug("BranchSDK", "A resource conflict occurred with this request " + tag);
                    }
                    return processEntityForJSON(null, responseCode, tag, log);
                }
            }


        } catch (SocketException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
            return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
        } catch (UnknownHostException ex) {
            if (log)
                PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
            return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
        } catch (SocketTimeoutException ex) {
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < prefHelper_.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper_.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return make_restful_post(bodyCopy, url, tag, timeout, retryNumber, log);
            } else {
                return new ServerResponse(tag, BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            }
        } catch (Exception ex) {
            if (log) PrefHelper.Debug(getClass().getSimpleName(), "Exception: " + ex.getMessage());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                if (ex instanceof NetworkOnMainThreadException)
                    Log.i("BranchSDK", "Branch Error: Don't call our synchronous methods on the main thread!!!");
            }
            return new ServerResponse(tag, 500);
        } finally {
            if (Branch.getInstance() != null) {
                int brttVal = (int) (System.currentTimeMillis() - reqStartTime);
                Branch.getInstance().addExtraInstrumentationData(tag + "-" + Defines.Jsonkey.Branch_Round_Trip_Time.getKey(), String.valueOf(brttVal));
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    //endregion

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

}
