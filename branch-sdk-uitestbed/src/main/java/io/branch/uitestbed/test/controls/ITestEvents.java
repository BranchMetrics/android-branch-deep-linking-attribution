package io.branch.uitestbed.test.controls;

import org.json.JSONObject;

import io.branch.uitestbed.test.data.TestResponse;

public interface ITestEvents {
    void onTestStart(String message);
    void onTestResponse(TestResponse resp);
}
