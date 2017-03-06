package io.branch.search;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;

/**
 * Created by sojanpr on 3/2/17.
 * <p/>
 * Provides methods to Delete the contents added to Local search
 *
 * @see {@link BranchUniversalObject#listOnSamsungSearch()} and {@link SearchBuilder}
 * </p>
 */
public class DeleteFromSearch {
    /**
     * Delete the content with specified canonical id from Samsung local search
     *
     * @param canonicalID {@link String} canonical id of the content added
     * @return {@code true} if content is successfully deleted from Samsung Local search
     * @see {@link BranchUniversalObject#listOnSamsungSearch()}
     */
    public static boolean deleteFromSamsungSearch(String canonicalID) {
        BranchUniversalObject branchUniversalObject = new BranchUniversalObject();
        branchUniversalObject.setCanonicalIdentifier(canonicalID);
        return BranchSearchServiceConnection.getInstance().deleteFromIndex(branchUniversalObject, Branch.getInstance().getAppContext().getPackageName());
    }

    /**
     * Clears all contents from this application added to the Samsung local search
     *
     * @return {@code true} if contents are successfully cleared from Samsung Local search
     */
    public static boolean deleteAllFromSamsungSearch() {
        return BranchSearchServiceConnection.getInstance().deleteAllFromIndex(Branch.getInstance().getAppContext().getPackageName());
    }
}
