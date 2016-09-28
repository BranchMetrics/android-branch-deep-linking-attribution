package io.branch.bridge;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.branch.indexing.BranchUniversalObject;

/**
 * Created by sojanpr on 9/26/16.
 */
public class AppBridgeService extends Service {
    private static final String APP_BRIDGE_PREF = "app_bridge_pref";
    private static final String APP_BRIDGE_CONTENT_KEY = "app_bridge_content";
    private JSONObject contentJsonObject_;

    @Override
    public IBinder onBind(Intent intent) {
        if (contentJsonObject_ == null){
            contentJsonObject_ = getStoredContents();
        }
        return mBinder;
    }

    private final IBridgeInterface.Stub mBinder = new IBridgeInterface.Stub() {

        @Override
        public void addToSharableContent(BranchUniversalObject contentBUO) throws RemoteException {
            addContent(contentBUO);
        }

        @Override
        public List<BranchUniversalObject> searchContent(String keyword) throws RemoteException {
            return getContents(keyword);
        }
    };


    private void addContent(BranchUniversalObject contentBUO) {
        Log.d("Bridge_test", "addContent");
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(APP_BRIDGE_PREF, Context.MODE_PRIVATE);
        try {
            String keywordStr = "";
            for (String keyword : contentBUO.getKeywords()) {
                keywordStr = keywordStr + "#" + keyword;
            }
            contentJsonObject_.put(keywordStr, contentBUO.convertToJson());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(APP_BRIDGE_CONTENT_KEY, contentJsonObject_.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getStoredContents(){

        JSONObject contentJsonObject = new JSONObject();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(APP_BRIDGE_PREF, Context.MODE_PRIVATE);
        String contentJsonStr = sharedPref.getString(APP_BRIDGE_CONTENT_KEY, "");
        if (!TextUtils.isEmpty(contentJsonStr)) {
            try {
                contentJsonObject = new JSONObject(contentJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d("Bridge_test", "getStoredContents"+contentJsonObject);
        return contentJsonObject;
    }

    private ArrayList<BranchUniversalObject> getContents(String searchKeyWord) {
        Log.d("Bridge_test", "getContents " +searchKeyWord);
        ArrayList<BranchUniversalObject> searchResult = new ArrayList<>();
        Iterator<String>keys = contentJsonObject_.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if(searchKeyWord == null || key.contains(searchKeyWord)) {
                try {
                    searchResult.add(BranchUniversalObject.createInstance(contentJsonObject_.getJSONObject(key)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return  searchResult;
    }


}
