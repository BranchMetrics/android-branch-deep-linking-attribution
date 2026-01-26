package io.branch.interfaces

import android.content.Context
interface GooglePlayBillingInterface {
    fun connect()
    fun logEventWithPurchase(context: Context, purchase: Any)
}