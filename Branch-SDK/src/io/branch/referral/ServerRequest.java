package io.branch.referral;

import java.io.Serializable;

import org.json.JSONObject;

public class ServerRequest implements Serializable {
	private static final long serialVersionUID = -160195137341529315L;
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
