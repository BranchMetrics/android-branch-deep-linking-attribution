package io.branch.referral;

/**
 * Define the Applications for the sharing the link with.
 */
public class SharingHelper {
    /**
     * <p>
     * Defines the Application for sharing a deep link with.
     * </p>
     */
    public enum SHARE_WITH {
        FACEBOOK("com.facebook.katana"),
        FACEBOOK_MESSENGER("com.facebook.orca"),
        TWITTER("com.twitter.android"),
        MESSAGE(".mms"),
        EMAIL("com.google.android.email"),
        FLICKR("com.yahoo.mobile.client.android.flickr"),
        GOOGLE_DOC("com.google.android.apps.docs"),
        WHATS_APP("com.whatsapp"),
        PINTEREST("com.pinterest"),
        HANGOUT("com.google.android.talk"),
        INSTAGRAM("com.instagram.android"),
        WECHAT("jom.tencent.mm"),
        GMAIL("com.google.android.gm");

        private String name = "";

        private SHARE_WITH(String key) {
            this.name = key;
        }

        public String getAppName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
