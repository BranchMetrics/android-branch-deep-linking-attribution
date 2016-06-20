package io.branch.referral;

import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.answers.shim.AnswersOptionalLogger;
import com.crashlytics.android.answers.shim.KitEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by sojanpr on 6/8/16.
 * <p>
 * Class for providing extended data to Fabric Answers.
 * Create Kit events for the "install" / "open" / "share" events from Branch and update the Answers through
 * Answers-Shim API. Class handles the logic for flattening  a Branch request to discrete key-value pairs for Answer {@link KitEvent}.
 * </p>
 */
class ExtendedAnswerProvider {
    // Event Name for Kit Event
    public static final String KIT_EVENT_INSTALL = "Branch Install";
    public static final String KIT_EVENT_OPEN = "Branch Open";
    public static final String KIT_EVENT_SHARE = "Branch Share";

    private static final String EXTRA_PARAM_NOTATION = "+";
    private static final String CTRL_PARAM_NOTATION = "~";
    private static final String INNER_PARAM_NOTATION = ".";

    /**
     * <p>
     * Method for sending the kit events to Answers for Branch Events.
     * Method create a Json Object to flatten and create the {@link KitEvent} with key-values
     * </p>
     *
     * @param eventName {@link String} name of the KitEvent
     * @param eventData {@link JSONObject} JsonObject containing the event data
     */
    public void provideData(String eventName, JSONObject eventData, String identityID) {
        try {
            KitEvent kitEvent = new KitEvent(eventName);
            if (eventData != null) {
                addJsonObjectToKitEvent(kitEvent, eventData, "");
                kitEvent.putAttribute(Defines.Jsonkey.BranchIdentity.getKey(), identityID);
                AnswersOptionalLogger.get().logKitEvent(kitEvent);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * <p>
     * Converts the given JsonObject to key-value pairs and update the {@link KitEvent}
     * </p>
     *
     * @param kitEvent       {@link KitEvent} to update with key-value pairs from the JsonObject
     * @param jsonData       {@link JSONObject} to add to the {@link KitEvent}
     * @param keyPathPrepend {@link String} with value to prepend to the keys adding to the {@link KitEvent}
     * @throws JSONException {@link JSONException} on any Json converting errors
     */
    private void addJsonObjectToKitEvent(KitEvent kitEvent, JSONObject jsonData, String keyPathPrepend) throws JSONException {
        Iterator<String> keyIterator = jsonData.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = jsonData.get(key);

            if (!key.startsWith(EXTRA_PARAM_NOTATION)) {
                if (value instanceof JSONObject) {
                    addJsonObjectToKitEvent(kitEvent, (JSONObject) value, keyPathPrepend + key + INNER_PARAM_NOTATION);
                } else if (value instanceof JSONArray) {
                    addJsonArrayToKitEvent(kitEvent, (JSONArray) value, key + INNER_PARAM_NOTATION);
                } else {
                    addBranchAttributes(kitEvent, keyPathPrepend, key, jsonData.getString(key));
                }
            }
        }
    }

    /**
     * <p>
     * Converts the given JsonArray to key-value pairs and update the {@link KitEvent}
     * </p>
     *
     * @param kitEvent       {@link KitEvent} to update with key-value pairs from the JsonArray
     * @param jsonArray      {@link JSONArray} to add to the {@link KitEvent}
     * @param keyPathPrepend {@link String} with value to prepend to the keys adding to the {@link KitEvent}
     * @throws JSONException {@link JSONException} on any Json converting errors
     */
    private void addJsonArrayToKitEvent(KitEvent kitEvent, JSONArray jsonArray, String keyPathPrepend) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            addBranchAttributes(kitEvent, keyPathPrepend, CTRL_PARAM_NOTATION + Integer.toString(i), jsonArray.getString(i));
        }
    }

    private void addBranchAttributes(KitEvent kitEvent, String keyPathPrepend, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            if (key.startsWith(CTRL_PARAM_NOTATION)) {
                String modifiedKey = keyPathPrepend.replaceFirst(CTRL_PARAM_NOTATION, "") + key.replaceFirst(CTRL_PARAM_NOTATION, "");
                kitEvent.putAttribute(modifiedKey, value);
            } else if (key.equals("$" + Defines.Jsonkey.IdentityID.getKey())) {
                kitEvent.putAttribute(Defines.Jsonkey.ReferringBranchIdentity.getKey(), value);
            }
        }
    }
}
