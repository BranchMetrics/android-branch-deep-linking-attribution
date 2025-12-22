package io.branch.referral

import io.branch.interfaces.GooglePlayBillingWrapper

object GooglePlayBillingManager {
    fun getBillingImplementation(): GooglePlayBillingWrapper? {
        // Try to load V8 first
        try {
            val classVal = Class.forName("com.branch.billing.v8.BillingV8Implementation")
            return classVal.getDeclaredConstructor().newInstance() as GooglePlayBillingWrapper
        } catch (e: Exception) {
            // V8 not found, try V6
        }

        try {
            val classVal = Class.forName("io.branch.referral.GooglePlayBillingLibraryV6")
            val getInstanceMethod = classVal.getMethod("getInstance")
            return getInstanceMethod.invoke(null) as GooglePlayBillingWrapper
        } catch (e: Exception) {
            // Neither version is linked in the user's app
            BranchLogger.e("No Billing Library dependency found!")
            return null
        }
    }
}