package io.branch.referral.validators;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;

import io.branch.referral.R;

public class IntegrationValidatorDialogRowItem extends LinearLayout {
    TextView titleText;
    TextView testResultSymbol;
    Button detailsButton;
    String detailsMessage;
    String moreInfoLink;

    public IntegrationValidatorDialogRowItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        View view =  LayoutInflater.from(getContext()).inflate(
                R.layout.integration_validator_dialog_row_item, null);
        this.addView(view);
        titleText = view.findViewById(R.id.title_text);
        testResultSymbol = view.findViewById(R.id.pass_or_fail_symbol_text);
        detailsButton = view.findViewById(R.id.details_button);
        detailsButton.setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(detailsMessage + "\n");
            TextView hyperlinkToDocs = new TextView(context);
            hyperlinkToDocs.setMovementMethod(LinkMovementMethod.getInstance());
            hyperlinkToDocs.setGravity(Gravity.CENTER_HORIZONTAL);
            String link = "<a href="  + moreInfoLink + "</a>";
            hyperlinkToDocs.setText(Html.fromHtml(link));
            builder.setView(hyperlinkToDocs);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", (dialog, which) -> {});
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }

    public IntegrationValidatorDialogRowItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void SetTitleText(String title) {
        titleText.setText(title);
    }

    public void SetDetailsMessage(String detailsMessage) {
        this.detailsMessage = detailsMessage;
    }

    public void SetMoreInfoLink(String moreInfoLink) {
        this.moreInfoLink = moreInfoLink;
    }

    public void SetTestResult(boolean didTestPass) {
        String result = didTestPass ? IntegrationValidatorConstants.checkmark : IntegrationValidatorConstants.xmark;
        testResultSymbol.setText(result);
        ToggleDetailsButton(didTestPass);
    }

    public void ToggleDetailsButton(boolean didTestPass) {
        if (didTestPass) {
            detailsButton.setVisibility(INVISIBLE);
        } else {
            detailsButton.setVisibility(VISIBLE);
        }
    }
}
