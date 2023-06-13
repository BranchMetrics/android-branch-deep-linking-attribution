package io.branch.referral;

/**
 * <p>An interface class for customizing sharing properties with selected channel.</p>
 */
public interface IChannelProperties {
    /**
     * @param channel The name of the channel selected for sharing.
     * @return {@link String} with value for the message title for sharing the link with the selected channel
     */
    String getSharingTitleForChannel(String channel);

    /**
     * @param channel The name of the channel selected for sharing.
     * @return {@link String} with value for the message body for sharing the link with the selected channel
     */
    String getSharingMessageForChannel(String channel);
}
