package io.branch.referral.validators;

import android.content.Context;
import android.view.WindowManager;
import android.widget.Button;

import java.util.Objects;

public class LinkingValidator {
    private static LinkingValidator instance;
    private LinkingValidatorDialog linkingValidatorDialog;

    private LinkingValidator(Context context) {}

    public static void validate(Context context) {
        if (instance == null) {
            instance = new LinkingValidator(context);
        }
        instance.linkingValidatorDialog = new LinkingValidatorDialog(context);
        instance.validateDeepLinkRouting(context);
    }

    private void validateDeepLinkRouting(Context context) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(Objects.requireNonNull(instance.linkingValidatorDialog.getWindow()).getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = 1000;
        instance.linkingValidatorDialog.show();
        instance.linkingValidatorDialog.getWindow().setAttributes(lp);
    }
}
