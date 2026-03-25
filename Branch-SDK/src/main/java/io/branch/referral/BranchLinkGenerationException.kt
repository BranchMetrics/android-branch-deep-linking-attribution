package io.branch.referral

/**
 * Exception thrown during Branch link generation operations.
 * 
 * Specific exception types for different failure scenarios.
 */
sealed class BranchLinkGenerationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Link generation failed due to network timeout.
     */
    class TimeoutException(
        message: String = "Link generation timed out",
        cause: Throwable? = null
    ) : BranchLinkGenerationException(message, cause)
    
    /**
     * Link generation failed due to invalid request parameters.
     */
    class InvalidRequestException(
        message: String = "Invalid link generation request",
        cause: Throwable? = null
    ) : BranchLinkGenerationException(message, cause)
    
    /**
     * Link generation failed due to server error.
     */
    class ServerException(
        val statusCode: Int,
        message: String = "Server error during link generation",
        cause: Throwable? = null
    ) : BranchLinkGenerationException("$message (Status: $statusCode)", cause)
    
    /**
     * Link generation failed due to network connectivity issues.
     */
    class NetworkException(
        message: String = "Network error during link generation",
        cause: Throwable? = null
    ) : BranchLinkGenerationException(message, cause)
    
    /**
     * Link generation failed due to Branch SDK not being initialized.
     */
    class NotInitializedException(
        message: String = "Branch SDK not initialized",
        cause: Throwable? = null
    ) : BranchLinkGenerationException(message, cause)
    
    /**
     * Generic link generation failure.
     */
    class GeneralException(
        message: String = "Link generation failed",
        cause: Throwable? = null
    ) : BranchLinkGenerationException(message, cause)
}