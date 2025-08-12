package io.branch.referral

/**
 * Extension to make Branch implement BranchSessionStateProvider
 */
fun Branch.asSessionStateProvider(): BranchSessionStateProvider {
    return object : BranchSessionStateProvider {
        override fun isInitialized(): Boolean = hasActiveSession() && canPerformOperations()
        override fun isInitializing(): Boolean = hasActiveSession() && !canPerformOperations()
        override fun isUninitialized(): Boolean = !hasActiveSession()
    }
} 