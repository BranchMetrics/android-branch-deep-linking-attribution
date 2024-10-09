package io.branch.referral.validators;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import io.branch.referral.R;

public class LinkingValidatorDialog extends Dialog {

    public LinkingValidatorDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_linking_validator);
    }
}
