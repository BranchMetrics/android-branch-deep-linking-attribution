package io.branch.referral;

import java.lang.ClassNotFoundException;

public class BillingGooglePlayReflection {
    public static BillingGooglePlayInterface getBillingLibraryVersion() {
        try {
            // Check for a class added in version 8.0 or higher
            Class billingClientBuilderClass = Class.forName("com.android.billingclient.api.BillingClient$Builder");
            billingClientBuilderClass.getMethod("enableAutoServiceReconnection");
            return new BillingGooglePlayV8();
        } catch (NoSuchMethodException | ClassNotFoundException version8CheckFailed) {
            try {
                // Check for a class added in version 7.0 or higher
                Class.forName("com.android.billingclient.api.ProductDetails$InstallmentPlanDetails");

                return new BillingGooglePlayV6V7();
            } catch (ClassNotFoundException version7CheckFailed) {
                try {
                    // Check for a class added in version 6.0 or higher
                    Class.forName("com.android.billingclient.api.BillingFlowParams$SubscriptionUpdateParams$ReplacementMode");
                    return new BillingGooglePlayV6V7();
                } catch (ClassNotFoundException version6CheckFailed) {
                    return new BillingGooglePlayV6V7();
                }
            }
        }
    }
}
