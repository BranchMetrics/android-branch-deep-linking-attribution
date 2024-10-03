package io.branch.referral.validators;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

public class CustomDomainCheck extends IntegrationValidatorCheck {

    String name = "Custom Domain";
    String errorMessage = "Could not find intent filter to support Branch default link domain. Please add intent filter for handling custom link domain in your Android Manifest file";
    String moreInfoLink = "<a href=\"https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app\">More info</a>";
    BranchIntegrationModel integrationModel;
    JSONObject branchAppConfig;

    public CustomDomainCheck(BranchIntegrationModel integrationModel, JSONObject branchAppConfig) {
        super.name = name;
        super.errorMessage = errorMessage;
        super.moreInfoLink = moreInfoLink;
        this.integrationModel = integrationModel;
        this.branchAppConfig = branchAppConfig;
    }

    @Override
    public boolean RunTests(Context context) {
        String customDomain = branchAppConfig.optString("short_url_domain");
        return TextUtils.isEmpty(customDomain) || checkIfIntentAddedForLinkDomain(customDomain);
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }

    private boolean checkIfIntentAddedForLinkDomain(String domainName) {
        boolean foundIntentFilterMatchingDomainName = false;
        if (!TextUtils.isEmpty(domainName)) {
            for (String host : integrationModel.applinkScheme) {
                if (domainName.equals(host)) {
                    foundIntentFilterMatchingDomainName = true;
                    break;
                }
            }
        }
        return foundIntentFilterMatchingDomainName;
    }
}
