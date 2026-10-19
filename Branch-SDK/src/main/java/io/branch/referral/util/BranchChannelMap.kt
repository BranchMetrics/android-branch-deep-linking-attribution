package io.branch.referral.util

/**
 * Maps an Android share-target package name to a human readable channel name.
 *
 * The names are deliberately aligned with the iOS SDK's
 * `+[BranchActivityItemProvider humanReadableChannelWithActivityType:]` dictionary so that the
 * same app reports the same `~channel` string on both platforms. Before this map existed the
 * same app could surface under three different names: the iOS friendly name, the 5.x Android
 * device app label (locale dependent) and the 6.x raw `ComponentName`.
 *
 * Only packages present in this map take part in per-target channel attribution. Unmapped
 * targets intentionally keep the existing behaviour and receive the unmodified link.
 */
object BranchChannelMap {

    private val defaultChannels: Map<String, String> = mapOf(
        // Messaging
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp",
        "com.facebook.orca" to "Facebook Messenger",
        "org.telegram.messenger" to "Telegram",
        "com.viber.voip" to "Viber",
        "com.skype.raider" to "Skype",
        "jp.naver.line.android" to "LINE",
        "com.tencent.mm" to "WeChat",
        "com.discord" to "Discord",
        "com.Slack" to "Slack",
        "com.microsoft.teams" to "Microsoft Teams",

        // SMS / MMS
        "com.google.android.apps.messaging" to "SMS",
        "com.samsung.android.messaging" to "SMS",
        "com.android.mms" to "SMS",

        // Email
        "com.google.android.gm" to "Email",
        "com.google.android.email" to "Email",
        "com.microsoft.office.outlook" to "Email",

        // Social
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "Twitter",
        "com.x.android" to "Twitter",
        "com.instagram.android" to "Instagram",
        "com.snapchat.android" to "Snapchat",
        "com.linkedin.android" to "LinkedIn",
        "com.reddit.frontpage" to "Reddit",
        "com.pinterest" to "Pinterest",
        "com.sina.weibo" to "Weibo",
        "com.zhiliaoapp.musically" to "TikTok",

        // Media / storage
        "com.google.android.apps.docs" to "Google Drive",
        "com.yahoo.mobile.client.android.flickr" to "flickr",
        "com.vimeo.android.videoapp" to "Vimeo",
        "com.dropbox.android" to "Dropbox"
    )

    private val overrides: MutableMap<String, String> = LinkedHashMap()

    /**
     * Returns the channel name for [packageName], or `null` when the package is not mapped.
     *
     * A `null` return is meaningful: it tells the caller to leave the link untouched for that
     * target rather than guessing a channel name.
     */
    @JvmStatic
    fun channelForPackage(packageName: String?): String? {
        if (packageName.isNullOrEmpty()) return null
        return overrides[packageName] ?: defaultChannels[packageName]
    }

    /**
     * Registers or replaces the channel name used for [packageName].
     *
     * Lets an integrator cover a target Branch does not ship a mapping for, or relabel one, without
     * waiting for an SDK release.
     */
    @JvmStatic
    fun setChannelForPackage(packageName: String, channelName: String) {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
        require(channelName.isNotEmpty()) { "channelName must not be empty" }
        overrides[packageName] = channelName
    }

    /**
     * Every package that currently resolves to a channel name, defaults plus overrides.
     */
    @JvmStatic
    fun mappedPackages(): Set<String> = defaultChannels.keys + overrides.keys

    /**
     * Drops all registered overrides, restoring the shipped defaults. Intended for tests.
     */
    @JvmStatic
    fun clearOverrides() {
        overrides.clear()
    }
}
