package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequest {
	private static final String TAG_KEY = "REQ_TAG";
	private static final String POST_KEY = "REQ_POST";
	
	private String tag_;
	private JSONObject post_;
	
	public ServerRequest(String tag, JSONObject post) {
		tag_ = tag;
		post_ = post;
	}
	
	public String getTag() {
		return tag_;
	}
	
	public JSONObject getPost() {
		return post_;
	}
	
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
		
		if (tag != null && !tag.isEmpty()) {
			return new ServerRequest(tag, post);
		}
		
		return null;
	}
}
