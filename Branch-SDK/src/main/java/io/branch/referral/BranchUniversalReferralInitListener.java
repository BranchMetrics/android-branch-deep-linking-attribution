package io.branch.referral;

import androidx.annotation.Nullable;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;

/**
 * <p>An Interface class that is implemented by all classes that make use of
 * {@link BranchUniversalReferralInitListener}, defining a single method that provides
 * {@link BranchUniversalObject}, {@link LinkProperties} and an error message of {@link BranchError} format that will be
 * returned on failure of the request response.
 * In case of an error the value for {@link BranchUniversalObject} and {@link LinkProperties} are set to null.</p>
 *
 * @see BranchUniversalObject
 * @see LinkProperties
 * @see BranchError
 */
public interface BranchUniversalReferralInitListener {
    void onInitFinished(@Nullable BranchUniversalObject branchUniversalObject, @Nullable LinkProperties linkProperties, @Nullable BranchError error);
}
