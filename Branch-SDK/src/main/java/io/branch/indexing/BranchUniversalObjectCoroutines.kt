package io.branch.indexing

import android.content.Context
import io.branch.referral.BranchException
import io.branch.referral.createLink
import io.branch.referral.util.LinkProperties

/**
 * Coroutine counterpart of [BranchUniversalObject.generateShortUrl]. Main-safe.
 *
 * @param defaultToLongUrl when true (the default), returns a long URL instead of throwing.
 * @throws BranchException if no URL could be produced.
 */
suspend fun BranchUniversalObject.createLink(
    context: Context,
    linkProperties: LinkProperties,
    defaultToLongUrl: Boolean = true
): String =
    getLinkBuilder(context, linkProperties)
        .setDefaultToLongUrl(defaultToLongUrl)
        .createLink()
