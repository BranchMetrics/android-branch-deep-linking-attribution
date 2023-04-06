package io.branch.referral;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The existing UniversalResourceAnalyser is not designed to be unit tested.
 * <p>
 * SkipListRegex is a copy pasted version of just the regex check.
 */
class SkipListRegex {
    private JSONObject skipURLFormats;
    private ArrayList<String> acceptURLFormats;
    private static final String SKIP_LIST_KEY = "uri_skip_list";

    public SkipListRegex() {
        useV0SkipList();

        // methods to manipulate this were not copied over
        acceptURLFormats = new ArrayList<>();
    }

    public void useV0SkipList() {
        skipURLFormats = getV0SkipList();
    }

    public void useV1SkipList() {
        skipURLFormats = getV1SkipList();
    }

    public void useV2SkipList() {
        skipURLFormats = getV2SkipList();
    }

    @NonNull
    private JSONObject getV0SkipList() {
        JSONObject skiplist = new JSONObject();
        try {
            skiplist.putOpt("version", 0);
            JSONArray skipURIArray = new JSONArray();
            skiplist.putOpt(SKIP_LIST_KEY, skipURIArray);
            skipURIArray.put("^fb\\d+:");
            skipURIArray.put("^li\\d+:");
            skipURIArray.put("^pdk\\d+:");
            skipURIArray.put("^twitterkit-.*:");
            skipURIArray.put("^com\\.googleusercontent\\.apps\\.\\d+-.*:\\/oauth");
            skipURIArray.put("^(?i)(?!(http|https):).*(:|:.*\\b)(password|o?auth|o?auth.?token|access|access.?token)\\b");
            skipURIArray.put("^(?i)((http|https):\\/\\/).*[\\/|?|#].*\\b(password|o?auth|o?auth.?token|access|access.?token)\\b");
        } catch (JSONException ignore) {
        }
        return skiplist;
    }

    @NonNull
    private JSONObject getV1SkipList() {
        JSONObject skiplist = new JSONObject();
        try {
            skiplist.putOpt("version", 0);
            JSONArray skipURIArray = new JSONArray();
            skiplist.putOpt(SKIP_LIST_KEY, skipURIArray);
            skipURIArray.put("^fb\\d+:");
            skipURIArray.put("^li\\d+:");
            skipURIArray.put("^pdk\\d+:");
            skipURIArray.put("^twitterkit-.*:");
            skipURIArray.put("^com\\.googleusercontent\\.apps\\.\\d+-.*:\\/oauth");
            skipURIArray.put("^(?i)(?!(http|https):).*(:|:.*\\b)(password|o?auth|o?auth.?token|access|access.?token)\\b");
            skipURIArray.put("^(?i)((http|https):\\/\\/).*[\\/|?|#].*\\b(password|o?auth|o?auth.?token|access|access.?token)\\b");
        } catch (JSONException ignore) {
        }
        return skiplist;
    }

    @NonNull
    private JSONObject getV2SkipList() {
        JSONObject skiplist = new JSONObject();
        try {
            skiplist.putOpt("version", 0);
            JSONArray skipURIArray = new JSONArray();
            skiplist.putOpt(SKIP_LIST_KEY, skipURIArray);
            skipURIArray.put("^fb\\d+:((?!campaign_ids).)*$"); // Proposed updated regex
            skipURIArray.put("^li\\d+:");
            skipURIArray.put("^pdk\\d+:");
            skipURIArray.put("^twitterkit-.*:");
            skipURIArray.put("^com\\.googleusercontent\\.apps\\.\\d+-.*:\\/oauth");
            skipURIArray.put("^(?i)(?!(http|https):).*(:|:.*\\b)(password|o?auth|o?auth.?token|access|access.?token)\\b");
            skipURIArray.put("^(?i)((http|https):\\/\\/).*[\\/|?|#].*\\b(password|o?auth|o?auth.?token|access|access.?token)\\b");
        } catch (JSONException ignore) {
        }
        return skiplist;
    }

