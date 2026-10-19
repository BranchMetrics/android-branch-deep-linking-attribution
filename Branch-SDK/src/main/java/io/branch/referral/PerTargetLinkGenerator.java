package io.branch.referral;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchChannelMap;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.PerTargetChannelConfig;
import io.branch.referral.util.PerTargetLinkStrategy;

/**
 * Builds one channel-tagged link per share target and packs them into the bundle that
 * {@link Intent#EXTRA_REPLACEMENT_EXTRAS} expects.
 *
 * <p>Android resolves the chooser's extras eagerly, before the user picks anything, so the SDK
 * cannot do what iOS does and generate a single link once the choice is known. Instead every mapped
 * target gets its own link up front and the chooser hands over the matching one.</p>
 *
 * <p>Per package only, never per component: a package exposing several share activities receives one
 * variant covering all of them. That ceiling matches the one iOS has via activity type.</p>
 */
class PerTargetLinkGenerator {

    /** Receives the finished replacement bundle, or null when there is nothing to override. */
    interface Callback {
        void onVariantsReady(@Nullable Bundle replacementExtras);
    }

    private PerTargetLinkGenerator() {
    }

    /**
     * Produces the per-target replacement extras for a share.
     *
     * <p>Falls back to a null bundle, meaning "share the default link everywhere", whenever the
     * feature is off, no target is mapped, or link generation yields nothing usable. The caller can
     * always proceed with the share.</p>
     *
     * @param context        context used to build the links
     * @param buo            the object being shared
     * @param linkProperties the base link properties; its channel is overridden per target
     * @param callback       invoked once the bundle is ready, on the thread that finished the work
     */
    static void generate(@NonNull Context context,
                         @NonNull BranchUniversalObject buo,
                         @NonNull LinkProperties linkProperties,
                         @NonNull Callback callback) {
        if (!PerTargetChannelConfig.isEnabled()) {
            callback.onVariantsReady(null);
            return;
        }

        // An alias names one specific link, so it cannot be spread across N per-target variants:
        // under SHORT every variant past the first would collide on the server, and either way the
        // integrator asked for a single fixed URL. Honour that and skip per-target attribution.
        if (!TextUtils.isEmpty(linkProperties.getAlias())) {
            BranchLogger.v("Per-target channel attribution skipped: link alias '"
                    + linkProperties.getAlias() + "' pins the share to a single URL");
            callback.onVariantsReady(null);
            return;
        }

        Set<String> packages = PerTargetChannelConfig.resolveTargetPackages();
        if (packages.isEmpty()) {
            BranchLogger.v("Per-target channel attribution enabled but no target packages resolved");
            callback.onVariantsReady(null);
            return;
        }

        if (PerTargetChannelConfig.getLinkStrategy() == PerTargetLinkStrategy.LONG) {
            callback.onVariantsReady(buildLongVariants(context, buo, linkProperties, packages));
        } else {
            buildShortVariants(context, buo, linkProperties, packages, callback);
        }
    }

    /**
     * Builds every variant on device. No network, so this returns synchronously.
     */
    @Nullable
    private static Bundle buildLongVariants(@NonNull Context context,
                                            @NonNull BranchUniversalObject buo,
                                            @NonNull LinkProperties linkProperties,
                                            @NonNull Set<String> packages) {
        Map<String, String> urlsByPackage = new HashMap<>();
        for (String packageName : packages) {
            String channel = BranchChannelMap.channelForPackage(packageName);
            if (channel == null) {
                continue;
            }
            String url = buo.getLongUrl(context, copyWithChannel(linkProperties, channel));
            if (url != null) {
                urlsByPackage.put(packageName, url);
            }
        }
        return toReplacementExtras(urlsByPackage);
    }

    /**
     * Requests one short link per target and reports once the last one settles.
     *
     * <p>Every target is dispatched before waiting on any of them, so the total wait is the slowest
     * link rather than the sum. A target whose link fails is simply left out and falls back to the
     * default link.</p>
     */
    private static void buildShortVariants(@NonNull Context context,
                                           @NonNull BranchUniversalObject buo,
                                           @NonNull LinkProperties linkProperties,
                                           @NonNull Set<String> packages,
                                           @NonNull Callback callback) {
        final Map<String, String> urlsByPackage = new HashMap<>();
        final AtomicInteger outstanding = new AtomicInteger(packages.size());

        for (final String packageName : packages) {
            final String channel = BranchChannelMap.channelForPackage(packageName);
            if (channel == null) {
                settle(outstanding, urlsByPackage, callback);
                continue;
            }
            buo.generateShortUrl(context, copyWithChannel(linkProperties, channel),
                    new Branch.BranchLinkCreateListener() {
                        @Override
                        public void onLinkCreate(String url, BranchError error) {
                            if (error == null && url != null) {
                                synchronized (urlsByPackage) {
                                    urlsByPackage.put(packageName, url);
                                }
                            } else if (error != null) {
                                BranchLogger.v("Per-target link failed for " + packageName + ": " + error.getMessage());
                            }
                            settle(outstanding, urlsByPackage, callback);
                        }
                    }, true);
        }
    }

    /**
     * Marks one target done and fires the callback when it was the last outstanding one.
     */
    private static void settle(@NonNull AtomicInteger outstanding,
                               @NonNull Map<String, String> urlsByPackage,
                               @NonNull Callback callback) {
        if (outstanding.decrementAndGet() > 0) {
            return;
        }
        Bundle extras;
        synchronized (urlsByPackage) {
            extras = toReplacementExtras(urlsByPackage);
        }
        callback.onVariantsReady(extras);
    }

    /**
     * Shapes the per-package urls into the nested bundle {@link Intent#EXTRA_REPLACEMENT_EXTRAS}
     * requires: an outer bundle keyed by package name, each value a bundle of extras to merge.
     */
    @Nullable
    private static Bundle toReplacementExtras(@NonNull Map<String, String> urlsByPackage) {
        if (urlsByPackage.isEmpty()) {
            return null;
        }
        Bundle replacementExtras = new Bundle();
        for (Map.Entry<String, String> entry : urlsByPackage.entrySet()) {
            Bundle targetExtras = new Bundle();
            targetExtras.putString(Intent.EXTRA_TEXT, entry.getValue());
            replacementExtras.putBundle(entry.getKey(), targetExtras);
        }
        return replacementExtras;
    }

    /**
     * Copies [linkProperties] with [channel] swapped in, leaving the caller's instance untouched
     * so the same properties object can back every variant.
     */
    @NonNull
    private static LinkProperties copyWithChannel(@NonNull LinkProperties source, @NonNull String channel) {
        LinkProperties copy = new LinkProperties()
                .setChannel(channel)
                .setFeature(source.getFeature())
                .setStage(source.getStage())
                .setCampaign(source.getCampaign())
                .setDuration(source.getMatchDuration());
        // Alias is deliberately not copied: generate() already refused the share path when one is
        // set, so every variant here is alias-free by construction.

        if (source.getTags() != null) {
            for (String tag : source.getTags()) {
                copy.addTag(tag);
            }
        }
        if (source.getControlParams() != null) {
            for (Map.Entry<String, String> entry : source.getControlParams().entrySet()) {
                copy.addControlParameter(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}
