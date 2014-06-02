package io.bloom.referral;

import org.json.JSONObject;

public class ServerRequest {
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
}
