package io.branch.referral;

/**
 * <p>An Interface class that is implemented by all classes that make use of
 * {@link BranchLinkShareListener}, defining methods to listen for link sharing status.</p>
 */
public interface BranchLinkShareListener {
    /**
     * <p> Callback method to update when share link dialog is launched.</p>
     */
    void onShareLinkDialogLaunched();

    /**
     * <p> Callback method to update when sharing dialog is dismissed.</p>
     */
    void onShareLinkDialogDismissed();

    /**
     * <p> Callback method to update the sharing status. Called on sharing completed or on error.</p>
     *
     * @param sharedLink    The link shared to the channel.
     * @param sharedChannel Channel selected for sharing.
     * @param error         A {@link BranchError} to update errors, if there is any.
     */
    void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error);

    /**
     * <p>Called when user select a channel for sharing a deep link.
     * Branch will create a deep link for the selected channel and share with it after calling this
     * method. On sharing complete, status is updated by onLinkShareResponse() callback. Consider
     * having a sharing in progress UI if you wish to prevent user activity in the window between selecting a channel
     * and sharing complete.</p>
     *
     * @param channelName Name of the selected application to share the link. An empty string is returned if unable to resolve selected client name.
     */
    void onChannelSelected(String channelName);
}
