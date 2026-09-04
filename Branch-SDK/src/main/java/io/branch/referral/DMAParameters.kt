package io.branch.referral

/** DMA consent fields for EEA region. Pass to [BranchConfiguration.Builder.setDMAParameters]. */
data class DMAParameters(
    val eeaRegion: Boolean,
    val adPersonalizationConsent: Boolean,
    val adUserDataUsageConsent: Boolean
)
