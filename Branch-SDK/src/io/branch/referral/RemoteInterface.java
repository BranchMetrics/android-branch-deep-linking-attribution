package io.branch.referral;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

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

/**
 * <p>This class assists with RESTful calls to the Branch API, by encapsulating a generic 
 * {@link HttpClient} object, and handling all restful calls via one of its GET or POST capable 
 * methods.</p>
 */
public class RemoteInterface {
    public static final String BRANCH_KEY = "branch_key";
	public static final int NO_CONNECTIVITY_STATUS = -1009;
	public static final int NO_BRANCH_KEY_STATUS = -1234;

	private static final String SDK_VERSION = "1.5.11";
	private static final int DEFAULT_TIMEOUT = 3000;

	/**
	 * Required, default constructor for the class.
	 */
	public RemoteInterface() { }
	
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
	 * <p>Creates an instance of {@link HttpClient}, with a defined timeout, to be used for all of 
	 * the method calls within the class.</p>
	 * 
	 * @param timeout		An {@link Integer} value indicating the time in milliseconds to wait 
	 * 						before considering a request to have timed out.
	 * 
	 * @return				An instance of {@link HttpClient}, pre-configured with the timeout value 
	 * 						supplied.
	 */
	private HttpClient getGenericHttpClient(int timeout) {
		if (timeout <= 0)
			timeout = DEFAULT_TIMEOUT;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
		HttpConnectionParams.setSoTimeout(httpParams, timeout);
		return new DefaultHttpClient(httpParams);
	}
	
