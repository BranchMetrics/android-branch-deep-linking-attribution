package io.branch.referral;

import android.content.Context;
import android.net.TrafficStats;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import io.branch.channels.SkipListChannel;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Created by sojanpr on 2/8/18.
 * <p>
 * Class for analysing the URLs or URIs for any sensitive data.
 * This class features differentiating URLs URIs based on the black listed or white listed formats.
 * </p>
 */

public class UniversalResourceAnalyser {
    private static JSONObject skipURLFormats;
    private final ArrayList<String> acceptURLFormats;
    private static final String SKIP_URL_FORMATS_KEY = "skip_url_format_key";
    public static final String VERSION_KEY = "version";
    private static final String SKIP_LIST_KEY = "uri_skip_list";

    private final JSONObject DEFAULT_SKIP_URL_LIST;

    private static UniversalResourceAnalyser instance;
    
    
    public static UniversalResourceAnalyser getInstance(Context context) {
        BranchLogger.d("UniversalResourceAnalyser getInstance");
        if (instance == null) {
            instance = new UniversalResourceAnalyser(context);
        }
        return instance;
    }
    
    private UniversalResourceAnalyser(Context context) {
        DEFAULT_SKIP_URL_LIST = new JSONObject();
        try {
            DEFAULT_SKIP_URL_LIST.putOpt("version", 0);
            JSONArray skipURIArray = new JSONArray();
            DEFAULT_SKIP_URL_LIST.putOpt("uri_skip_list", skipURIArray);
            skipURIArray.put("^fb\\d+:((?!campaign_ids).)*$");
            skipURIArray.put("^li\\d+:");
            skipURIArray.put("^pdk\\d+:");
            skipURIArray.put("^twitterkit-.*:");
            skipURIArray.put("^com\\.googleusercontent\\.apps\\.\\d+-.*:\\/oauth");
            skipURIArray.put("^(?i)(?!(http|https):).*(:|:.*\\b)(password|o?auth|o?auth.?token|access|access.?token)\\b");
            skipURIArray.put("^(?i)((http|https):\\/\\/).*[\\/|?|#].*\\b(password|o?auth|o?auth.?token|access|access.?token)\\b");
        } catch (JSONException e) {
            BranchLogger.d(e.getMessage());
        }
        skipURLFormats = retrieveSkipURLFormats(context);
        acceptURLFormats = new ArrayList<>();
    }

    public JSONObject getSkipURLFormats(){
        return this.getSkipURLFormats();
    }
    
    private JSONObject retrieveSkipURLFormats(Context context) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        JSONObject urlFormat = new JSONObject();
        String latestUrlFormats = prefHelper.getString(SKIP_URL_FORMATS_KEY);
        if (TextUtils.isEmpty(latestUrlFormats) || PrefHelper.NO_STRING_VALUE.equals(latestUrlFormats)) {
            urlFormat = DEFAULT_SKIP_URL_LIST;
        } else {
            try {
                urlFormat = new JSONObject(latestUrlFormats);
            } catch (JSONException e) {
                BranchLogger.d(e.getMessage());
            }
        }
        return urlFormat;
    }
    
    void addToSkipURLFormats(String skipURLFormat) {
        JSONArray skipURLArray = skipURLFormats.optJSONArray(SKIP_LIST_KEY);
        try {
            if (skipURLArray == null) {
                skipURLArray = new JSONArray();
                skipURLFormats.put(SKIP_LIST_KEY, skipURLArray);
            }
            skipURLArray.put(skipURLFormat);
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
    }
    
    void addToAcceptURLFormats(String acceptUrl) {
        acceptURLFormats.add(acceptUrl);
    }
    
    void addToAcceptURLFormats(List<String> acceptUrls) {
        acceptURLFormats.addAll(acceptUrls);
    }
    
    void checkAndUpdateSkipURLFormats(Context context) {
        try {
            new UrlSkipListUpdateTask(context).executeTask(context);
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
    }
    
    String getStrippedURL(String url) {
        String strippedURL = null;
        try {
            JSONArray skipURLArray = skipURLFormats.optJSONArray(SKIP_LIST_KEY);
            if (skipURLArray != null) {
                for (int i = 0; i < skipURLArray.length(); i++) {
                    try {
                        String skipPattern = skipURLArray.getString(i);
                        Pattern p = Pattern.compile(skipPattern);
                        Matcher m = p.matcher(url);
                        if (m.find()) {
                            strippedURL = skipPattern;
                            break;
                        }
                        
                    } catch (JSONException e) {
                        BranchLogger.d(e.getMessage());
                    }
                }
            }
            if (strippedURL == null) {
                if (acceptURLFormats.size() > 0) {
                    for (String skipPattern : acceptURLFormats) {
                        if (url.matches(skipPattern)) {
                            strippedURL = url;
                            break;
                        }
                    }
                } else {
                    strippedURL = url;
                }
            }
        } catch (Exception ex) {
            strippedURL = url;
        }
        return strippedURL;
    }
    
    private static class UrlSkipListUpdateTask {
        private final PrefHelper prefHelper;
        
        private UrlSkipListUpdateTask(Context context) {
            this.prefHelper = PrefHelper.getInstance(context);
        }

        public void executeTask(Context context){
            new SkipListChannel().enqueue(context, new Continuation<JSONObject>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    if(o != null && o instanceof JSONObject){
                        onPostExecute((JSONObject) o);
                    }
                    else{
                        BranchLogger.d("Expected JSONObject, was " + o);
                    }
                }
            });
        }

        protected void onPostExecute(JSONObject updatedURLFormatsObj) {
            BranchLogger.d("UniversalResourceAnalyser onPostExecute " + updatedURLFormatsObj);
            if (updatedURLFormatsObj.optInt(VERSION_KEY) > skipURLFormats.optInt(VERSION_KEY)) {
                skipURLFormats = updatedURLFormatsObj;
                prefHelper.setString(SKIP_URL_FORMATS_KEY, skipURLFormats.toString());
            }
        }
    }
    
}
