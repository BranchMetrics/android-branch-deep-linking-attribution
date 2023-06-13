package io.branch.referral;

/**
 * <p>An Interface class that is implemented by all classes that make use of
 * {@link BranchLinkCreateListener}, defining a single method that takes a URL
 * {@link String} format, and an error message of {@link BranchError} format that will be
 * returned on failure of the request response.</p>
 *
 * @see String
 * @see BranchError
 */
public interface BranchLinkCreateListener {
    void onLinkCreate(String url, BranchError error);
}
