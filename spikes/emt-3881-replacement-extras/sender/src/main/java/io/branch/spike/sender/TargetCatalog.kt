package io.branch.spike.sender

/**
 * Chooser targets probed by this spike: our own receiver app (always
 * present, guaranteed to show up) plus a handful of real-world share
 * targets likely to be installed on a test device.
 */
object TargetCatalog {
    const val RECEIVER_PACKAGE = "io.branch.spike.receiver"

    val PACKAGES = listOf(
        RECEIVER_PACKAGE,
        "com.whatsapp",
        "com.facebook.katana",
        "com.google.android.gm",
        "com.instagram.android",
    )
}
