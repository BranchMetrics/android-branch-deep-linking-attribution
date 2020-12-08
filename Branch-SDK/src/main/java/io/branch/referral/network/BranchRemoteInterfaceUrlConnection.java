package io.branch.referral.network;

import android.content.Context;
import android.net.TrafficStats;
import android.os.NetworkOnMainThreadException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.util.Strings;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines;
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

    private @NonNull final Branch branch;

    public BranchRemoteInterfaceUrlConnection(@NonNull Branch branch) {
        this.branch = branch;
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
        PrefHelper prefHelper = PrefHelper.getInstance(branch.getApplicationContext());
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

            String requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.getKey());
            maybeSetCloseRequestFlag(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 500 && retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            } else {
                BranchResponse result;
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        result = new BranchResponse(getResponseString(connection.getErrorStream()), responseCode);
                    } else {
                        result = new BranchResponse(getResponseString(connection.getInputStream()), responseCode);
                    }
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    PrefHelper.Debug("A resource conflict occurred with this request " + url);
                    result = new BranchResponse(null, responseCode);
                }
                result.requestId = Strings.emptyToNull(requestId);
                return result;
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
        PrefHelper prefHelper = PrefHelper.getInstance(branch.getApplicationContext());
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

            requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.getKey());
            maybeSetCloseRequestFlag(connection);

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
                BranchResponse result;
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        result = new BranchResponse(getResponseString(connection.getErrorStream()), responseCode);
                    } else {
                        result = new BranchResponse(getResponseString(connection.getInputStream()), responseCode);
                    }
                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    PrefHelper.Debug("A resource conflict occurred with this request " + url);
                    result = new BranchResponse(null, responseCode);
                }
                result.requestId = requestId;
                return result;
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
            return new BranchResponse(null, 500);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void maybeSetCloseRequestFlag(HttpsURLConnection connection) {
        // technically only open/install events should have this header, but this method is called with
        // every request and, by default, "X-Branch-Send-Close-Request" header is not added to the response.
        // Note that, even if it gets added, we do not reset the `branch.closeRequestNeeded` flag if it has been set to `true`
        // at least once during this session already. The flag will be reset in `executeClose()` where we potentially call v1/close.
        // In the case of intra-app linking, this means that we will close session after the last v1/open event.
        @Nullable String maybeHeaderVal =  connection.getHeaderField(Defines.HeaderKey.SendCloseRequest.getKey());
        if (maybeHeaderVal != null && !branch.closeRequestNeeded) {
            branch.closeRequestNeeded = Boolean.parseBoolean(maybeHeaderVal);
        }
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
