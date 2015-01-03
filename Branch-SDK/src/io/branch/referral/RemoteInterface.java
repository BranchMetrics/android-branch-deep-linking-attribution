package io.branch.referral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteInterface {
	public static final String NO_TAG_VALUE = "no_tag";
	public static final int NO_CONNECTIVITY_STATUS = -1009;

	private static final String SDK_VERSION = "1.2.6";
	private static final int DEFAULT_TIMEOUT = 3000;
	
	private HttpClient getGenericHttpClient(int timeout) {
		if (timeout <= 0)
			timeout = DEFAULT_TIMEOUT;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
		HttpConnectionParams.setSoTimeout(httpParams, timeout);
		return new DefaultHttpClient(httpParams);
	}
	
	private ServerResponse processEntityForJSON (HttpEntity entity, int statusCode, String tag, boolean log) {
		ServerResponse result = new ServerResponse(tag, statusCode);
		try {
			if (entity != null) {
		    	InputStream instream = entity.getContent();
		    	BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
		    	
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
		    				if (log) PrefHelper.Debug(getClass().getSimpleName(), "JSON exception: " + ex2.getMessage());
		    			}
		    		}
			    } 
		    }
	   	} catch (IOException ex) { 
	   		if (log) PrefHelper.Debug(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
		}
		
		return result;
	}
	
	public ServerResponse make_restful_get(String url, String tag, int timeout) {
		return make_restful_get(url, tag, timeout, true);
	}
	
	public ServerResponse make_restful_get(String url, String tag, int timeout, boolean log) {
		try {    	
			if (url.indexOf('?') == -1) {
				url += "?sdk=android" + SDK_VERSION;
			} else {
				url += "&sdk=android" + SDK_VERSION;
			}
			if (log) PrefHelper.Debug("BranchSDK", "getting " + url);
		    HttpGet request = new HttpGet(url);
		    HttpResponse response = getGenericHttpClient(timeout).execute(request);
		    return processEntityForJSON(response.getEntity(), response.getStatusLine().getStatusCode(), tag, log);

		} catch (ClientProtocolException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Client protocol exception: " + ex.getMessage());
		} catch (SocketException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (UnknownHostException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (IOException ex) { 
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "IO exception: " + ex.getMessage());
			return new ServerResponse(tag, 500);
		}
		return null;
	}

	public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout) {
		return make_restful_post(body, url, tag, timeout, true);
	}

	public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout, boolean log) {
		try {    	
			body.put("sdk", "android" + SDK_VERSION);
			if (log) {
				PrefHelper.Debug("BranchSDK", "posting to " + url);
				PrefHelper.Debug("BranchSDK", "Post value = " + body.toString());
			}
		    HttpPost request = new HttpPost(url);
		    request.setEntity(new ByteArrayEntity(body.toString().getBytes("UTF8")));
		    request.setHeader("Content-type", "application/json");
		    HttpResponse response = getGenericHttpClient(timeout).execute(request);
		    return processEntityForJSON(response.getEntity(), response.getStatusLine().getStatusCode(), tag, log);

		} catch (SocketException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (UnknownHostException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (Exception ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Exception: " + ex.getMessage());
			return new ServerResponse(tag, 500);
		}
	}
}
