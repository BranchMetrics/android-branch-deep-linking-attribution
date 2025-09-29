package io.branch.referral;

import java.lang.ClassNotFoundException;

import io.branch.referral.util.DependencyUtilsKt;

public class BillingGooglePlayReflection {
    public static BillingGooglePlayInterface getBillingLibraryVersion() {
        String billingClientVersionString = com.android.billingclient.BuildConfig.VERSION_NAME;
        BillingGooglePlayInterface billingInterface;

        try {
            String billingMajorVersion = DependencyUtilsKt.dependencyMajorVersionFinder(billingClientVersionString);
            switch (billingMajorVersion) {
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
            BranchLogger.e("Error parsing billing client version: " + e.getMessage());

            billingInterface = new BillingGooglePlayDefault();
        }

        return billingInterface;
    }
}
