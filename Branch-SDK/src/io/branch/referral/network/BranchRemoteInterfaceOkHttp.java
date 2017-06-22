package io.branch.referral.network;

import android.content.Context;
import android.os.Build;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

/**
 * Created by sojanpr on 5/31/17, EvanG added OkHttp on 6/6/2017
 * Class for implementing BranchRemoteInterface using OkHttp.
 * This class provides implementation for Branch RESTful operations using OkHttp3
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
        okHttpClient_ = getNewHttpClient(timeout);
    }

    private OkHttpClient getNewHttpClient(int timeout) {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false);

        return enableTls12OnPreLollipop(client).build();
    }

    @Override
    public BranchResponse doRestfulGet(String url) throws BranchRemoteException {
        return doRestfulGet(url, 0);
    }

    @Override
    public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
        return doRestfulPost(url, payload, 0);
    }

    ///-------------- private methods to implement RESTful GET / POST using OkHttp ---------------//
    private BranchResponse doRestfulGet(String url, int retryNumber) throws BranchRemoteException {
        Response response = null;
        try {
            String appendKey = url.contains("?") ? "&" : "?";
            String modifiedUrl = url + appendKey + RETRY_NUMBER + "=" + retryNumber;

            Request request = new Request.Builder()
                    .url(modifiedUrl)
                    .build();

            response = okHttpClient_.newCall(request).execute();
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
                InputStream inputStream;
                if (response.isSuccessful()) {
                    try {
                        inputStream = response.body().byteStream();
                    } catch (NullPointerException e) {
                        inputStream = null;
                    }
                    return new BranchResponse(getResponseString(inputStream), responseCode);
                } else {
                    return new BranchResponse(null, responseCode);
                }
            }
        } catch (IllegalStateException ex) {
            PrefHelper.Debug(getClass().getSimpleName(), "Http illegal state exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_ILLEGAL_STATE_EXCEPTION);
        } catch (IOException ex) {
            PrefHelper.Debug(getClass().getSimpleName(), "Branch connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private BranchResponse doRestfulPost(String url, JSONObject payload, int retryNumber) throws BranchRemoteException {
        Response response = null;
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
            response = okHttpClient_.newCall(request).execute();
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
                InputStream inputStream;
                if (response.isSuccessful()) {
                    try {
                        inputStream = response.body().byteStream();
                    } catch (NullPointerException e) {
                        inputStream = null;
                    }
                    return new BranchResponse(getResponseString(inputStream), responseCode);
                } else {
                    return new BranchResponse(null, responseCode);
                }
            }
        } catch (IllegalStateException ex) {
            PrefHelper.Debug(getClass().getSimpleName(), "Http illegal state exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_ILLEGAL_STATE_EXCEPTION);
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
        } finally {
            if (response != null) {
                response.close();
            }
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

    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }

}
