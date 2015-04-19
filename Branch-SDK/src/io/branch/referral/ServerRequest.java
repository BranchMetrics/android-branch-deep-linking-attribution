	package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for all Branch API server requests.
 */
public class ServerRequest {
	private static final String TAG_KEY = "REQ_TAG";
	private static final String POST_KEY = "REQ_POST";
	
	private String tag_;
	private JSONObject post_;
	
	/**
	 * <p>Constructor class for instantiating a {@link ServerRequest} prior to adding JSON data.</p>
	 * 
	 * @param tag		A {@link String} tag to apply to current request.
	 */
	public ServerRequest(String tag) {
		this(tag, null);
	}
	
	/**
	 * <p>Constructor class for instantiating a {@link ServerRequest}, with a {@link JSONObject} 
	 * supplied containing post data in key-value pairs.</p>
	 * 
	 * @param tag		A {@link String} tag to apply to current request.
	 * 
	 * @param post		A {@link JSONObject} supplied containing post data in key-value pairs.
	 */
	public ServerRequest(String tag, JSONObject post) {
		tag_ = tag;
		post_ = post;
	}
	
	/**
	 * <p>Returns a {@link String} value containing the tag assigned to this {@link ServerRequest}.</p>
	 * 
	 * @return		A {@link String} value containing the tag.
	 */
	public String getTag() {
		return tag_;
	}
	
	/**
	 * <p>Gets a {@link JSONObject} containing the post data supplied with the current request as 
	 * key-value pairs.</p>
	 * 
	 * @return		A {@link JSONObject} containing the post data supplied with the current request 
	 * 				as key-value pairs.
	 */
	public JSONObject getPost() {
		return post_;
	}
	
	/**
	 * <p>Gets a {@link JSONObject} containing the values of {@link ServerRequest#TAG_KEY} and 
	 * {@link ServerRequest#POST_KEY} as currently configured.</p>
	 * 
	 * @return		A {@link JSONObject} containing the values of {@link ServerRequest#TAG_KEY} and 
	 * 				{@link ServerRequest#POST_KEY} as currently configured, or <i>null</i> if 
	 * 				one or both of those values have not yet been set.
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put(TAG_KEY, tag_);
			json.put(POST_KEY, post_);
		} catch (JSONException e) {
			return null;
		}
		return json;
	}
	
	/**
	 * <p>Converts a {@link JSONObject} object containing keys stored as key-value pairs into 
	 * a {@link ServerRequest}.</p>
	 * 
	 * @param json		A {@link JSONObject} object containing post data stored as key-value pairs
	 * 
	 * @return			A {@link ServerRequest} object with the {@link #POST_KEY} and {@link #TAG_KEY} 
	 * 					values set if not null; this can be one or the other. If both values in the 
	 * 					supplied {@link JSONObject} are null, null is returned instead of an object.
	 */
	public static ServerRequest fromJSON(JSONObject json) {
		String tag;
		JSONObject post = null;
		
		try {
			if (json.has(TAG_KEY)) {
				tag = json.getString(TAG_KEY);
			} else {
				return null;
			}
		} catch (JSONException e) {
			return null;
		}
		
		try {
			if (json.has(POST_KEY)) {
				post = json.getJSONObject(POST_KEY);
			}
		} catch (JSONException e) {
			// it's OK for post to be null
		}
		
		if (tag != null && tag.length() > 0) {
			return new ServerRequest(tag, post);
		}
		
		return null;
	}
}
