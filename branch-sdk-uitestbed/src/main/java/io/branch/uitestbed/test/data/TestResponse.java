package io.branch.uitestbed.test.data;

import io.branch.referral.BranchError;

public class TestResponse {
    private final boolean isSucceeded;
    private final BranchError branchError;

    private static final int UNKNOWN_ERR = -200;

    public static TestResponse getSuccessResponse() {
        return new TestResponse(true);
    }

    public static TestResponse getFailedResponse(BranchError err) {
        return new TestResponse(err);
    }

    public static TestResponse getFailedResponseUnknownError(String message) {
        return new TestResponse(new BranchError(message, UNKNOWN_ERR));
    }

    private TestResponse(boolean isSucceeded) {
        this.isSucceeded = isSucceeded;
        this.branchError = null;
    }

    private TestResponse(BranchError branchError) {
        this.isSucceeded = false;
        this.branchError = branchError;
    }

    @Override
    public String toString() {
        String testResp;
        if (isSucceeded) {
            testResp = "Passed : OK";
        } else {
            testResp = "Failed : " + this.branchError.getMessage();
        }
        return testResp;
    }
}
