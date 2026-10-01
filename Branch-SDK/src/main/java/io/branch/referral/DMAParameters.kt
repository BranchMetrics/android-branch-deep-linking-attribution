package io.branch.referral

/**
 * DMA consent fields for the EEA region. Build with [Builder], then pass to
 * [BranchConfiguration.Builder.setDMAParameters] at init, or to [Branch.setDMAParameters] when
 * consent changes later.
 *
 * [eeaRegion] states whether European regulation applies to this user at all; the two consent
 * fields record the user's answer to a consent prompt. Outside the EEA the consent fields carry
 * no meaning.
 *
 * Java:
 * ```java
 * DMAParameters dma = new DMAParameters.Builder()
 *         .setEeaRegion(true)
 *         .setAdPersonalizationConsent(false)
 *         .setAdUserDataUsageConsent(true)
 *         .build();
 * ```
 */
class DMAParameters private constructor(
    val eeaRegion: Boolean,
    val adPersonalizationConsent: Boolean,
    val adUserDataUsageConsent: Boolean,
    private val warnings: List<String>
) {

    /**
     * Emitted by whichever consumer takes this object, rather than from [Builder.build], because at
     * build time the logger is still disabled and the lines would be lost.
     */
    @JvmName("logWarnings")
    internal fun logWarnings() {
        warnings.forEach { BranchLogger.w(it) }
    }

    /** Returns a [Builder] seeded with these values, for changing one field. */
    fun toBuilder(): Builder = Builder()
        .setEeaRegion(eeaRegion)
        .setAdPersonalizationConsent(adPersonalizationConsent)
        .setAdUserDataUsageConsent(adUserDataUsageConsent)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DMAParameters) return false
        return eeaRegion == other.eeaRegion &&
            adPersonalizationConsent == other.adPersonalizationConsent &&
            adUserDataUsageConsent == other.adUserDataUsageConsent
    }

    override fun hashCode(): Int {
        var result = eeaRegion.hashCode()
        result = 31 * result + adPersonalizationConsent.hashCode()
        result = 31 * result + adUserDataUsageConsent.hashCode()
        return result
    }

    override fun toString(): String =
        "DMAParameters(eeaRegion=$eeaRegion, " +
            "adPersonalizationConsent=$adPersonalizationConsent, " +
            "adUserDataUsageConsent=$adUserDataUsageConsent)"

    class Builder {
        private var eeaRegion: Boolean = false
        private var adPersonalizationConsent: Boolean = false
        private var adUserDataUsageConsent: Boolean = false

        fun setEeaRegion(inEeaRegion: Boolean) = apply { eeaRegion = inEeaRegion }

        fun setAdPersonalizationConsent(granted: Boolean) = apply { adPersonalizationConsent = granted }

        fun setAdUserDataUsageConsent(granted: Boolean) = apply { adUserDataUsageConsent = granted }

        fun build(): DMAParameters {
            val warnings = mutableListOf<String>()
            if (!eeaRegion && (adPersonalizationConsent || adUserDataUsageConsent)) {
                warnings += "DMAParameters: consent was granted with eeaRegion false. Consent only " +
                    "applies to users in the EEA — call setEeaRegion(true) if DMA applies to this user."
            }
            return DMAParameters(
                eeaRegion,
                adPersonalizationConsent,
                adUserDataUsageConsent,
                warnings.toList()
            )
        }
    }
}
