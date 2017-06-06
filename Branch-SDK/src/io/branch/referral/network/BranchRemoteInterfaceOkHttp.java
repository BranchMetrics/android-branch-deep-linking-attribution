package io.branch.referral.network;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

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
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by sojanpr on 5/31/17.
 * Class for implementing BranchRemoteInterface using the HttpUrlConnection.
 * This class provides implementation for Branch RESTful operations using HTTP URL Connection.
 */
public class BranchRemoteInterfaceOkHttp extends BranchRemoteInterface {
    private static final int DEFAULT_TIMEOUT = 3000;
    PrefHelper prefHelper;
    private OkHttpClient okHttpClient_;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    BranchRemoteInterfaceOkHttp(Context context) {
        prefHelper = PrefHelper.getInstance(context);
        int timeout = prefHelper.getTimeout();
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        okHttpClient_ = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
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
        try {
            String appendKey = url.contains("?") ? "&" : "?";
            String modifiedUrl = url + appendKey + RETRY_NUMBER + "=" + retryNumber;

            Request request = new Request.Builder()
                    .url(modifiedUrl)
                    .build();
            Response response = okHttpClient_.newCall(request).execute();

            int responseCode = response.code();
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
                if (responseCode != HttpsURLConnection.HTTP_OK && !response.isSuccessful()) {
                    return new BranchResponse(getResponseString(null), responseCode);
                } else {
                    return new BranchResponse(getResponseString(response.body().byteStream()), responseCode);
                }
            }
        } catch (SocketException ex) {
            PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
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
            PrefHelper.Debug(getClass().getSimpleName(), "Branch connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        }
    }


    private BranchResponse doRestfulPost(String url, JSONObject payload, int retryNumber) throws BranchRemoteException {
        try {
            payload.put(RETRY_NUMBER, retryNumber);
        } catch (JSONException ignore) {
        }
        try {
            RequestBody rbody = RequestBody.create(JSON, payload.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(rbody)
                    .build();
            Response response = okHttpClient_.newCall(request).execute();
            int responseCode = response.code();
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
                if (responseCode != HttpsURLConnection.HTTP_OK && !response.isSuccessful()) {
                    return new BranchResponse(getResponseString(null), responseCode);
                } else {
                    return new BranchResponse(getResponseString(response.body().byteStream()), responseCode);
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
            PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        } catch (Exception ex) {
            PrefHelper.Debug(getClass().getSimpleName(), "Exception: " + ex.getMessage());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                if (ex instanceof NetworkOnMainThreadException)
                    Log.i("BranchSDK", "Branch Error: Don't call our synchronous methods on the main thread!!!");
            }
            return new BranchResponse(null, 500);
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
