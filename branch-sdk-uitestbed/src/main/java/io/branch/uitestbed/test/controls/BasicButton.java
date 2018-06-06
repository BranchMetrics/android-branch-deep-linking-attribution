package io.branch.uitestbed.test.controls;

import android.content.Context;
import android.view.View;

import org.json.JSONObject;

import io.branch.uitestbed.test.data.TestResponse;

public abstract class BasicButton extends android.support.v7.widget.AppCompatButton implements View.OnClickListener {
    private final ITestEvents testEvents;

    public BasicButton(Context context, ITestEvents testEvents) {
        super(context);
        this.testEvents = testEvents;
        init(context);
    }

    private void init(Context context) {
        this.setOnClickListener(this);
        setText(getDisplayName());
        setTextSize(9.0f);
    }

    @Override
    public void onClick(View v) {
        if (testEvents != null) {
            testEvents.onTestStart(getTestDescription());
        }
        onClicked();
    }

    // public methods
    public void onActionResponse(TestResponse resp) {
        if (testEvents != null) {
            testEvents.onTestResponse(resp);
        }
    }

    // Abstract methods
    public abstract void onClicked();

    public abstract String getDisplayName();

    public abstract String getTestDescription();


}
