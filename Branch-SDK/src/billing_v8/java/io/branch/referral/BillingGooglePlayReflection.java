package io.branch.referral;

import io.branch.referral.BillingGooglePlayV8;
import java.lang.ClassNotFoundException;

public class BillingGooglePlayReflection {
    public static BillingGooglePlayInterface getBillingLibraryVersion() {
        return new BillingGooglePlayV8();
    }
}