package io.branch.referral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RemoteInterface {
	public static final String KEY_SERVER_CALL_TAG = "server_call_tag";
	public static final String KEY_SERVER_CALL_STATUS_CODE = "httpcode";

	public static final String NO_IDENT_VALUE = "no_value";
	public static final String NO_TAG_VALUE = "no_tag";

	private HttpClient getGenericHttpClient() {
		int timeout = 10000;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
		HttpConnectionParams.setSoTimeout(httpParams, timeout);
		return new DefaultHttpClient(httpParams);
	}
	
	private JSONObject processEntityForJSON (HttpEntity entity, int statusCode, String tag) {
		JSONObject jsonreturn = new JSONObject();;
		try {
			jsonreturn.put(KEY_SERVER_CALL_STATUS_CODE, statusCode);
			jsonreturn.put(KEY_SERVER_CALL_TAG, tag);
			
			if (entity != null) {
		    	InputStream instream = entity.getContent();
		    	
		    	BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
		    	
		    	String line = rd.readLine();
				Log.i("KindredReferral", "returned " + line);

		    	if (line != null) {
		    		JSONObject tempJson = new JSONObject(line);	
		    		Iterator<?> keys = tempJson.keys();
		    		while (keys.hasNext()) {
		    			String key = (String)keys.next();
		    			jsonreturn.put(key, tempJson.get(key));
		    		}	
		    		return jsonreturn;
			    } 
		    }
		} catch (JSONException ex) {
	   		Log.i(getClass().getSimpleName(), "JSON exception: " + ex.getMessage());
	   	} catch (IOException ex) { 
    		Log.i(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
		}
		
		return jsonreturn;
	}
	
	public JSONObject make_restful_get(String url, String tag) {
		try {    	
			Log.i("KindredSDK", "getting " + url);
		    HttpGet request = new HttpGet(url);
		    HttpResponse response = getGenericHttpClient().execute(request);
		    return processEntityForJSON(response.getEntity(), response.getStatusLine().getStatusCode(), tag);
		} catch (ClientProtocolException ex) {
	    		Log.i(getClass().getSimpleName(), "Client protocol exception: " + ex.getMessage());
		} catch (IOException ex) { 
    		Log.i(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
    		JSONObject jsonreturn = new JSONObject();;
    		try {
				jsonreturn.put(KEY_SERVER_CALL_STATUS_CODE, 500);
				jsonreturn.put(KEY_SERVER_CALL_TAG, tag);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonreturn;
		}
		return null;
	}

	public JSONObject make_restful_post(JSONObject body, String url, String tag) {
		try {    	
			Log.i("KindredReferral", "posting to " + url);
		    HttpPost request = new HttpPost(url);
		    request.setEntity(new ByteArrayEntity(body.toString().getBytes("UTF8")));
		    request.setHeader("Content-type", "application/json");
		    HttpResponse response = getGenericHttpClient().execute(request);
		    return processEntityForJSON(response.getEntity(), response.getStatusLine().getStatusCode(), tag);
		} catch (Exception ex) {
			Log.i(getClass().getSimpleName(), "Exception: " + ex.getMessage());
			ex.printStackTrace();
			JSONObject jsonreturn = new JSONObject();	
			try {
				jsonreturn.put(KEY_SERVER_CALL_TAG, tag);
				jsonreturn.put(KEY_SERVER_CALL_STATUS_CODE, 500);
			} catch (JSONException e) {
				e.printStackTrace();
			}  
			return jsonreturn;
		}
	}
}
