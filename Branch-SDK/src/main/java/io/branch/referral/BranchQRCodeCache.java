package io.branch.referral;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BranchQRCodeCache {

    private final SystemObserver systemObserver_;
    private final Context context_;

    public ConcurrentHashMap<JSONObject, byte[]> cache = new ConcurrentHashMap<>();

    /**
     * Get the singleton instance for this class
     *
     * @return {@link BranchQRCodeCache} instance if already initialised or null
     */
    public static BranchQRCodeCache getInstance() {
        Branch b = Branch.init();
        if (b == null) return null;
        return b.getBranchQRCodeCache();
    }

    BranchQRCodeCache(Context context) {
        context_ = context;
        systemObserver_ = new BranchQRCodeCache.SystemObserverInstance();
    }

    /**
     * Concrete SystemObserver implementation
     */
    private class SystemObserverInstance extends SystemObserver {
        public SystemObserverInstance() {
            super();
        }
    }

    /**
     * @return the current SystemObserver instance
     */
    SystemObserver getSystemObserver() {
        return systemObserver_;
    }

    //QR Code Caching Functions
    public void addQRCodeToCache(JSONObject parameters, byte[] qrCodeData) {
        cache.clear();
        try {
            parameters.getJSONObject("data").remove(Defines.Jsonkey.CreationTimestamp.getKey());
            cache.put(parameters, qrCodeData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public byte[] checkQRCodeCache(JSONObject parameters) {
        if (cache.isEmpty()) {
            return null;
        }
        try {
            parameters.getJSONObject("data").remove(Defines.Jsonkey.CreationTimestamp.getKey());
            JSONObject cacheParam = (JSONObject) cache.keySet().iterator().next();

            if (areEqual(parameters, cacheParam)) {
                return cache.get(cacheParam);
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Helper Functions
    public static boolean areEqual(Object ob1, Object ob2) throws JSONException {
        Object obj1Converted = convertJsonElement(ob1);
        Object obj2Converted = convertJsonElement(ob2);
        return obj1Converted.equals(obj2Converted);
    }

    private static Object convertJsonElement(Object elem) throws JSONException {
        if (elem instanceof JSONObject) {
            JSONObject obj = (JSONObject) elem;
            Iterator<String> keys = obj.keys();
            Map<String, Object> jsonMap = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                jsonMap.put(key, convertJsonElement(obj.get(key)));
            }
            return jsonMap;
        } else if (elem instanceof JSONArray) {
            JSONArray arr = (JSONArray) elem;
            Set<Object> jsonSet = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                jsonSet.add(convertJsonElement(arr.get(i)));
            }
            return jsonSet;
        } else {
            return elem;
        }
    }
}


