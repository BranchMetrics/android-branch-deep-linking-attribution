package io.branch.referral.validators;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.branch.referral.R;

public class LinkingValidatorDialogRowItem extends LinearLayout {

    TextView titleText;

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.linking_validator_dialog_row_item, null);
        this.addView(view);
        titleText = view.findViewById(R.id.linkingValidatorRowTitleText);
    }

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void SetTitleText(String title) {
        titleText.setText(title);
    }
}
