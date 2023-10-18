package io.branch.referral;

/**
 * <p>
 * Defines all Json keys associated with branch request parameters.
 * </p>
 */
public class Defines {
    
    public enum Jsonkey {
        RandomizedBundleToken("randomized_bundle_token"),
        Identity("identity"),
        RandomizedDeviceToken("randomized_device_token"),
        SessionID("session_id"),
        LinkClickID("link_click_id"),
        GoogleSearchInstallReferrer("google_search_install_referrer"),
        GooglePlayInstallReferrer("install_referrer_extras"),
        ClickedReferrerTimeStamp("clicked_referrer_ts"),
        Gclid("gclid"), //The parameter that is passed in the url
        IsDeeplinkGclid("is_deeplink_gclid"),
        ReferrerGclid("referrer_gclid"), //Key APIOpen expects for gclid in event
        ReferringUrlQueryParameters("bnc_referringUrlQueryParameters"),
        InstallBeginTimeStamp("install_begin_ts"),
        @Deprecated BranchLinkUsed("branch_used"),          //use Defines.IntentKeys.BranchLinkUsed
        ReferringBranchIdentity("referring_branch_identity"),
        BranchIdentity("branch_identity"),
        BranchKey("branch_key"),
        @Deprecated BranchData("branch_data"),              //use Defines.IntentKeys.BranchData
        PlayAutoInstalls("play-auto-installs"),             //UTM_Source set by Xiaomi
        UTMCampaign("utm_campaign"),
        UTMMedium("utm_medium"),
        InitialReferrer("initial_referrer"),

        @Deprecated Bucket("bucket"),
        @Deprecated DefaultBucket("default"),
        Amount("amount"),
        CalculationType("calculation_type"),
        Location("location"),
        Type("type"),
        CreationSource("creation_source"),
        Prefix("prefix"),
        Expiration("expiration"),
        Event("event"),
        Metadata("metadata"),
        CommerceData("commerce_data"),
        Total("total"),
        Unique("unique"),
        Length("length"),
        Direction("direction"),
        BeginAfterID("begin_after_id"),
        Link("link"),
        ReferringData("referring_data"),
        ReferringLink("referring_link"),
        IsFullAppConv("is_full_app_conversion"),
        Data("data"),
        OS("os"),
        HardwareID("hardware_id"),
        AndroidID("android_id"),
        UnidentifiedDevice("unidentified_device"),
        HardwareIDType("hardware_id_type"),
        HardwareIDTypeVendor("vendor_id"),
        HardwareIDTypeRandom("random"),
        IsHardwareIDReal("is_hardware_id_real"),
        AnonID("anon_id"),
        AppVersion("app_version"),
        APILevel("os_version"),
        Country("country"),
        Language("language"),
        Update("update"),
        OriginalInstallTime("first_install_time"),
        FirstInstallTime("latest_install_time"),
        LastUpdateTime("latest_update_time"),
        PreviousUpdateTime("previous_update_time"),
        URIScheme("uri_scheme"),
        AppLinks("app_links"),
        AppIdentifier("app_identifier"),
        LinkIdentifier("link_identifier"),
        GoogleAdvertisingID("google_advertising_id"),       // V1 Only, "Google Advertising Id"
        AAID("aaid"),                                       // V2 Only, "Android Advertising Id"
        FireAdId("fire_ad_id"),
        OpenAdvertisingID("oaid"),                          // Huawei Mobile Services
        LATVal("lat_val"),
        LimitedAdTracking("limit_ad_tracking"),
        limitFacebookTracking("limit_facebook_tracking"),
        Debug("debug"),
        Brand("brand"),
        Model("model"),
        ScreenDpi("screen_dpi"),
        ScreenHeight("screen_height"),
        ScreenWidth("screen_width"),
        WiFi("wifi"),
        LocalIP("local_ip"),
        UserData("user_data"),
        AdvertisingIDs("advertising_ids"),
        DeveloperIdentity("developer_identity"),
        UserAgent("user_agent"),
        SDK("sdk"),
        SdkVersion("sdk_version"),
        UIMode("ui_mode"),
        InstallMetadata("install_metadata"),
        LATDAttributionWindow("attribution_window"),
        
        Clicked_Branch_Link("+clicked_branch_link"),
        IsFirstSession("+is_first_session"),
        AndroidDeepLinkPath("$android_deeplink_path"),
        DeepLinkPath(Branch.DEEPLINK_PATH),
        
        AndroidAppLinkURL("android_app_link_url"),
        @Deprecated AndroidPushNotificationKey("branch"),   //use Defines.IntentKeys.AndroidPushNotificationKey
        AndroidPushIdentifier("push_identifier"),
        
        CanonicalIdentifier("$canonical_identifier"),
        ContentTitle(Branch.OG_TITLE),
        ContentDesc(Branch.OG_DESC),
        ContentImgUrl(Branch.OG_IMAGE_URL),
        CanonicalUrl("$canonical_url"),
        
        ContentType("$content_type"),
        PublicallyIndexable("$publicly_indexable"),
        LocallyIndexable("$locally_indexable"),
        ContentKeyWords("$keywords"),
        ContentExpiryTime("$exp_date"),
        Params("params"),
        SharedLink("$shared_link"),
        ShareError("$share_error"),
        SharedChannel("$shared_channel"),

        URLSource("android"),
        