    /**
     * Copy pasted from UniversalResourceAnalyser
     *
     * This is an odd design.
     * When a url matches, it returns the regex string that matched the url
     * When a url does not match, it returns the url as is
     *
     * @param url
     * @return
     */
    public String getStrippedURL(String url) {
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

                    } catch (JSONException ignore) {
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

@RunWith(AndroidJUnit4.class)
public class UniversalResourceAnalyserSkipListUpgradeTests extends BranchTest {

    /**
     * List of good URLs from the original iOS unit tests
     */
    List<String> goodURLs = Arrays.asList(
            "shshs:/content/path",
            "shshs:content/path",
            "https://myapp.app.link/12345/link",
            "fb123x:/",
            "https://myapp.app.link?authentic=true&tokemonsta=false",
            "myscheme://path/brauth=747474"
    );

    /**
     * List of bad URLs from the original iOS unit tests
     */
    List<String> badURLs = Arrays.asList(
            "fb123456:login/464646",
            "twitterkit-.4545:",
            "shsh:oauth/login",
            "https://myapp.app.link/oauth_token=fred",
            "https://myapp.app.link/auth_token=fred",
            "https://myapp.app.link/authtoken=fred",
            "https://myapp.app.link/auth=fred",
            "fb1234:",
            "fb1234:/",
            "fb1234:/this-is-some-extra-info/?whatever",
            "fb1234:/this-is-some-extra-info/?whatever:andstuff",
            "myscheme:path/to/resource?oauth=747474",
            "myscheme:oauth=747474",
            "myscheme:/oauth=747474",
            "myscheme://oauth=747474",
            "myscheme://path/oauth=747474",
            "myscheme://path/:oauth=747474",
            "https://google.com/userprofile/devonbanks=oauth?"
    );

    @Test
    public void testBadURLsWithDefault() {
        SkipListRegex regex = new SkipListRegex();
        for (String url : this.badURLs) {
            String stripped = regex.getStrippedURL(url);
            Assert.assertNotNull(stripped);
            Assert.assertNotEquals(url, stripped);
        }
    }

    @Test
    public void testGoodURLsWithDefault() {
        SkipListRegex regex = new SkipListRegex();
        for (String url : this.goodURLs) {
            String stripped = regex.getStrippedURL(url);
            Assert.assertNotNull(stripped);
            Assert.assertEquals(url, stripped);
        }
    }

    @Test
    public void testBadURLsWithV2() {
        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        for (String url : this.badURLs) {
            String stripped = regex.getStrippedURL(url);
            Assert.assertNotNull(stripped);
            Assert.assertNotEquals(url, stripped);
        }
    }

    @Test
    public void testGoodURLsWithV2() {
        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        for (String url : this.goodURLs) {
            String stripped = regex.getStrippedURL(url);
            Assert.assertNotNull(stripped);
            Assert.assertEquals(url, stripped);
        }
    }

    @Test
    public void testMetaAEMWithV0() {
        String url = "fb1://?campaign_ids=a";

        SkipListRegex regex = new SkipListRegex();
        regex.useV0SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }

    @Test
    public void testMetaAEMWithV2() {
        String url = "fb1://?campaign_ids=a";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertEquals(url, stripped);
    }

    @Test
    public void testMetaAEMWithV2WithTrailingParameters() {
        String url = "fb1://?campaign_ids=a&token=abcde";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertEquals(url, stripped);
    }

    @Test
    public void testMetaAEMWithV2WithPrecedingParameters() {
        String url = "fb1://?brand=abcde&campaign_ids=a";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertEquals(url, stripped);
    }

    @Test
    public void testMetaAEMWithV2WithPrecedingAndTrailingParameters() {
        String url = "fb1://?brand=abcde&campaign_ids=a&link=12345";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertEquals(url, stripped);
    }

    @Test
    public void testSampleMetaAEMWithV0() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22campaign_ids%22%3A%22ARFUlbyOurYrHT2DsknR7VksCSgN4tiH8TzG8RIvVoUQoYog5bVCvADGJil5kFQC6tQm-fFJQH0w8wCi3NbOmEHHrtgCNglkXNY-bECEL0aUhj908hIxnBB0tchJCqwxHjorOUqyk2v4bTF75PyWvxOksZ6uTzBmr7wJq8XnOav0bA%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV0SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }

    @Test
    public void testSampleMetaAEMWithV1() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22campaign_ids%22%3A%22ARFUlbyOurYrHT2DsknR7VksCSgN4tiH8TzG8RIvVoUQoYog5bVCvADGJil5kFQC6tQm-fFJQH0w8wCi3NbOmEHHrtgCNglkXNY-bECEL0aUhj908hIxnBB0tchJCqwxHjorOUqyk2v4bTF75PyWvxOksZ6uTzBmr7wJq8XnOav0bA%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV1SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }

    // The V2 regex list does NOT match the url
    @Test
    public void testSampleMetaAEMWithV2() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22campaign_ids%22%3A%22ARFUlbyOurYrHT2DsknR7VksCSgN4tiH8TzG8RIvVoUQoYog5bVCvADGJil5kFQC6tQm-fFJQH0w8wCi3NbOmEHHrtgCNglkXNY-bECEL0aUhj908hIxnBB0tchJCqwxHjorOUqyk2v4bTF75PyWvxOksZ6uTzBmr7wJq8XnOav0bA%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertEquals(url, stripped);
    }

    @Test
    public void testSampleMetaAEMNoCampignIDsWithV0() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV0SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }

    @Test
    public void testSampleMetaAEMNoCampignIDsWithV1() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV1SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }

    @Test
    public void testSampleMetaAEMNoCampignIDsWithV2() {
        String url = "fb123456789://products/next?al_applink_data=%7B%22target_url%22%3A%22http%3A%5C%2F%5C%2Fitunes.apple.com%5C%2Fapp%5C%2Fid880047117%22%2C%22extras%22%3A%7B%22fb_app_id%22%3A2020399148181142%7D%2C%22referer_app_link%22%3A%7B%22url%22%3A%22fb%3A%5C%2F%5C%2F%5C%2F%3Fapp_id%3D2020399148181142%22%2C%22app_name%22%3A%22Facebook%22%7D%2C%22acs_token%22%3A%22debuggingtoken%22%2C%22test_deeplink%22%3A1%7D";

        SkipListRegex regex = new SkipListRegex();
        regex.useV2SkipList();

        String stripped = regex.getStrippedURL(url);
        Assert.assertNotNull(stripped);
        Assert.assertNotEquals(url, stripped);
    }
}
