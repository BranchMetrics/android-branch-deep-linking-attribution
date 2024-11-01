package io.branch.referral.validators;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.R;
import io.branch.referral.util.LinkProperties;

public class LinkingValidatorDialogRowItem extends LinearLayout {

    private static final String TAG = "BranchSDK";

    TextView titleText;
    Button infoButton;
    String infoText;
    Button shareButton;
    HashMap<String, String> linkDataParams;
    String routingKey;
    String routingValue;
    String canonicalIdentifier;
    Context context;
    Button debugButton;
    String debugText;

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public LinkingValidatorDialogRowItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public void InitializeRow(String title, String infoText, String debugText, String routingKey, String routingValue, String canonicalIdentifier, String... params) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.linking_validator_dialog_row_item, null);
        this.addView(view);
        titleText = view.findViewById(R.id.linkingValidatorRowTitleText);
        infoButton = view.findViewById(R.id.linkingValidatorRowInfoButton);
        shareButton = view.findViewById(R.id.linkingValidatorRowShareButton);
        debugButton = view.findViewById(R.id.linkingValidatorRowDebugButton);

        titleText.setText(title);
        this.infoText = infoText;
        this.debugText = debugText;
        this.routingKey = routingKey;
        this.routingValue = routingValue;
        this.canonicalIdentifier = canonicalIdentifier;

        linkDataParams = new HashMap<>();

        for (int i = 0; i < params.length; i += 2) {
            linkDataParams.put(params[i], params[i + 1]);
        }

        infoButton.setOnClickListener(view1 -> {
            HandleInfoButtonClicked();
        });

        shareButton.setOnClickListener(view2 -> {
            HandleShareButtonClicked();
        });

        debugButton.setOnClickListener(view2 -> {
            HandleDebugButtonClicked();
        });
    }

    private void HandleInfoButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(infoText).setTitle(titleText.getText());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void HandleShareButtonClicked() {
        LinkProperties lp = new LinkProperties();
        for (String key : linkDataParams.keySet()) {
            lp.addControlParameter(key, linkDataParams.get(key));
        }
        BranchUniversalObject buo = new BranchUniversalObject().setCanonicalIdentifier(canonicalIdentifier);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Branch.getInstance().share(getActivity(context), buo, lp, new Branch.BranchNativeLinkShareListener() {
                        @Override
                        public void onLinkShareResponse(String sharedLink, BranchError error) {
                        }

                        @Override
                        public void onChannelSelected(String channelName) {
                        }
                    },
                    titleText.getText().toString(),
                    infoText);
        }
    }

    private void HandleDebugButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(debugText).setTitle(titleText.getText() + " not working?");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }
}
