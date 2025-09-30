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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "URLAnalyzer-Worker");
        t.setDaemon(true);
        return t;
    });

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
            BranchLogger.d("MODERNIZATION_TRACE: UniversalResourceAnalyser using CompletableFuture pattern");
            CompletableFuture.supplyAsync(() -> {
                BranchLogger.d("MODERNIZATION_TRACE: Executing URL skip list update in CompletableFuture");
                return updateUrlSkipList(context);
            }, executor).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    BranchLogger.w("Failed to update URL skip list: " + throwable.getMessage());
                } else if (result != null && result.length() > 0) {
                    try {
                        PrefHelper prefHelper = PrefHelper.getInstance(context);
                        int currentVersion = skipURLFormats.optInt(VERSION_KEY, 0);
                        int newVersion = result.optInt(VERSION_KEY, 0);
                        
                        if (newVersion > currentVersion) {
                            skipURLFormats = result;
                            prefHelper.setString(SKIP_URL_FORMATS_KEY, skipURLFormats.toString());
                            BranchLogger.d("MODERNIZATION_TRACE: Updated URL skip list to version " + newVersion + " via CompletableFuture");
                        } else {
                            BranchLogger.d("MODERNIZATION_TRACE: URL skip list is already up to date (version " + currentVersion + ")");
                        }
                    } catch (Exception e) {
                        BranchLogger.w("Error processing URL skip list update: " + e.getMessage());
                    }
                } else {
                    BranchLogger.d("No URL skip list update available");
                }
            }).exceptionally(throwable -> {
                BranchLogger.w("URL skip list update failed with exception: " + throwable.getMessage());
                return null;
            });
        } catch (Exception e) {
            BranchLogger.w("Failed to initiate URL skip list update: " + e.getMessage());
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
    
    private JSONObject updateUrlSkipList(Context context) {
        TrafficStats.setThreadStatsTag(0);
        JSONObject respObject = new JSONObject();
        HttpsURLConnection connection = null;
        final int TIME_OUT = 1500;
        
        try {
            String cdnBaseUrl = PrefHelper.getCDNBaseUrl();
            if (cdnBaseUrl == null || cdnBaseUrl.isEmpty()) {
                BranchLogger.w("CDN base URL is not available");
                return respObject;
            }
            
            String update_url_path = UPDATE_URL_PATH.replace("%", cdnBaseUrl);
            int nextVersion = skipURLFormats.optInt(VERSION_KEY, 0) + 1;
            String finalUrl = update_url_path.replace("#", Integer.toString(nextVersion));
            
            BranchLogger.d("Checking for URL skip list update at: " + finalUrl);
            
            URL urlObject = new URL(finalUrl);
            connection = (HttpsURLConnection) urlObject.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIME_OUT);
            connection.setReadTimeout(TIME_OUT);
            connection.setUseCaches(true);
            connection.setDefaultUseCaches(true);
            
            int responseCode = connection.getResponseCode();
            BranchLogger.d("URL skip list update response code: " + responseCode);
            
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                if (connection.getInputStream() != null) {
                    try (BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = rd.readLine()) != null) {
                            response.append(line);
                        }
                        if (response.length() > 0) {
                            respObject = new JSONObject(response.toString());
                            BranchLogger.d("Successfully parsed URL skip list update");
                        }
                    }
                }
            } else if (responseCode == HttpsURLConnection.HTTP_NOT_FOUND) {
                BranchLogger.d("No newer URL skip list version available");
            } else {
                BranchLogger.w("URL skip list update failed with response code: " + responseCode);
            }
        } catch (java.net.MalformedURLException e) {
            BranchLogger.w("Invalid URL for skip list update: " + e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            BranchLogger.w("URL skip list update timed out: " + e.getMessage());
        } catch (java.io.IOException e) {
            BranchLogger.w("Network error during URL skip list update: " + e.getMessage());
        } catch (org.json.JSONException e) {
            BranchLogger.w("Invalid JSON in URL skip list response: " + e.getMessage());
        } catch (Exception e) {
            BranchLogger.w("Unexpected error during URL skip list update: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    BranchLogger.d("Error disconnecting HTTP connection: " + e.getMessage());
                }
            }
        }
        return respObject;
    }
    
}
