package io.branch.referral;

import android.content.Context;
import android.net.TrafficStats;
import android.text.TextUtils;

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

/**
 * Created by sojanpr on 2/8/18.
 * <p>
 * Class for analysing the URLs or URIs for any sensitive data.
 * This class features differentiating URLs URIs based on the black listed or white listed formats.
 * </p>
 */

class UniversalResourceAnalyser {
    private static JSONObject skipURLFormats;
    private final ArrayList<String> acceptURLFormats;
    private static final String SKIP_URL_FORMATS_KEY = "skip_url_format_key";
    private static final String VERSION_KEY = "version";
    private static final String SKIP_LIST_KEY = "uri_skip_list";
    // This is the path for updating skip url list. Check for the next version of the file
    private static final String UPDATE_URL_PATH = "%sdk/uriskiplist_v#.json";
    
    private final JSONObject DEFAULT_SKIP_URL_LIST;

    private static UniversalResourceAnalyser instance;
    
    
    public static UniversalResourceAnalyser getInstance(Context context) {
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
            // Direct network call instead of AsyncTask for thread safety
            updateSkipURLFormatsDirectly(context);
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        }
    }
    
    private void updateSkipURLFormatsDirectly(Context context) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        TrafficStats.setThreadStatsTag(0);
        JSONObject respObject = new JSONObject();
        HttpsURLConnection connection = null;
        try {
            String update_url_path = UPDATE_URL_PATH.replace("%", PrefHelper.getCDNBaseUrl());
            URL urlObject = new URL(update_url_path.replace("#", Integer.toString(skipURLFormats.optInt(VERSION_KEY) + 1)));
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                if (connection.getInputStream() != null) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    respObject = new JSONObject(rd.readLine());
                }
            }
            
            if (respObject.optInt(VERSION_KEY) > skipURLFormats.optInt(VERSION_KEY)) {
                skipURLFormats = respObject;
                prefHelper.setString(SKIP_URL_FORMATS_KEY, skipURLFormats.toString());
            }
        } catch (Exception e) {
            BranchLogger.d(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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
    
}
