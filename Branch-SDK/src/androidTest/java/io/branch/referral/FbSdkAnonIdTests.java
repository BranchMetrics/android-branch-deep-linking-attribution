package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.DependencyUtilsKt;

@RunWith(AndroidJUnit4.class)
public class FbSdkAnonIdTests extends BranchTest {
    private static final String META_ANON_ID = "XZ6f1c2a3e-1111-4222-8333-944455556666";
    private static final String LEGACY_GUID = "70831425-43f3-4534-ab8e-41d6f326a022";
    private static final String ANON_ID_KEY = Defines.Jsonkey.AnonID.getKey();

    private DeviceInfo deviceInfo;
    private PrefHelper prefHelper;

    private void initWithStub() {
        TestFbSdkAppEventsLogger.reset();
        initBranchInstance(TEST_KEY);
        deviceInfo = DeviceInfo.getInstance();
        Assert.assertNotNull(deviceInfo);
        deviceInfo.fbSdkLoggerClassName = TestFbSdkAppEventsLogger.class.getName();
        prefHelper = PrefHelper.getInstance(getTestContext());
    }

    @After
    public void resetStub() {
        TestFbSdkAppEventsLogger.reset();
    }

    private JSONObject installPost() {
        return new ServerRequestRegisterInstall(getTestContext(), null, false).getPost();
    }

    private JSONObject openPost() {
        return new ServerRequestRegisterOpen(getTestContext(), null, false).getPost();
    }

    private JSONObject eventUserData() throws Exception {
        ServerRequest event = new ServerRequestLogEvent(getTestContext(), Defines.RequestPath.TrackStandardEvent,
                "PURCHASE", new HashMap<String, Object>(), new JSONObject(), new JSONObject(), new ArrayList<BranchUniversalObject>());
        return event.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey());
    }

    @Test
    public void testValidValueReturned() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());
    }

    @Test
    public void testNullValueReturnsNull() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;
        Assert.assertNull(deviceInfo.fbSdkAnonId());
    }

    @Test
    public void testEmptyValueReturnsNull() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = "";
        Assert.assertNull(deviceInfo.fbSdkAnonId());
    }

    @Test
    public void testNonXZValueReturnsNull() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = LEGACY_GUID;
        Assert.assertNull(deviceInfo.fbSdkAnonId());

        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = "XZ";
        Assert.assertNull(deviceInfo.fbSdkAnonId());
    }

    @Test
    public void testMetaSdkAbsentReturnsNull() {
        initWithStub();
        // The real Meta class name; the Meta SDK is not on this module's test classpath.
        deviceInfo.fbSdkLoggerClassName = DependencyUtilsKt.metaAppEventsLoggerClass;
        Assert.assertNull(deviceInfo.fbSdkAnonId());

        // Class present but without the accessor.
        deviceInfo.fbSdkLoggerClassName = Object.class.getName();
        Assert.assertNull(deviceInfo.fbSdkAnonId());
    }

    @Test
    public void testValidValueIsCached() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());

        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = "XZsomething-else";
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());
        Assert.assertEquals(1, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    public void testInvalidValueIsNotCachedAndRetried() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;
        Assert.assertNull(deviceInfo.fbSdkAnonId());
        Assert.assertEquals(1, TestFbSdkAppEventsLogger.callCount.get());

        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());
        Assert.assertEquals(2, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    public void testNewDeviceInfoRereadsMeta() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());

        String nextLaunchId = "XZ00000000-aaaa-4bbb-8ccc-dddddddddddd";
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = nextLaunchId;
        DeviceInfo nextLaunch = new DeviceInfo(getTestContext());
        nextLaunch.fbSdkLoggerClassName = TestFbSdkAppEventsLogger.class.getName();
        Assert.assertEquals(nextLaunchId, nextLaunch.fbSdkAnonId());
        Assert.assertEquals(2, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedPrefHelperGetterReturnsMetaValueAndSetterIsIgnored() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, prefHelper.getAnonID());

        prefHelper.setAnonID("custom-value");
        Assert.assertEquals(META_ANON_ID, prefHelper.getAnonID());
        Assert.assertFalse(prefHelper.hasPrefValue(PrefHelper.KEY_ANON_ID));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedPrefHelperGetterReturnsNullWithoutMeta() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;
        Assert.assertNull(prefHelper.getAnonID());
    }

    @Test
    public void testLegacyGuidNeverUsed() throws Exception {
        initWithStub();
        prefHelper.setString(PrefHelper.KEY_ANON_ID, LEGACY_GUID);

        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;
        Assert.assertFalse(installPost().has(ANON_ID_KEY));
        Assert.assertFalse(eventUserData().has(ANON_ID_KEY));

        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, installPost().getString(ANON_ID_KEY));
        Assert.assertEquals(META_ANON_ID, eventUserData().getString(ANON_ID_KEY));

        // The legacy value is left in place, untouched.
        Assert.assertEquals(LEGACY_GUID, prefHelper.getString(PrefHelper.KEY_ANON_ID));
    }

    @Test
    public void testSentOnInstallOpenAndEvent() throws Exception {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;

        Assert.assertEquals(META_ANON_ID, installPost().getString(ANON_ID_KEY));
        Assert.assertEquals(META_ANON_ID, openPost().getString(ANON_ID_KEY));
        Assert.assertEquals(META_ANON_ID, eventUserData().getString(ANON_ID_KEY));
    }

    @Test
    public void testOmittedWhenMetaAbsent() throws Exception {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;

        Assert.assertFalse(installPost().has(ANON_ID_KEY));
        Assert.assertFalse(openPost().has(ANON_ID_KEY));
        Assert.assertFalse(eventUserData().has(ANON_ID_KEY));
    }

    @Test
    public void testOmittedAtAttributionLevelNone() throws Exception {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        prefHelper.setConsumerProtectionAttributionLevel(Defines.BranchAttributionLevel.NONE);

        Assert.assertFalse(installPost().has(ANON_ID_KEY));
        Assert.assertFalse(openPost().has(ANON_ID_KEY));
        Assert.assertFalse(eventUserData().has(ANON_ID_KEY));
    }

    @Test
    public void testSentAtAttributionLevelsOtherThanNone() throws Exception {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;

        Defines.BranchAttributionLevel[] levels = {
                Defines.BranchAttributionLevel.FULL,
                Defines.BranchAttributionLevel.REDUCED,
                Defines.BranchAttributionLevel.MINIMAL
        };
        for (Defines.BranchAttributionLevel level : levels) {
            prefHelper.setConsumerProtectionAttributionLevel(level);
            Assert.assertEquals(level.toString(), META_ANON_ID, installPost().getString(ANON_ID_KEY));
            Assert.assertEquals(level.toString(), META_ANON_ID, eventUserData().getString(ANON_ID_KEY));
        }
    }
}
