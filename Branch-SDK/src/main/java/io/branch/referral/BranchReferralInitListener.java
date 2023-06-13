package io.branch.referral;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * <p>An Interface class that is implemented by all classes that make use of
 * {@link BranchReferralInitListener}, defining a single method that takes a list of params in
 * {@link JSONObject} format, and an error message of {@link BranchError} format that will be
 * returned on failure of the request response.</p>
 *
 * @see JSONObject
 * @see BranchError
 */
public interface BranchReferralInitListener {
    void onInitFinished(@Nullable JSONObject referringParams, @Nullable BranchError error);
}
