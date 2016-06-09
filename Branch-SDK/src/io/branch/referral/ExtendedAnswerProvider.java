package io.branch.referral;

import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.sdk.android.answersshim.AnswersOptionalLogger;
import com.crashlytics.sdk.android.answersshim.KitEvent;

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
    private static final String KIT_EVENT_INSTALL = "install";
    private static final String KIT_EVENT_OPEN = "open";
    private static final String KIT_EVENT_SHARE = "share";

    private static final String EXTRA_PARAM_NOTATION = "+";
    private static final String CTRL_PARAM_NOTATION = "~";
    private static final String INNER_PARAM_NOTATION = ".";

    /**
     * <p>
     * Method for sending the kit events to Answers for Branch Events.
     * Method create a Json Object to flatten and create the {@link KitEvent} with key-values
     * </p>
     *
     * @param request  {@link ServerRequest} corresponding to the event
     * @param response {@link ServerResponse} corresponding to the event
     */
    public static void provideData(ServerRequest request, ServerResponse response) {
        JSONObject jsonData = null;
        ExtendedKitEvent kitEvent = null;
        try {
            if (request instanceof ServerRequestInitSession) {
                if (response.getObject() != null && response.getObject().has(Defines.Jsonkey.Data.getKey())) {
                    if (request instanceof ServerRequestRegisterInstall) {
                        kitEvent = new ExtendedKitEvent(KIT_EVENT_INSTALL);
                    } else {
                        kitEvent = new ExtendedKitEvent(KIT_EVENT_OPEN);
                    }
                    jsonData = new JSONObject();
                    jsonData.put(Defines.Jsonkey.Data.getKey(), new JSONObject(response.getObject().getString(Defines.Jsonkey.Data.getKey())));
                }
            } else if (request instanceof ServerRequestCreateUrl && ((ServerRequestCreateUrl) request).isReqStartedFromBranchShareSheet()) {
                BranchLinkData linkData = ((ServerRequestCreateUrl) request).getLinkPost();
                if (linkData != null && linkData.has(Defines.Jsonkey.Data.getKey())) {
                    kitEvent = new ExtendedKitEvent(KIT_EVENT_SHARE);
                    jsonData = new JSONObject();
                    JSONObject linkDataJson = new JSONObject(linkData.getString(Defines.Jsonkey.Data.getKey()));

                    jsonData.put(Defines.LinkParam.Channel.getKey(), linkData.getChannel());
                    jsonData.put(Defines.LinkParam.Alias.getKey(), linkData.getAlias());
                    jsonData.put(Defines.LinkParam.Type.getKey(), linkData.getType());
                    jsonData.put(Defines.LinkParam.Feature.getKey(), linkData.getFeature());
                    jsonData.put(Defines.LinkParam.Duration.getKey(), linkData.getDuration());
                    jsonData.put(Defines.LinkParam.Stage.getKey(), linkData.getStage());
                    if (linkData.has(Defines.LinkParam.Tags.getKey())) {
                        jsonData.put(Defines.LinkParam.Tags.getKey(), linkData.getJSONArray(Defines.LinkParam.Tags.getKey()));
                    }

                    jsonData.put(Defines.Jsonkey.Data.getKey(), linkDataJson);
                    jsonData.put(Defines.Jsonkey.Link.getKey(), response.getObject().getString(Defines.LinkParam.URL.getKey()));
                }
            }
            if (jsonData != null) {
                addJsonDObjectToKitEvent(kitEvent, jsonData, "");
                AnswersOptionalLogger.get().logKitEvent(kitEvent);
            }
        } catch (JSONException ignore) {
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
    private static void addJsonDObjectToKitEvent(ExtendedKitEvent kitEvent, JSONObject jsonData, String keyPathPrepend) throws JSONException {
        Iterator<String> keyIterator = jsonData.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = jsonData.get(key);

            if (!key.startsWith(EXTRA_PARAM_NOTATION)) {
                if (value instanceof JSONObject) {
                    addJsonDObjectToKitEvent(kitEvent, (JSONObject) value, keyPathPrepend + key + INNER_PARAM_NOTATION);
                } else if (value instanceof JSONArray) {
                    addJsonArrayToKitEvent(kitEvent, (JSONArray) value, key + INNER_PARAM_NOTATION);
                } else {
                    kitEvent.addBranchAttributes(keyPathPrepend, key, jsonData.getString(key));
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
    private static void addJsonArrayToKitEvent(ExtendedKitEvent kitEvent, JSONArray jsonArray, String keyPathPrepend) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            kitEvent.addBranchAttributes(keyPathPrepend, Integer.toString(i), jsonArray.getString(i));
        }
    }

    /**
     * <p>
     * Convenient wrapper class for  {@link KitEvent}
     * </p>
     */
    private static class ExtendedKitEvent extends KitEvent {
        public ExtendedKitEvent(String eventName) {
            super(eventName);
        }

        public void addBranchAttributes(String keyPathPrepend, String key, String value) {
            if (!TextUtils.isEmpty(value)) {
                String modifiedKey = keyPathPrepend + key;
                if (key.startsWith(CTRL_PARAM_NOTATION)) {
                    modifiedKey = key.replaceFirst(CTRL_PARAM_NOTATION, "");
                } else if (modifiedKey.startsWith(CTRL_PARAM_NOTATION)) {
                    modifiedKey = modifiedKey.replaceFirst(CTRL_PARAM_NOTATION, "");
                }
                PrefHelper.Debug("kittest", modifiedKey + " = " + value);
                putAttribute(modifiedKey, value);
            }
        }
    }
}
