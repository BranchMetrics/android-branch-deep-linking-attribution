package io.branch.referral.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Defines;

/**
 * Created by sojanpr on 7/12/17.
 * <p>
 * Builder class for tracking a custom event. This class builds a custom event and
 * send to Branch. Please see {@link #track(Context)} method to execute tracking
 * </p>
 */
public class TrackCustomEventBuilder extends TrackEventBuilder {
    public TrackCustomEventBuilder(String eventName) {
        super(Defines.RequestPath.TrackCustomEvent.getPath(), eventName);
    }

    @Override
    protected JSONObject getRequestBody() {
        JSONObject reqBody = new JSONObject();
        try {
            reqBody.put(Defines.Jsonkey.Name.getKey(), eventName);
            if (customData.length() > 0) {
                reqBody.put(Defines.Jsonkey.CustomData.getKey(), customData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqBody;
    }
}
