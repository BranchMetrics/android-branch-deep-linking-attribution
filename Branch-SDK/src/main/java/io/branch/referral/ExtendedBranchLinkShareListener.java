package io.branch.referral;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;

/**
 * <p>An extended version of {@link BranchLinkShareListener} with callback that supports updating link data or properties after user select a channel to share
 * This will provide the extended callback {@link #onChannelSelected(String, BranchUniversalObject, LinkProperties)} only when sharing a link using Branch Universal Object.</p>
 */
public interface ExtendedBranchLinkShareListener extends BranchLinkShareListener {
    /**
     * <p>
     * Called when user select a channel for sharing a deep link.
     * This method allows modifying the link data and properties by providing the params  {@link BranchUniversalObject} and {@link LinkProperties}
     * </p>
     *
     * @param channelName    The name of the channel user selected for sharing a link
     * @param buo            {@link BranchUniversalObject} BUO used for sharing link for updating any params
     * @param linkProperties {@link LinkProperties} associated with the sharing link for updating the properties
     * @return Return {@code true} to create link with any updates added to the data ({@link BranchUniversalObject}) or to the properties ({@link LinkProperties}).
     * Return {@code false} otherwise.
     */
    boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties);
}
