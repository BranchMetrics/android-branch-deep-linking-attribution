package io.branch.referral;

import org.json.JSONObject;

public interface NetworkCallback {
	public void finished(JSONObject serverResponse);
}
