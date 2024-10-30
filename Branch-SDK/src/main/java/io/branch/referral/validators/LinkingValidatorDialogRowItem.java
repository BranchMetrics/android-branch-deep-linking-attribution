package io.branch.referral.validators;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.branch.referral.R;

public class LinkingValidatorDialogRowItem extends LinearLayout {

    Context context;
    TextView titleText;
    Button infoButton;
    String infoText;

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public void InitializeRow(String title, String infoText) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.linking_validator_dialog_row_item, null);
        this.addView(view);
        titleText = view.findViewById(R.id.linkingValidatorRowTitleText);
        infoButton = view.findViewById(R.id.linkingValidatorRowInfoButton);
        titleText.setText(title);
        this.infoText = infoText;
        infoButton.setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(infoText).setTitle(titleText.getText());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }
}
