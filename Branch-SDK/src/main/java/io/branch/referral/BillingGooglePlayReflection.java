package io.branch.referral;

import java.lang.ClassNotFoundException;

public class BillingGooglePlayReflection {
    public static BillingGooglePlayInterface getBillingLibraryVersion() {
        String billingClient = com.android.billingclient.BuildConfig.VERSION_NAME;
        BillingGooglePlayInterface billingInterface;

        try {
            int majorIndex = billingClient.indexOf(".");
            String majorVersion = billingClient.substring(0, majorIndex);

            switch (majorVersion) {
                case "8":
                    billingInterface = new BillingGooglePlayV8();
                    break;
                case "7":
                case "6":
                    billingInterface = new BillingGooglePlayV6V7();
                    break;
                default:
                    billingInterface = new BillingGooglePlayDefault();
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error parsing billing client version: " + e.getMessage());

            billingInterface = new BillingGooglePlayDefault();
        }

        return billingInterface;
    }
}
