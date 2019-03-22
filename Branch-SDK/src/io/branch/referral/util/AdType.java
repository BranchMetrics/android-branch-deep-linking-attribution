package io.branch.referral.util;

/**
 * Ad Types
 */
public enum AdType {
    BANNER("BANNER"),
    INTERSTITIAL("INTERSTITIAL"),
    REWARDED_VIDEO("REWARDED_VIDEO"),
    NATIVE("NATIVE");

    private final String name;

    AdType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
