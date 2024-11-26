package io.branch.referral.validators;

import static io.branch.referral.validators.IntegrationValidatorConstants.uriSchemeAppMoreInfoDocsLink;
import static io.branch.referral.validators.IntegrationValidatorConstants.uriSchemeDashboardMoreInfoDocsLink;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class URISchemeCheck extends IntegrationValidatorCheck {

    String name = "URI Scheme";

    String uriSchemeNotSetInManifestErrorMessage = "No intent found for opening the app through uri Scheme. Please add the intent with URI scheme to your Android manifest.";
    String uriSchemeNotSetInBranchDashboardErrorMessage = "Uri Scheme to open your app is not specified in Branch dashboard. Please add URI scheme in Branch dashboard.";
    String uriSchemesDoNotMatchErrorMessage = "Uri scheme specified in Branch dashboard doesn't match with the deep link intent in manifest file.";

    String moreInfoLinkApp = uriSchemeAppMoreInfoDocsLink;
    String moreInfoLinkDashboard = uriSchemeDashboardMoreInfoDocsLink;

    BranchIntegrationModel integrationModel;
    JSONObject branchAppConfig;

    public URISchemeCheck(BranchIntegrationModel integrationModel, JSONObject branchAppConfig) {
        super.name = name;
        super.errorMessage = ""; //defaulting to first check so it isn't empty before running tests
        super.moreInfoLink = moreInfoLinkApp; //defaulting to first check so it isn't empty before running tests
        this.integrationModel = integrationModel;
        this.branchAppConfig = branchAppConfig;
    }

    @Override
    public boolean RunTests(Context context) {
        String branchAppUriScheme = branchAppConfig.optString("android_uri_scheme").substring(0, branchAppConfig.optString("android_uri_scheme").length() - 3);
        String dashboardUriScheme = String.valueOf(integrationModel.deeplinkUriScheme.keys().next());
        boolean isUriSchemeProperlySetOnDashboard = !TextUtils.isEmpty(branchAppUriScheme);
        boolean isUriSchemeIntentProperlySetup = checkIfIntentAddedForURIScheme(branchAppConfig.optString("android_uri_scheme")) && integrationModel.appSettingsAvailable;
        boolean doUriSchemesMatch = branchAppUriScheme.trim().equals(dashboardUriScheme.trim());

        if(!isUriSchemeProperlySetOnDashboard) {
            super.errorMessage = uriSchemeNotSetInBranchDashboardErrorMessage;
            super.moreInfoLink = moreInfoLinkDashboard;
        }
        else if(!isUriSchemeIntentProperlySetup) {
            super.errorMessage = uriSchemeNotSetInManifestErrorMessage;
            super.moreInfoLink = moreInfoLinkApp;
        }
        else if(!doUriSchemesMatch) {
            super.errorMessage = uriSchemesDoNotMatchErrorMessage;
            super.moreInfoLink = moreInfoLinkApp;
        }

        return doUriSchemesMatch && isUriSchemeProperlySetOnDashboard && isUriSchemeIntentProperlySetup;
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }

    private boolean checkIfIntentAddedForURIScheme(String uriScheme) {
        Uri branchDeepLinkURI = Uri.parse(uriScheme);
        String uriHost = branchDeepLinkURI.getScheme();
        String uriPath = branchDeepLinkURI.getHost();
        uriPath = TextUtils.isEmpty(uriPath) ? "open" : uriPath;
        boolean foundMatchingUri = false;
        if (integrationModel.deeplinkUriScheme != null) {
            for (Iterator<String> it = integrationModel.deeplinkUriScheme.keys(); it.hasNext(); ) {
                String key = it.next();
                if (uriHost != null && uriHost.equals(key)) {
                    JSONArray hosts = integrationModel.deeplinkUriScheme.optJSONArray(key);
                    if (hosts != null && hosts.length() > 0) {
                        for (int i = 0; i < hosts.length(); ++i) {
                            if (uriPath != null && uriPath.equals(hosts.optString(i))) {
                                foundMatchingUri = true;
                                break;
                            }
                        }
                    } else {
                        foundMatchingUri = true;
                        break;
                    }
                }
            }
        }
        return foundMatchingUri;
    }
}
