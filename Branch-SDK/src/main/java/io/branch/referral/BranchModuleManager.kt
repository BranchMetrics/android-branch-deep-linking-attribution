package io.branch.referral

import io.branch.interfaces.GooglePlayBillingWrapper
import java.util.ServiceLoader

object BranchModuleManager {
    private var googlePlayBillingModule: GooglePlayBillingWrapper? = null

    fun initializeModules() {
        val googlePlayBillingLoader = ServiceLoader.load(GooglePlayBillingWrapper::class.java)
        googlePlayBillingModule = googlePlayBillingLoader.iterator().asSequence().firstOrNull()
        if (googlePlayBillingModule != null) {
            BranchLogger.v("Google Play Billing module found and loaded.")
            googlePlayBillingModule?.connect()
        } else {
            BranchLogger.w("No Google Play Billing Module found. Google Play Billing features disabled.")
        }
    }

    fun getGooglePlayBillingImplementation(): GooglePlayBillingWrapper? {
        return googlePlayBillingModule
    }
}