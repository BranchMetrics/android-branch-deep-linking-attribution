package io.branch.referral.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.os.NetworkOnMainThreadException;
import android.util.Base64;

import androidx.annotation.NonNull;

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
        this.prefHelper = PrefHelper.getInstance(branch.getApplicationContext());
        this.retryLimit = prefHelper.getRetryCount();
    }

    @Override
    public BranchResponse doRestfulGet(String url) throws BranchRemoteException {
        return doRestfulGet(url, 0);
    }

    @Override
    public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
        return doRestfulPost(url, payload, 0);
    }

    private int lastResponseCode = -1;
    private String lastResponseMessage = "";
    private String lastRequestId = "";
    private PrefHelper prefHelper;
    private int retryLimit;



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
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
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
                    BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
                    result = new BranchResponse(null, responseCode);
                }
                result.requestId = Strings.emptyToNull(requestId);
                return result;
            }
        }
        catch (SocketException ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY, ex.getMessage());
        }
        catch (SocketTimeoutException ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_REQ_TIMED_OUT, ex.getMessage());
            }
        }
        catch(InterruptedIOException ex){
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));

            // When the thread times out before or while sending the request
            if (retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulGet(url, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_TASK_TIMEOUT, ex.getMessage());
            }
        }
        catch (IOException ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY, ex.getMessage());
        }
        finally {
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
        }
        catch (JSONException e) {
            BranchLogger.e("Caught JSONException, retry number: " + retryNumber + " " + e.getMessage() + " stacktrace " + BranchLogger.stackTraceToString(e));
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
            }
            else {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
             }
            connection.setRequestMethod("POST");

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());

            outputStreamWriter.write(payload.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            String requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.getKey());
            lastRequestId = requestId;

            int responseCode = connection.getResponseCode();
            lastResponseCode = responseCode;
            lastResponseMessage = connection.getResponseMessage(); // If we have the response code, this will not invoke any more data transfer
            BranchLogger.d("lastResponseMessage " + lastResponseMessage);

            if (responseCode >= HttpsURLConnection.HTTP_INTERNAL_ERROR
                    && retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            }
            else {
                BranchResponse result;
                try {
                    if (responseCode != HttpsURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        BranchLogger.e("Branch Networking Error: " +
                                "\nURL: " + url + "" +
                                "\nResponse Code: " + lastResponseCode +
                                "\nResponse Message: " + lastResponseMessage +
                                "\nRetry number: " + retryNumber +
                                "\nFinal attempt: true" + // no retry on 4XX errors
                                "\nrequestId: " + lastRequestId +
                                "\nObject: " + this );
                        result = new BranchResponse(getResponseString(connection.getErrorStream()), responseCode);
                    }
                    else {
                        if (url.contains(Defines.Jsonkey.QRCodeTag.getKey())) {
                            // Converting binary data to Base64
                            InputStream inputStream = connection.getInputStream();
                            Bitmap bmp = BitmapFactory.decodeStream(inputStream);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] b = baos.toByteArray();
                            String bmpString = Base64.encodeToString(b, Base64.DEFAULT);

                            result = new BranchResponse(bmpString, responseCode);
                        }
                        else {
                            result = new BranchResponse(getResponseString(connection.getInputStream()), responseCode);
                            BranchLogger.v("Branch Networking Success" +
                                            "\nURL: " + url + "" +
                                            "\nResponse Code: " + lastResponseCode +
                                            "\nResponse Message: " + lastResponseMessage +
                                            "\nRetry number: " + retryNumber +
                                            "\nrequestId: " + lastRequestId +
                                            "\nObject: " + this );
                        }
                    }
                }
                catch (FileNotFoundException ex) {
                    // In case of Resource conflict getInputStream will throw FileNotFoundException. Handle it here in order to send the right status code
                    BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
                    result = new BranchResponse(null, responseCode);
                }

                result.requestId = requestId;
                return result;
            }


        } catch (SocketTimeoutException ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            // On socket  time out retry the request for retryNumber of times
            if (retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_REQ_TIMED_OUT, ex.getMessage());
            }
        }
        catch(InterruptedIOException ex){
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            // When the thread times out before or while sending the request
            if (retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_TASK_TIMEOUT, ex.getMessage());
            }
        }
        // Unable to resolve host/Unknown host exception
        catch (IOException ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            if (retryNumber < retryLimit) {
                try {
                    Thread.sleep(prefHelper.getRetryInterval());
                }
                catch (InterruptedException e) {
                    BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber));
                }
                retryNumber++;
                return doRestfulPost(url, payload, retryNumber);
            }
            else {
                throw new BranchRemoteException(BranchError.ERR_BRANCH_NO_CONNECTIVITY, ex.getMessage());
            }
        }
        catch (Exception ex) {
            BranchLogger.e(getNetworkErrorMessage(ex, url, retryNumber));
            if (ex instanceof NetworkOnMainThreadException) {
                BranchLogger.e("Cannot make network request on main thread.");
                throw new BranchRemoteException((BranchError.ERR_NETWORK_ON_MAIN), ex.getMessage());
            }
            throw new BranchRemoteException(BranchError.ERR_OTHER, ex.getMessage());
        }
        finally {
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
                BranchLogger.d(e.getMessage());
            }
        }
        return responseString;
    }

    String getNetworkErrorMessage(Exception e, String url, int retry){
        return "Branch Networking Error: " +
                "\nURL: " + url + "" +
                "\nResponse Code: " + lastResponseCode +
                "\nResponse Message: " + lastResponseMessage +
                "\nCaught exception type: " + e.getClass().getCanonicalName() +
                "\nRetry number: " + retry +
                "\nrequestId: " + lastRequestId +
                "\nFinal attempt: " + (retry >= retryLimit) +
                "\nObject: " + this +
                "\nException Message: " + e.getMessage() +
                "\nStacktrace: " + BranchLogger.stackTraceToString(e);
    }
}
