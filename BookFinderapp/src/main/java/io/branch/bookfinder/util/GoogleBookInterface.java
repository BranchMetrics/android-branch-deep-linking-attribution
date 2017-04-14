package io.branch.bookfinder.util;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

import io.branch.bookfinder.IBookHandleEvents;

/**
 * Created by sojanpr on 8/9/16.
 */
public class GoogleBookInterface {

    private static GoogleBookInterface thisInstance_;
    final BookRequester bookRequester_;

    private GoogleBookInterface(Context context) {
        bookRequester_ = new BookRequester(context);
    }

    public static GoogleBookInterface getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new GoogleBookInterface(context);
        }
        return thisInstance_;
    }

    private BFBookResponse make_restful_get(String reqUrl) {

        HttpsURLConnection connection = null;

        try {
            URL urlObject = new URL(reqUrl);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);


            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {

                try {
                    if (responseCode != HttpURLConnection.HTTP_OK && connection.getErrorStream() != null) {
                        return processEntityForJSON(connection.getErrorStream(),
                                responseCode);
                    } else {
                        return processEntityForJSON(connection.getInputStream(),
                                responseCode);
                    }
                } catch (FileNotFoundException ex) {
                    return processEntityForJSON(null, responseCode);
                }
            }
        } catch (SocketException ex) {

        } catch (SocketTimeoutException ex) {

        } catch (UnknownHostException ex) {

        } catch (IOException ex) {

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return new BFBookResponse(500);

    }

    private BFBookResponse processEntityForJSON(InputStream inStream, int statusCode) {
        BFBookResponse result = new BFBookResponse(statusCode);
        try {
            if (inStream != null) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(inStream));
                String payload = "";
                String line;
                while ((line = rd.readLine()) != null) {
                    payload += line;
                }

                if (payload != null) {
                    try {
                        JSONObject jsonObj = new JSONObject(payload);
                        result.setBookResponse(jsonObj);
                    } catch (JSONException ex) {
                    }
                }
            }
        } catch (IOException ex) {
        }
        return result;
    }


    private class BookReqTask extends AsyncTask<String, Void, BFBookResponse> {
        private final IBookHandleEvents bookEvents_;
        private final int startIdx_;

        public BookReqTask(IBookHandleEvents bookEvents, int startIdx) {
            bookEvents_ = bookEvents;
            startIdx_ = startIdx;
        }

        @Override
        protected BFBookResponse doInBackground(String... params) {
            return make_restful_get(params[0]);
        }

        @Override
        protected void onPostExecute(BFBookResponse bfBookResponse) {
            super.onPostExecute(bfBookResponse);
            if (bookEvents_ != null) {
                bookEvents_.onBookResponseReceived(bfBookResponse, startIdx_);
            }
        }
    }


    public void getBooksForCategory(String categoryName, IBookHandleEvents callback) {
        String reqURl = "https://www.googleapis.com/books/v1/volumes?q=subject:" + categoryName;
        new BookReqTask(callback, 0).execute(reqURl);
    }

    public void searchBook(String searchString, int startIdx, int count, IBookHandleEvents callback) {
        String modifiedSearch = "";
        if (!TextUtils.isEmpty(searchString)) {
            if (searchString.contains(" ")) {
                String[] splits = searchString.split(" ");
                for (String split : splits) {
                    modifiedSearch += split;
                    modifiedSearch += "+";
                }
                modifiedSearch = modifiedSearch.substring(0, modifiedSearch.length() - 1);
            } else {
                modifiedSearch = searchString;
            }
        }


        String reqURl = "https://www.googleapis.com/books/v1/volumes?q=" + modifiedSearch + "&startIndex=" + startIdx + "&maxResults=" + count;
        //https://www.googleapis.com/books/v1/volumes?q=subject:adventure
        //new BookReqTask(callback, startIdx).execute(reqURl);
        bookRequester_.getBooks(callback, startIdx, reqURl);
    }


    private class BookRequester {
        private RequestQueue requestQueue;
        final String REQ_TAG = "GET_BOOK";

        public BookRequester(Context context) {
            requestQueue = Volley.newRequestQueue(context);
        }

        public void getBooks(final IBookHandleEvents callback, final int startIdx, String reqUrl) {
            JsonObjectRequest bookReq = new JsonObjectRequest(Request.Method.GET, reqUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (callback != null) {
                        BFBookResponse result = new BFBookResponse(200);
                        result.setBookResponse(jsonObject);
                        callback.onBookResponseReceived(result, startIdx);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });
            bookReq.setTag(REQ_TAG);
            requestQueue.cancelAll(REQ_TAG);
            requestQueue.add(bookReq);

        }

    }
}
