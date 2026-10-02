package io.branch.referral.util

/**
 * How the per-target link variants are produced when per-target channel attribution is enabled.
 */
enum class PerTargetLinkStrategy {
    /**
     * Build each variant client side with no network call at all.
     *
     * Free and instant, but the shared URL is a long Branch URL rather than the short one, which is
     * a user visible change to what gets pasted into the target app.
     */
    LONG,

    /**
     * Request one short link per mapped target before opening the chooser.
     *
     * Keeps the URL shape users expect, at the cost of N link creations per share instead of one.
     * The chooser cannot open until they all return, so this trades latency for URL aesthetics.
     */
    SHORT
}

/**
 * Opt-in configuration for putting `~channel` back on the link when sharing through the OS native
 * share sheet (EMT-3881).
 *
 * Android's chooser resolves `Intent.EXTRA_TEXT` eagerly, before the user picks a target, so unlike
 * iOS the SDK cannot generate one link after the choice is known. The workaround is
 * `Intent.EXTRA_REPLACEMENT_EXTRAS`: supply a per package `EXTRA_TEXT` up front and let the chooser
 * hand the matching one to whichever target the user picks.
 *
 * **Disabled by default, and deliberately so.** `config_chooserActivity` is an official OEM override
 * point and there is no evidence yet that Samsung One UI or Xiaomi HyperOS honour
 * `EXTRA_REPLACEMENT_EXTRAS`. On Android 13+ the chooser is also a Play-updatable mainline module
 * that can change independently of the OS version. Until the device spike in
 * `spikes/emt-3881-replacement-extras/` reports back, this stays off.
 *
 * When disabled, sharing behaves exactly as it does today.
 */
object PerTargetChannelConfig {

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var strategy: PerTargetLinkStrategy = PerTargetLinkStrategy.LONG

    /**
     * Turns per-target channel attribution on or off. Off by default.
     */
    @JvmStatic
    fun setEnabled(value: Boolean) {
        enabled = value
    }

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    /**
     * Chooses how the per-target variants are built. Defaults to [PerTargetLinkStrategy.LONG],
     * which costs no network calls.
     *
     * [PerTargetLinkStrategy.SHORT] preserves the short URL shape but issues one link creation per
     * mapped target and blocks the chooser until they finish. Pick it only against measured latency
     * for a realistic target count.
     */
    @JvmStatic
    fun setLinkStrategy(value: PerTargetLinkStrategy) {
        strategy = value
    }

    @JvmStatic
    fun getLinkStrategy(): PerTargetLinkStrategy = strategy

    @Volatile
    private var targetPackages: Set<String>? = null

    /**
     * Restricts per-target attribution to [packages], instead of every package
     * [BranchChannelMap] knows about.
     *
     * This matters most under [PerTargetLinkStrategy.SHORT], where each target costs one link
     * creation and the chooser cannot open until they all return. Narrowing to the handful of
     * channels actually measured keeps that cost bounded. Passing null restores the full map.
     */
    @JvmStatic
    fun setTargetPackages(packages: Set<String>?) {
        targetPackages = packages?.toSet()
    }

    /**
     * The packages that will receive a channel-tagged link variant: the configured allowlist
     * intersected with what [BranchChannelMap] can name, or the whole map when unset.
     */
    @JvmStatic
    fun resolveTargetPackages(): Set<String> {
        val mapped = BranchChannelMap.mappedPackages()
        val allowlist = targetPackages ?: return mapped
        return allowlist.intersect(mapped)
    }

    /**
     * Restores the shipped defaults, disabled and [PerTargetLinkStrategy.LONG]. Intended for tests.
     */
    @JvmStatic
    fun reset() {
        enabled = false
        strategy = PerTargetLinkStrategy.LONG
        targetPackages = null
    }
}
