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
        // Case 1: Meta referrer is click-through
        InstallReferrerResult metaReferrerClickThrough = new InstallReferrerResult("Meta", 1705614627, "referrer", 1705614648, true);
        InstallReferrerResult playStoreReferrer = new InstallReferrerResult("PlayStore", 1705614653, "referrer", 1705614648, true);
        List<InstallReferrerResult> allReferrers = Arrays.asList(metaReferrerClickThrough, playStoreReferrer);
        InstallReferrerResult result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(metaReferrerClickThrough, result);

        // Case 2: Meta referrer is view-through with organic Play Store referrer
        InstallReferrerResult metaReferrerViewThrough = new InstallReferrerResult("Meta", 1705616932, "referrer", 1705616955, false);
        InstallReferrerResult latestPlayStoreReferrer = new InstallReferrerResult("PlayStore", 1684358581, "utm_source=google-play&utm_medium=organic", 0, true);
        allReferrers = Arrays.asList(metaReferrerViewThrough, latestPlayStoreReferrer);
        result = InstallReferrersKt.getLatestValidReferrerStore(allReferrers);
        Assert.assertEquals(metaReferrerViewThrough, result);
    }
}
