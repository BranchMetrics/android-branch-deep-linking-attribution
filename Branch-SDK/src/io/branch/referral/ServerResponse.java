package io.branch.referral;

import org.json.JSONArray;
import org.json.JSONObject;

public class ServerResponse {
	private int statusCode_;
	private String tag_;
	private Object post_;
	
	public ServerResponse(String tag, int statusCode) {
		tag_ = tag;
		statusCode_ = statusCode;
	}
	
	public String getTag() {
		return tag_;
	}
	
	public int getStatusCode() {
		return statusCode_;
	}
	
	public void setPost(Object post) {
		post_ = post;
	}
	
	public JSONObject getObject() {
		if (post_ instanceof JSONObject) {
			return (JSONObject)post_;
		}
		
		return null;
	}
	
	public JSONArray getArray() {
		if (post_ instanceof JSONArray) {
			return (JSONArray)post_;
		}
		
		return null;
	}
}
