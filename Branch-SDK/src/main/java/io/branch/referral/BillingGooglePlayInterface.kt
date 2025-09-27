package io.branch.referral;

import android.content.Context;
import io.branch.referral.util.*;

import com.android.billingclient.api.Purchase;

public interface BillingGooglePlayInterface {
    fun logEventWithPurchase(context: Context, purchase: Purchase);
    fun startBillingClient(callback: (Boolean) -> Unit);
}
