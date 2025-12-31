package io.branch.referral

import io.branch.interfaces.GooglePlayBillingWrapper
import java.util.ServiceLoader

object BranchModuleManager {
    // Google Play Billing wrapper
    private var billingModule: GooglePlayBillingWrapper? = null

    fun initializeModules() {
        // Find and load Google Play Billing Module
        val billingLoader = ServiceLoader.load(GooglePlayBillingWrapper::class.java)
        billingModule = billingLoader.iterator().asSequence().firstOrNull()
        if (billingModule != null) {
            BranchLogger.v("Billing module found and loaded.")
            billingModule?.connect()
        } else {
            BranchLogger.w("No Billing Module found. Billing features disabled.")
        }

        // Repeat above code to add new modules
    }

    fun getBillingImplementation(): GooglePlayBillingWrapper? {
        return billingModule
    }

    // Repeat above code to add new wrappers
}