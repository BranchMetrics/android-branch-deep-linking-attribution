package io.branch.referral;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Class for Branch utility methods
 */
class BranchUtil {

    /**
     * Converts a given link param as {@link JSONObject} to string after adding the source param and removes replaces any illegal characters.
     *
     * @param params Link param JSONObject.
     * @return A {@link String} representation of link params.
     */
    public static String formatAndStringifyLinkParam(JSONObject params) {
        return stringifyAndAddSource(filterOutBadCharacters(params));
    }

    /**
     * Convert the given JSONObject to string and adds source value as
     *
     * @param params JSONObject to convert to string
     * @return A {@link String} value representing the JSONObject
     */
    public static String stringifyAndAddSource(JSONObject params) {
        if (params == null) {
            params = new JSONObject();
        }
        try {
            params.put("source", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

    /**
     * Replaces illegal characters in the given JSONObject.
     *
     * @param inputObj JSONObject to remove illegal characters.
     * @return A {@link JSONObject} with illegal characters replaced.
     */
    public static JSONObject filterOutBadCharacters(JSONObject inputObj) {
        JSONObject filteredObj = new JSONObject();
        if (inputObj != null) {
            Iterator<String> keys = inputObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (inputObj.has(key) && inputObj.get(key).getClass().equals(String.class)) {
                        filteredObj.put(key, inputObj.getString(key).replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\""));
                    } else if (inputObj.has(key)) {
                        filteredObj.put(key, inputObj.get(key));
                    }
                } catch (JSONException ignore) {

                }
            }
        }
        return filteredObj;
    }
}
