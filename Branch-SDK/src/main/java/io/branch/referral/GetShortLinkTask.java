package io.branch.referral;

import android.os.AsyncTask;

/**
 * Async Task to create  a short link for synchronous methods
 */
class GetShortLinkTask extends AsyncTask<ServerRequest, Void, ServerResponse> {
    private final Branch branch;

    public GetShortLinkTask(Branch branch) {
        this.branch = branch;
    }

    @Override
    protected ServerResponse doInBackground(ServerRequest... serverRequests) {
        return branch.branchRemoteInterface_.make_restful_post(serverRequests[0].getPost(), branch.prefHelper_.getAPIBaseUrl() + Defines.RequestPath.GetURL.getPath(), Defines.RequestPath.GetURL.getPath(), branch.prefHelper_.getBranchKey());
    }
}
