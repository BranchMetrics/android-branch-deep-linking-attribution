package io.branch.referral.validators;

import android.content.Context;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

class ServerRequestGetAppConfig extends ServerRequest {
    private final IGetAppConfigEvents callback;

    public ServerRequestGetAppConfig(Context context, IGetAppConfigEvents callback) {
        super(context, Defines.RequestPath.GetApp);
        this.callback = callback;
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        if (callback != null) {
            callback.onAppConfigAvailable(response.getObject());
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback != null) {
            callback.onAppConfigAvailable(null);
        }
    }

    @Override
    public boolean isGetRequest() {
        return true;
    }

    @Override
    public String getRequestUrl() {
        return prefHelper_.getAPIBaseUrl() + getRequestPath() + "/" + prefHelper_.getBranchKey();
    }

    @Override
    public void clearCallbacks() {

    }

    public interface IGetAppConfigEvents {
        void onAppConfigAvailable(JSONObject obj);
    }
}