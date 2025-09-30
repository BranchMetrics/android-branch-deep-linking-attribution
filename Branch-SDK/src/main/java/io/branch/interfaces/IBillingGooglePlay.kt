package io.branch.interfaces

import android.content.Context
import com.android.billingclient.api.Purchase

public interface IBillingGooglePlay {
    fun logEventWithPurchase(context: Context, purchase: Purchase);
    fun startBillingClient(callback: (Boolean) -> Unit);
}