package io.branch.referral;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.branch.coroutines.InstallReferrersKt;
import io.branch.data.InstallReferrerResult;

@RunWith(JUnit4.class)

public class InstallReferrerResultTests {
    Context context;
    PrefHelper prefHelper;

    @Test
    public void allValidReferrersLatestWins(){
        InstallReferrerResult test1 = new InstallReferrerResult(
                "test1",
                1,
                "test1",
                1,
                true
        );

        InstallReferrerResult test2 = new InstallReferrerResult(
                "test2",
                Long.MAX_VALUE,
                "test2",
                Long.MAX_VALUE,
                true
        );

        InstallReferrerResult test3 = new InstallReferrerResult(
                "test3",
                2,
                "test3",
                2,
                true
        );

        InstallReferrerResult test4 = new InstallReferrerResult(
                "test4",
                3,
                "test4",
                3,
                true
        );

        List<InstallReferrerResult> installReferrers = new ArrayList<>();
        installReferrers.add(test1);
        installReferrers.add(test2);
        installReferrers.add(test3);
        installReferrers.add(test4);

        InstallReferrerResult actual = InstallReferrersKt.getLatestValidReferrerStore(installReferrers);
        Assert.assertEquals(test2, actual);
    }

    @Test
    public void NoValidReferrersReturnsNull(){
        List<InstallReferrerResult> installReferrers = new ArrayList<>();
        installReferrers.add(null);
        installReferrers.add(null);
        installReferrers.add(null);
        installReferrers.add(null);

        InstallReferrerResult actual = InstallReferrersKt.getLatestValidReferrerStore(installReferrers);
        Assert.assertNull(actual);
    }

    @Test
    public void oneValidReferrerReturnsItself(){
        InstallReferrerResult test1 = new InstallReferrerResult(
                "test1",
                1,
                "test1",
                1,
                true
        );

        List<InstallReferrerResult> installReferrers = new ArrayList<>();
        installReferrers.add(test1);

        InstallReferrerResult actual = InstallReferrersKt.getLatestValidReferrerStore(installReferrers);
        Assert.assertEquals(test1, actual);
    }

    @Test
    public void testMetaInstallReferrerCases() {
        // Case 1: Meta referrer is click-through with non-organic Play Store referrer
        InstallReferrerResult metaReferrerClickThrough = new InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, true);
        InstallReferrerResult playStoreReferrer = new InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play&utm_medium=cpc", 1700000000, true);
        List<InstallReferrerResult> allReferrers = Arrays.asList(metaReferrerClickThrough, playStoreReferrer);
        InstallReferrerResult result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(metaReferrerClickThrough, result);

        // Case 2: Meta referrer is view-through with organic Play Store referrer
        InstallReferrerResult metaReferrerViewThrough = new InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, false);
        InstallReferrerResult latestPlayStoreReferrer = new InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play&utm_medium=organic", 0, true);
        allReferrers = Arrays.asList(metaReferrerViewThrough, latestPlayStoreReferrer);
        result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(metaReferrerViewThrough, result);

        // Case 3: Meta referrer is view-through with non-organic Play Store referrer
        metaReferrerViewThrough = new InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, false);
        latestPlayStoreReferrer = new InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play&utm_medium=cpc", 1700000000, true);
        allReferrers = Arrays.asList(metaReferrerViewThrough, latestPlayStoreReferrer);
        result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(latestPlayStoreReferrer, result);

        // Case 4: Meta referrer is outdated click-through with non-organic Play Store referrer
        metaReferrerClickThrough = new InstallReferrerResult("Meta", 1700000030, "referrer", 1700000000, true);
        latestPlayStoreReferrer = new InstallReferrerResult("PlayStore", 1700000500, "utm_source=google-play&utm_medium=cpc", 1700000450, true);
        allReferrers = Arrays.asList(metaReferrerClickThrough, latestPlayStoreReferrer);
        result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(latestPlayStoreReferrer, result);

        // Case 5: Meta, Google Play, and Samsung Referrer (latest) are available
        metaReferrerClickThrough = new InstallReferrerResult("Meta", 1700000000, "referrer", 1700000000, true);
        latestPlayStoreReferrer = new InstallReferrerResult("PlayStore", 1700000000, "utm_source=google-play&utm_medium=cpc", 1700000000, true);
        InstallReferrerResult samsungReferrer = new InstallReferrerResult("Samsung", 1700001000, "utm_source=samsung-store&utm_medium=cpc", 1700001000, true);
        allReferrers = Arrays.asList(metaReferrerClickThrough, latestPlayStoreReferrer, samsungReferrer);
        result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(samsungReferrer, result);
    }

}
