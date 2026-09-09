package io.branch.referral

/**
 * Thrown by the coroutine variants of the Branch API. Carries the same [BranchError] the
 * callback form would have delivered.
 */
class BranchException(
    val branchError: BranchError
) : Exception(branchError.message)
