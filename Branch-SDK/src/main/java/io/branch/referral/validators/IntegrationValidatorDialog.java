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

    IntegrationValidatorDialogRowItem test1RowItem;
    IntegrationValidatorDialogRowItem test2RowItem;
    IntegrationValidatorDialogRowItem test3RowItem;
    IntegrationValidatorDialogRowItem test4RowItem;
    IntegrationValidatorDialogRowItem test5RowItem;
    IntegrationValidatorDialogRowItem test6RowItem;
    IntegrationValidatorDialogRowItem test7RowItem;
    IntegrationValidatorDialogRowItem test8RowItem;

    Button exportLogsButton;
    Button testDeepLinkingButton;

    public IntegrationValidatorDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_integration_validator);
        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
        sdkVersionTextView.setText("SDK Version: " + Branch.getSdkVersionNumber());

        test1RowItem = findViewById(R.id.test_1_auto_instance_validator_row);
        test2RowItem = findViewById(R.id.test_2_verify_branch_keys);
        test3RowItem = findViewById(R.id.test_3_verify_package_name);
        test4RowItem = findViewById(R.id.test_4_verify_uri_scheme);
        test5RowItem = findViewById(R.id.test_5_verify_app_links);
        test6RowItem = findViewById(R.id.test_6_verify_custom_domain);
        test7RowItem = findViewById(R.id.test_7_domain_intent_filters);
        test8RowItem = findViewById(R.id.test_8_alternate_domain_intent_filters);

        exportLogsButton = findViewById(R.id.export_logs_button);
        testDeepLinkingButton = findViewById(R.id.test_deep_linking_button);

        exportLogsButton.setOnClickListener(view -> {
            shareLogsAsText(context);
        });

        testDeepLinkingButton.setOnClickListener(view -> {
            LinkingValidator.validate(context);
        });
    }

    public void setTestResult(int testNumber, String name, boolean didTestPass, String detailsMessage, String moreInfoLink) {
        switch (testNumber) {
            case 1:
                setResult(test1RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 2:
                setResult(test2RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 3:
                setResult(test3RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 4:
                setResult(test4RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 5:
                setResult(test5RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 6:
                setResult(test6RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 7:
                setResult(test7RowItem, name, didTestPass, detailsMessage, moreInfoLink);
                break;
            case 8:
                setResult(test8RowItem, name, didTestPass, detailsMessage, moreInfoLink);
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
