package io.branch.uitestbed.test.controls;

import org.json.JSONObject;

public interface ITestEvents {
    void onTestStart(String message);
    void onTestResponse(JSONObject resp);
}
