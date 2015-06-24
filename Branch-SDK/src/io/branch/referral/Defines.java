package io.branch.referral;

/**
 * <p>
 * Defines all Json keys associated with branch request parameters.
 * </p>
 *
 */
public class Defines {

    public enum Jsonkey {

        IdentityID("identity_id"),
        DeviceFingerprintID("device_fingerprint_id"),
        SessionID("session_id"),
        LinkClickID("link_click_id"),

        Bucket("bucket"),
        DefaultBucket("default"),
        Amount("amount");


        private String key = "";

        private Jsonkey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * <p>
     * Defines all server path for the requests
     * </p>
     *
     */
    public enum RequestPath {
        RedeemRewards("v1/redeem"),
        GetURL("v1/url"),
        RegisterInstall("v1/install"),
        RegisterClose("v1/close"),
        RegisterOpen("v1/open"),
        Referrals("v1/referrals/"),
        SendAPPList("v1/applist"),
        GetCredits("v1/credits/"),
        GetCreditHistory("v1/credithistory"),
        CompletedAction("v1/event"),
        IdentifyUser("v1/profile"),
        Logout("v1/logout"),
        GetReferralCode("v1/referralcode"),
        ValidateReferralCode("v1/referralcode/"),
        ApplyReferralCode("v1/applycode/"),
        DebugConnect("v1/debug/connect");


        private String key = "";

        private RequestPath(String key) {
            this.key = key;
        }

        public String getPath() {
            return key;
        }

        @Override
        public String toString() {
            return key;
        }
    }


}
