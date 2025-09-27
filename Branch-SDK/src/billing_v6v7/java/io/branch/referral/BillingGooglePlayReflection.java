package io.branch.referral;

import io.branch.referral.BillingGooglePlayV6V7;
import java.lang.ClassNotFoundException;

public class BillingGooglePlayReflection {
    public static BillingGooglePlayInterface getBillingLibraryVersion() {
        return new BillingGooglePlayV6V7();
    }
}