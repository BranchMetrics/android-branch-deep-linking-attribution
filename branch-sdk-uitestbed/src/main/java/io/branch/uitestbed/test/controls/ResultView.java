package io.branch.uitestbed.test.controls;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.json.JSONException;
import org.json.JSONObject;

public class ResultView extends android.support.v7.widget.AppCompatTextView implements ITestEvents {
    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void onTestStart(String message) {
        this.setText(message);
    }

    @Override
    public void onTestResponse(JSONObject resp) {
        try {
            this.setText(resp.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
