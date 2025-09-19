package io.branch.referral;

import java.lang.ClassNotFoundException;

public class BillingGooglePlayReflection {
    public static String getBillingLibraryVersion() {
        try {
            // Check for a class added in version 8.0 or higher
            Class billingClientBuilderClass = Class.forName("com.android.billingclient.api.BillingClient$Builder");
            billingClientBuilderClass.getMethod("enableAutoServiceReconnection");
            return "Version 8.0 or higher";
        } catch (NoSuchMethodException | ClassNotFoundException version8CheckFailed) {
            try {
                // Check for a class added in version 7.0 or higher
                Class.forName("com.android.billingclient.api.ProductDetails$InstallmentPlanDetails");
                return "Version 7.0 or higher";
            } catch (ClassNotFoundException version7CheckFailed) {
                try {
                    // Check for a class added in version 6.0 or higher
                    Class.forName("com.android.billingclient.api.BillingFlowParams$SubscriptionUpdateParams$ReplacementMode");
                    return "Version 6.0 or higher";
                } catch (ClassNotFoundException version6CheckFailed) {
                    try {
                        // Check for the ProductDetails class, introduced in version 5.0
                        Class.forName("com.android.billingclient.api.ProductDetails");
                        return "Version 5.0 or higher";
                    } catch (ClassNotFoundException version5CheckFailed) {
                        try {
                            // If ProductDetails is not found, check for the older SkuDetails class
                            Class.forName("com.android.billingclient.api.SkuDetails");
                            return "Version 4.0 or older";
                        } catch (ClassNotFoundException version4CheckFailed) {
                            // If neither class is found, the library is not present or is a very old version
                            return "Not found or very old version";
                        }
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        String version = getBillingLibraryVersion();
        System.out.println("Detected Google Billing Library Version: " + version);
    }
}
