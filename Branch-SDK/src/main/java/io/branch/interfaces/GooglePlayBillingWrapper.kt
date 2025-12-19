package io.branch.interfaces

import android.content.Context

interface GooglePlayBillingWrapper {
    fun connect()
    fun logEventWithPurchase(context: Context, purchase: Any)
}

