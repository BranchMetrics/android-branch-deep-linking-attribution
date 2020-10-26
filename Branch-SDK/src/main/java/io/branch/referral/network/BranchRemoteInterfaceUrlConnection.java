package io.branch.referral.network;

import android.content.Context;
import android.net.TrafficStats;
import android.os.NetworkOnMainThreadException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sojanpr on 5/31/17.
 * Class for implementing BranchRemoteInterface using the HttpUrlConnection.
 * This class provides implementation for Branch RESTful operations using HTTP URL Connection.
 */
public class BranchRemoteInterfaceUrlConnection extends BranchRemoteInterface {
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int THREAD_TAG_POST= 102;

    private PrefHelper prefHelper;

    BranchRemoteInterfaceUrlConnection(Context context) {
        prefHelper = PrefHelper.getInstance(context);
    }

    @Override
    public BranchResponse doRestfulGet(String url) throws BranchRemoteException {
        return doRestfulGet(url, 0);
    }

    @Override
    public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
        return doRestfulPost(url, payload, 0);
    }


    ///-------------- private methods to implement RESTful GET / POST using HttpURLConnection ---------------//
    private BranchResponse doRestfulGet(String url, int retryNumber) throws BranchRemoteException {
        HttpsURLConnection connection = null;
        String requestId = null;
        try {
            int timeout = prefHelper.getTimeout();
            if (timeout <= 0) {
                timeout = DEFAULT_TIMEOUT;
            }
            String appendKey = url.contains("?") ? "&" : "?";
            String modifiedUrl = url + appendKey + RETRY_NUMBER + "=" + retryNumber;
            URL urlObject = new URL(modifiedUrl);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            requestId = getRequestIdFromHeader(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 500 &&
                    retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            } else {
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        return new BranchResponse(getResponseString(connection.getErrorStream()), responseCode, requestId);
                    } else {
                        return new BranchResponse(getResponseString(connection.getInputStream()), responseCode, requestId);
                    }
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    PrefHelper.Debug("A resource conflict occurred with this request " + url);
                    return new BranchResponse(null, responseCode, requestId);
                }
            }
        } catch (SocketException ex) {
            PrefHelper.Debug("Http connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);

        } catch (SocketTimeoutException ex) {
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            } else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            }
        } catch (IOException ex) {
            PrefHelper.Debug("Branch connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private BranchResponse doRestfulPost(String url, JSONObject payload, int retryNumber) throws BranchRemoteException {
        HttpsURLConnection connection = null;
        String requestId = null;
        int timeout = prefHelper.getTimeout();
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        try {
            payload.put(RETRY_NUMBER, retryNumber);
        } catch (JSONException ignore) {
        }
        try {
            // set the setThreadStatsTag for POST if API 26+
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                TrafficStats.setThreadStatsTag(THREAD_TAG_POST);
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

            outputStreamWriter.write(payload.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            requestId = getRequestIdFromHeader(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode >= HttpsURLConnection.HTTP_INTERNAL_ERROR
                    && retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            } else {
                InputStream inputStream = null;
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        inputStream = connection.getErrorStream();
                    } else {
                        inputStream = connection.getInputStream();
                    }
                    return new BranchResponse(getResponseString(inputStream), responseCode, requestId);
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    PrefHelper.Debug("A resource conflict occurred with this request " + url);
                    return new BranchResponse(null, responseCode, requestId);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        } catch (SocketTimeoutException ex) {
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            } else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_REQ_TIMED_OUT);
            }
        } catch (IOException ex) {
            PrefHelper.Debug("Http connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        } catch (Exception ex) {
            PrefHelper.Debug("Exception: " + ex.getMessage());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                if (ex instanceof NetworkOnMainThreadException)
                    PrefHelper.Debug("Branch Error: Don't call our synchronous methods on the main thread!!!");
            }
            return new BranchResponse(null, 500, requestId);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private String getRequestIdFromHeader(@NonNull HttpsURLConnection connection) {
        if (!connection.getHeaderFields().isEmpty() && connection.getHeaderFields()
            .containsKey("X-Branch-Request-Id")) {
            return connection.getHeaderField("X-Branch-Request-Id");
        }
        return null;
    }


    private String getResponseString(InputStream inputStream) {
        String responseString = null;
        if (inputStream != null) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            try {
                responseString = rd.readLine();
            } catch (IOException ignore) {
            }
        }
        return responseString;
    }

}
