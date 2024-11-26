package io.branch.referral.validators;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import io.branch.referral.Branch;
import io.branch.referral.R;

public class IntegrationValidatorDialog extends Dialog {

    IntegrationValidatorDialogRowItem autoInstanceValidatorRowItem;
    IntegrationValidatorDialogRowItem verifyBranchKeysRowItem;
    IntegrationValidatorDialogRowItem verifyPackageNameRowItem;
    IntegrationValidatorDialogRowItem verifyURISchemeRowItem;
    IntegrationValidatorDialogRowItem verifyAppLinksRowItem;
    IntegrationValidatorDialogRowItem verifyCustomDomainRowItem;
    IntegrationValidatorDialogRowItem domainIntentFiltersRowItem;
    IntegrationValidatorDialogRowItem alternateDomainIntentFiltersRowItem;

    Button exportLogsButton;
    Button testDeepLinkingButton;

    public IntegrationValidatorDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_integration_validator);
        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
        sdkVersionTextView.setText("SDK Version: " + Branch.getSdkVersionNumber());

        autoInstanceValidatorRowItem = findViewById(R.id.test_1_auto_instance_validator_row);
        verifyBranchKeysRowItem = findViewById(R.id.test_2_verify_branch_keys);
        verifyPackageNameRowItem = findViewById(R.id.test_3_verify_package_name);
        verifyURISchemeRowItem = findViewById(R.id.test_4_verify_uri_scheme);
        verifyAppLinksRowItem = findViewById(R.id.test_5_verify_app_links);
        verifyCustomDomainRowItem = findViewById(R.id.test_6_verify_custom_domain);
        domainIntentFiltersRowItem = findViewById(R.id.test_7_domain_intent_filters);
        alternateDomainIntentFiltersRowItem = findViewById(R.id.test_8_alternate_domain_intent_filters);

        exportLogsButton = findViewById(R.id.export_logs_button);
        testDeepLinkingButton = findViewById(R.id.test_deep_linking_button);

        exportLogsButton.setOnClickListener(view -> {
            shareLogsAsText(context);
        });

        testDeepLinkingButton.setOnClickListener(view -> {
            LinkingValidator.validate(context);
        });
    }

    public void SetTestResultForRowItem(int testNumber, String name, boolean didTestPass, String detailsMessage, String moreInfoLink) {
        switch (testNumber) {
            case 1:
                setResult(autoInstanceValidatorRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 2:
                setResult(verifyBranchKeysRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 3:
                setResult(verifyPackageNameRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 4:
                setResult(verifyURISchemeRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 5:
                setResult(verifyAppLinksRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 6:
                setResult(verifyCustomDomainRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 7:
                setResult(domainIntentFiltersRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 8:
                setResult(alternateDomainIntentFiltersRowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
        }
    }

    private void setResult(IntegrationValidatorDialogRowItem rowItem, String name, boolean didTestPass, String detailsMessage, String moreInfoLink) {
        rowItem.SetTitleText(name);
        rowItem.SetTestResult(didTestPass);
        rowItem.SetDetailsMessage(detailsMessage);
        rowItem.SetMoreInfoLink(moreInfoLink);
    }

    private void shareLogsAsText(Context context) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, IntegrationValidator.getLogs());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            context.startActivity(shareIntent);
    }
}