	/**
	 * <p>Converts a generic {@link HttpEntity} object into a {@link ServerResponse} object by 
	 * reading the content supplied in the raw server response, and creating a {@link JSONObject} 
	 * that contains the same data. This data is then attached as the post data of the 
	 * {@link ServerResponse} object returned.</p>
	 * 
	 * @param entity		A generic {@link HttpEntity} returned as a result of a HTTP response.
	 * 
	 * @param statusCode	An {@link Integer} value containing the HTTP response code.
	 * 
	 * @param tag			A {@link String} value containing the tag value to be applied to the 
	 * 						resultant {@link ServerResponse} object.
	 * 
	 * @param log			A {@link Boolean} value indicating whether or not to log the raw 
	 * 						content lines via the debug interface.
	 * 
	 * @param linkData		A {@link BranchLinkData} object containing the data dictionary associated 
	 * 						with the link subject of the original server request.
	 * 
	 * @return				A {@link ServerResponse} object representing the {@link HttpEntity} 
	 * 						response in Branch SDK terms.
	 * 
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1: Status Codes</a>
	 */
	private ServerResponse processEntityForJSON (HttpEntity entity, int statusCode, String tag, boolean log, BranchLinkData linkData) {
		ServerResponse result = new ServerResponse(tag, statusCode, linkData);
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

	/**
	 * <p>Make a RESTful GET request, by calling {@link #make_restful_get(String, String, int, int, boolean)}
	 * with the logging {@link Boolean} parameter pre-populated.</p>
	 * 
	 * @param url		A {@link String} URL to request from.
	 * 
	 * @param tag		A {@link String} tag for logging/analytics purposes.
	 * 
	 * @param timeout	An {@link Integer} value containing the number of milliseconds to wait 
	 * 					before considering a server request to have timed out.
	 * 
	 * @return			A {@link ServerResponse} object containing the result of the RESTful request.
	 */
	public ServerResponse make_restful_get(String url, String tag, int timeout) {
		return make_restful_get(url, tag, timeout, 0, true);
	}

	private boolean addCommonParams(JSONObject post, int retryNumber) {
		try {
			String branch_key = prefHelper_.getBranchKey();
			String app_key = prefHelper_.getAppKey();

			post.put("sdk", "android" + SDK_VERSION);
			post.put("retryNumber", retryNumber);
			if (!branch_key.equals(PrefHelper.NO_STRING_VALUE)) {
				post.put(BRANCH_KEY, prefHelper_.getBranchKey());
				return true;
			} else if (!app_key.equals(PrefHelper.NO_STRING_VALUE)) {
				post.put("app_id", prefHelper_.getAppKey());
				return true;
			}
		} catch (JSONException ignore) {
		}
		return false;
	}

    /**
     * <p>The main RESTful GET method; the other one ({@link #make_restful_get(String, String, int)}) calls this one
     * with a pre-populated logging parameter.</p>
     *
     * @param baseUrl		A {@link String} URL to request from.
     *
     * @param tag		A {@link String} tag for logging/analytics purposes.
     *
     * @param timeout	An {@link Integer} value containing the number of milliseconds to wait
     * 					before considering a server request to have timed out.
     *
     * @param log		A {@link Boolean} value that specifies whether debug logging should be
     * 					enabled for this request or not.
     *
     * @return			A {@link ServerResponse} object containing the result of the RESTful request.
     */
	private ServerResponse make_restful_get(String baseUrl, String tag, int timeout, int retryNumber, boolean log) {
		String modifiedUrl = baseUrl;
		JSONObject getParameters = new JSONObject();
		if (addCommonParams(getParameters, retryNumber)) {
            modifiedUrl += this.convertJSONtoString(getParameters);
        } else {
			return new ServerResponse(tag, NO_BRANCH_KEY_STATUS);
		}
		
		try {
			if (log) PrefHelper.Debug("BranchSDK", "getting " + modifiedUrl);
		    HttpGet request = new HttpGet(modifiedUrl);
		    HttpResponse response = getGenericHttpClient(timeout).execute(request);
			if (response.getStatusLine().getStatusCode() >= 500 &&
				retryNumber < prefHelper_.getRetryCount()) {
				try {
					Thread.sleep(prefHelper_.getRetryInterval());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				retryNumber++;
				return make_restful_get(baseUrl, tag, timeout, retryNumber, log);
			} else {
				return processEntityForJSON(response.getEntity(),
					response.getStatusLine().getStatusCode(), tag, log, null);
            }

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
    //endregion

	/**
	 * <p>Makes a RESTful POST call with logging enabled, without an associated data dictionary; 
	 * passed as null.</p>
	 * 
	 * @param body			A {@link JSONObject} containing the main data body/payload of the request.
	 * 
	 * @param url			A {@link String} URL to request from.
	 * 
	 * @param tag			A {@link String} tag for logging/analytics purposes.
	 * 
	 * @param timeout		An {@link Integer} value containing the number of milliseconds to wait 
	 * 						before considering a server request to have timed out.
	 * 
	 * @return				A {@link ServerResponse} object representing the {@link HttpEntity} 
	 * 						response in Branch SDK terms.
	 */
	public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout) {
		return make_restful_post(body, url, tag, timeout, 0, true, null);
	}
	
	/**
	 * <p>Makes a RESTful POST call with logging enabled.</p>
	 * 
	 * @param body			A {@link JSONObject} containing the main data body/payload of the request.
	 * 
	 * @param url			A {@link String} URL to request from.
	 * 
	 * @param tag			A {@link String} tag for logging/analytics purposes.
	 * 
	 * @param timeout		An {@link Integer} value containing the number of milliseconds to wait 
	 * 						before considering a server request to have timed out.
	 * 
	 * @param linkData		A {@link BranchLinkData} object containing the data dictionary associated 
	 * 						with the link subject of the original server request.
	 * 
	 * @return				A {@link ServerResponse} object representing the {@link HttpEntity} 
	 * 						response in Branch SDK terms.
	 */
	public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout, BranchLinkData linkData) {
		return make_restful_post(body, url, tag, timeout, 0, true, linkData);
	}

	/**
	 * <p>Makes a RESTful POST call without an associated data dictionary; passed as null.</p>
	 * 
	 * @param body			A {@link JSONObject} containing the main data body/payload of the request.
	 * 
	 * @param url			A {@link String} URL to request from.
	 * 
	 * @param tag			A {@link String} tag for logging/analytics purposes.
	 * 
	 * @param timeout		An {@link Integer} value containing the number of milliseconds to wait 
	 * 						before considering a server request to have timed out.
	 * 
	 * @param log			A {@link Boolean} value that specifies whether debug logging should be 
	 * 						enabled for this request or not.
	 * 
	 * @return				A {@link ServerResponse} object representing the {@link HttpEntity} 
	 * 						response in Branch SDK terms.
	 */
	public ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout, boolean log) {
		return make_restful_post(body, url, tag, timeout, 0, log, null);
	}
	

	/**
	 * <p>The main RESTful POST method. The others call this one with pre-populated parameters.</p>
	 * 
	 * @param body			A {@link JSONObject} containing the main data body/payload of the request.
	 * 
	 * @param url			A {@link String} URL to request from.
	 * 
	 * @param tag			A {@link String} tag for logging/analytics purposes.
	 * 
	 * @param timeout		An {@link Integer} value containing the number of milliseconds to wait 
	 * 						before considering a server request to have timed out.
	 * 
	 * @param log			A {@link Boolean} value that specifies whether debug logging should be 
	 * 						enabled for this request or not.
	 * 
	 * @param linkData		A {@link BranchLinkData} object containing the data dictionary associated 
	 * 						with the link subject of the original server request.
	 * 
	 * @return				A {@link ServerResponse} object representing the {@link HttpEntity} 
	 * 						response in Branch SDK terms.
	 */
    private ServerResponse make_restful_post(JSONObject body, String url, String tag, int timeout,
											int retryNumber, boolean log, BranchLinkData linkData) {
		try {
			JSONObject bodyCopy = new JSONObject();
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
		    HttpPost request = new HttpPost(url);
		    request.setEntity(new ByteArrayEntity(bodyCopy.toString().getBytes("UTF8")));
		    request.setHeader("Content-type", "application/json");
		    HttpResponse response = getGenericHttpClient(timeout).execute(request);
            if (response.getStatusLine().getStatusCode() >= 500
                    && retryNumber < prefHelper_.getRetryCount()) {
            	try {
					Thread.sleep(prefHelper_.getRetryInterval());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				retryNumber++;
				return make_restful_post(bodyCopy, url, tag, timeout, retryNumber, log, linkData);
			} else {
				ServerResponse serverResponse = processEntityForJSON(response.getEntity(), response.getStatusLine().getStatusCode(), tag, log, linkData);
				serverResponse.setRequestObject(body);
				return serverResponse;
			}
		} catch (SocketException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (UnknownHostException ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Http connect exception: " + ex.getMessage());
			return new ServerResponse(tag, NO_CONNECTIVITY_STATUS);
		} catch (Exception ex) {
			if (log) PrefHelper.Debug(getClass().getSimpleName(), "Exception: " + ex.getMessage());
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				if (ex instanceof NetworkOnMainThreadException)
					Log.i("BranchSDK", "Branch Error: Don't call our synchronous methods on the main thread!!!");
			}
			return new ServerResponse(tag, 500);
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
                for(int i = 0; i < size; i++) {
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
