package io.branch.referral.validators;

import static io.branch.referral.validators.IntegrationValidatorConstants.appLinksMoreInfoDocsLink;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

public class AppLinksCheck extends IntegrationValidatorCheck {

    String name = "App Links";
    String errorMessage = "Could not find any App Link hosts to support Android AppLinks. Please add intent filter for handling AppLinks in your Android Manifest file";
    String moreInfoLink = appLinksMoreInfoDocsLink;
    BranchIntegrationModel integrationModel;
    JSONObject branchAppConfig;

    public AppLinksCheck(BranchIntegrationModel integrationModel, JSONObject branchAppConfig) {
        super.name = name;
        super.errorMessage = errorMessage;
        super.moreInfoLink = moreInfoLink;
        this.integrationModel = integrationModel;
        this.branchAppConfig = branchAppConfig;
    }

    @Override
    public boolean RunTests(Context context) {
        String defAppLinkDomain = branchAppConfig.optString("default_short_url_domain");
        return !integrationModel.applinkScheme.isEmpty() && !TextUtils.isEmpty(defAppLinkDomain) && checkIfIntentAddedForLinkDomain(defAppLinkDomain);
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }

    private boolean checkIfIntentAddedForLinkDomain(String domainName) {
        boolean foundIntentFilterMatchingDomainName = false;
        if (!TextUtils.isEmpty(domainName) && integrationModel.applinkScheme != null) {
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
