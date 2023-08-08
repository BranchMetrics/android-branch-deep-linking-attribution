package io.branch.referral;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
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
                1
        );

        InstallReferrerResult test2 = new InstallReferrerResult(
                "test2",
                Long.MAX_VALUE,
                "test2",
                Long.MAX_VALUE
        );

        InstallReferrerResult test3 = new InstallReferrerResult(
                "test3",
                2,
                "test3",
                2
        );

        InstallReferrerResult test4 = new InstallReferrerResult(
                "test4",
                3,
                "test4",
                3
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
                1
        );

        List<InstallReferrerResult> installReferrers = new ArrayList<>();
        installReferrers.add(test1);

        InstallReferrerResult actual = InstallReferrersKt.getLatestValidReferrerStore(installReferrers);
        Assert.assertEquals(test1, actual);
    }
}