        External_Intent_URI("external_intent_uri"),
        External_Intent_Extra("external_intent_extra"),
        Last_Round_Trip_Time("lrtt"),
        Branch_Round_Trip_Time("brtt"),
        Branch_Instrumentation("instrumentation"),
        Queue_Wait_Time("qwt"),
        InstantDeepLinkSession("instant_dl_session"),

        Path("path"),
        ViewList("view_list"),
        ContentActionView("view"),
        ContentPath("content_path"),
        ContentNavPath("content_nav_path"),
        ReferralLink("referral_link"),
        ContentData("content_data"),
        ContentEvents("events"),
        ContentAnalyticsMode("content_analytics_mode"),
        Environment("environment"),
        InstantApp("INSTANT_APP"),
        NativeApp("FULL_APP"),
        
        CustomerEventAlias("customer_event_alias"),
        TransactionID("transaction_id"),
        Currency("currency"),
        Revenue("revenue"),
        Shipping("shipping"),
        Tax("tax"),
        Coupon("coupon"),
        Affiliation("affiliation"),
        Description("description"),
        SearchQuery("search_query"),
        AdType("ad_type"),

        CPUType("cpu_type"),
        DeviceBuildId("build"),
        Locale("locale"),
        ConnectionType("connection_type"),
        DeviceCarrier("device_carrier"),
        PluginName("plugin_name"),
        PluginVersion("plugin_version"),
        OSVersionAndroid("os_version_android"),
        
        Name("name"),
        CustomData("custom_data"),
        EventData("event_data"),
        ContentItems("content_items"),
        ContentSchema("$content_schema"),
        Price("$price"),
        PriceCurrency("$currency"),
        Quantity("$quantity"),
        SKU("$sku"),
        ProductName("$product_name"),
        ProductBrand("$product_brand"),
        ProductCategory("$product_category"),
        ProductVariant("$product_variant"),
        Rating("$rating"),
        RatingAverage("$rating_average"),
        RatingCount("$rating_count"),
        RatingMax("$rating_max"),
        AddressStreet("$address_street"),
        AddressCity("$address_city"),
        AddressRegion("$address_region"),
        AddressCountry("$address_country"),
        AddressPostalCode("$address_postal_code"),
        Latitude("$latitude"),
        Longitude("$longitude"),
        ImageCaptions("$image_captions"),
        Condition("$condition"),
        CreationTimestamp("$creation_timestamp"),
        TrackingDisabled("tracking_disabled"),
        DisableAdNetworkCallouts("disable_ad_network_callouts"),
        PartnerData("partner_data"),
        Instant("instant"),

        QRCodeTag("qr-code"),
        CodeColor("code_color"),
        BackgroundColor("background_color"),
        Width("width"),
        Margin("margin"),
        ImageFormat("image_format"),
        CenterLogo("center_logo_url"),
        QRCodeSettings("qr_code_settings"),
        QRCodeData("data"),
        QRCodeBranchKey("branch_key"),
        QRCodeResponseString("QRCodeString"),

        App_Store("app_store"),
        Google_Play_Store("PlayStore"),
        Huawei_App_Gallery("AppGallery"),
        Samsung_Galaxy_Store("GalaxyStore"),
        Xiaomi_Get_Apps("GetApps");

        private final String key;
        
        Jsonkey(String key) {
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
     */
    public enum RequestPath {
        GetURL("v1/url"),
        GetApp("v1/app-link-settings"),
        RegisterInstall("v1/install"),
        RegisterOpen("v1/open"),
        Logout("v1/logout"),
        ContentEvent("v1/content-events"),
        TrackStandardEvent("v2/event/standard"),
        TrackCustomEvent("v2/event/custom"),
        GetLATD("v1/cpid/latd"),
        QRCode("v1/qr-code");

        private final String key;
        
        RequestPath(String key) {
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
    
    /**
     * <p>
     * Defines link parameter keys
     * </p>
     */
    public enum LinkParam {
        Tags("tags"),
        Alias("alias"),
        Type("type"),
        Duration("duration"),
        Channel("channel"),
        Feature("feature"),
        Stage("stage"),
        Campaign("campaign"),
        Data("data"),
        URL("url");
        
        private final String key;
        
        LinkParam(String key) {
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
     * Defines preinstall parameter keys
     * </p>
     */
    public enum PreinstallKey {
        campaign("preinstall_campaign"),
        partner("preinstall_partner");

        private final String key;

        PreinstallKey(String key) {
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
     * Defines module name keys
     * </p>
     */
    public enum IntentKeys {
        BranchData("branch_data"),
        ForceNewBranchSession("branch_force_new_session"),
        BranchLinkUsed("branch_used"),
        BranchURI("branch"),

        /* Key to indicate whether the Activity was launched by Branch or not. */
        AutoDeepLinked("io.branch.sdk.auto_linked");

        // The below intent keys are also used to extract data from the intent (via ActivityCompact.getReferrer())
        // public static final String EXTRA_REFERRER = "android.intent.extra.REFERRER";
        // public static final String EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";

        private final String key;

        IntentKeys(String key) {
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
     * Defines Branch header keys
     * </p>
     */
    public enum HeaderKey {
        RequestId("X-Branch-Request-Id"),
        SendCloseRequest("X-Branch-Send-Close-Request");

        private final String key;

        HeaderKey(String key) {
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
}
