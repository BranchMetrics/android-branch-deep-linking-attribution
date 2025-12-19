package io.branch.referral

import io.branch.interfaces.GooglePlayBillingWrapper

object GooglePlayBillingManager {
    fun getBillingImplementation(): GooglePlayBillingWrapper? {
        // Try to load V8 first
        try {
            val clazz = Class.forName("com.branch.billing.v8.BillingV8Implementation")
            return clazz.getConstructor().newInstance() as GooglePlayBillingWrapper
        } catch (e: ClassNotFoundException) {
            // V8 not found, try V6
        }

        try {
            val clazz = Class.forName("com.branch.billing.v6.BillingV6Implementation")
            return clazz.getConstructor().newInstance() as GooglePlayBillingWrapper
        } catch (e: ClassNotFoundException) {
            // Neither version is linked in the user's app
            BranchLogger.e("No Billing Library dependency found!")
            return null
        }
    }
}