package io.branch.referral.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.os.NetworkOnMainThreadException;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.util.Strings;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sojanpr on 5/31/17.
 * Class for implementing BranchRemoteInterface using the HttpUrlConnection.
 * This class provides implementation for Branch RESTful operations using HTTP URL Connection.
 */
public class BranchRemoteInterfaceUrlConnection extends BranchRemoteInterface {
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
            int connectTimeout = prefHelper.getConnectTimeout();
            String appendKey = url.contains("?") ? "&" : "?";
            String modifiedUrl = url + appendKey + RETRY_NUMBER + "=" + retryNumber;
            URL urlObject = new URL(modifiedUrl);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(timeout);

            String requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.getKey());

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
                    BranchLogger.v("A resource conflict occurred with this request " + url);
                    result = new BranchResponse(null, responseCode);
                }
                result.requestId = Strings.emptyToNull(requestId);
                return result;
            }
        } catch (SocketException ex) {
            BranchLogger.v("Http connect exception: " + ex.getMessage());
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
        } catch(InterruptedIOException ex){
            // When the thread times out before or while sending the request
            if (retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            } else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_TASK_TIMEOUT);
            }
        } catch (IOException ex) {
            BranchLogger.v("Branch connect exception: " + ex.getMessage());
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private BranchResponse doRestfulPost(String url, JSONObject payload, int retryNumber) throws BranchRemoteException {
        HttpsURLConnection connection = null;
        PrefHelper prefHelper = PrefHelper.getInstance(branch.getApplicationContext());
        int timeout = prefHelper.getTimeout();
        int connectTimeout = prefHelper.getConnectTimeout();

        try {
            payload.put(RETRY_NUMBER, retryNumber);
        } catch (JSONException e) {
            BranchLogger.d(Objects.requireNonNull(e.getMessage()));
        }
        try {
            // set the setThreadStatsTag for POST if API 26+
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                TrafficStats.setThreadStatsTag(THREAD_TAG_POST);
            }

            URL urlObject = new URL(url);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(timeout);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            if (url.contains(Defines.Jsonkey.QRCodeTag.getKey())) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Accept", "image/*");
            } else {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
             }
            connection.setRequestMethod("POST");

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());

            outputStreamWriter.write(payload.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            String requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.getKey());

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
                        if (url.contains(Defines.Jsonkey.QRCodeTag.getKey())) {
                            // Converting binary data to Base64
                            InputStream inputStream = connection.getInputStream();
                            Bitmap bmp = BitmapFactory.decodeStream(inputStream);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] b = baos.toByteArray();
                            String bmpString = Base64.encodeToString(b, Base64.DEFAULT);

                            result = new BranchResponse(bmpString, responseCode);
                        } else {
                            result = new BranchResponse(getResponseString(connection.getInputStream()), responseCode);
                        }
                    }

                } catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    BranchLogger.v("A resource conflict occurred with this request " + url);
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
        } catch(InterruptedIOException ex){
            // When the thread times out before or while sending the request
            if (retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            } else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_TASK_TIMEOUT);
            }
        }
        // Unable to resolve host/Unknown host exception
        catch (IOException ex) {
            if (retryNumber < prefHelper.getRetryCount()) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY);
            }
        } catch (Exception ex) {
            BranchLogger.v("Exception: " + ex.getMessage());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                if (ex instanceof NetworkOnMainThreadException)
                    BranchLogger.v("Branch Error: Don't call our synchronous methods on the main thread!!!");
            }
            return new BranchResponse(null, 500);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getResponseString(InputStream inputStream) {
        String responseString = null;
        if (inputStream != null) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            try {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                responseString = sb.toString();
            } catch (IOException e) {
                BranchLogger.d(Objects.requireNonNull(e.getMessage()));
            }
        }
        return responseString;
    }

}
