package io.branch.referral;

import android.content.Context;
import android.content.SharedPreferences;

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

    private ServerRequest newEvent() {
        return new ServerRequestLogEvent(getTestContext(), Defines.RequestPath.TrackStandardEvent,
                "PURCHASE", new HashMap<String, Object>(), new JSONObject(), new JSONObject(), new ArrayList<BranchUniversalObject>());
    }

    // The anon_id is added in the background step that runs just before a request is sent.
    private static JSONObject sentPost(ServerRequest request) {
        request.doFinalUpdateOnBackgroundThread();
        return request.getPost();
    }

    private JSONObject installPost() {
        return sentPost(new ServerRequestRegisterInstall(getTestContext(), null, false));
    }

    private JSONObject openPost() {
        return sentPost(new ServerRequestRegisterOpen(getTestContext(), null, false));
    }

    private JSONObject eventUserData() throws Exception {
        return sentPost(newEvent()).getJSONObject(Defines.Jsonkey.UserData.getKey());
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
    public void testMetaSdkAbsentIsRemembered() {
        initWithStub();
        // The real Meta class name; the Meta SDK is not on this module's test classpath.
        deviceInfo.fbSdkLoggerClassName = DependencyUtilsKt.metaAppEventsLoggerClass;
        Assert.assertNull(deviceInfo.fbSdkAnonId());

        // Once the class is known to be missing, it is not looked up again.
        deviceInfo.fbSdkLoggerClassName = TestFbSdkAppEventsLogger.class.getName();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertNull(deviceInfo.fbSdkAnonId());
        Assert.assertEquals(0, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    public void testMetaAccessorMissingIsRemembered() {
        initWithStub();
        // Class present but without the accessor, as when R8 strips it.
        deviceInfo.fbSdkLoggerClassName = Object.class.getName();
        Assert.assertNull(deviceInfo.fbSdkAnonId());

        deviceInfo.fbSdkLoggerClassName = TestFbSdkAppEventsLogger.class.getName();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertNull(deviceInfo.fbSdkAnonId());
        Assert.assertEquals(0, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    public void testMetaThrowingIsRetried() {
        initWithStub();
        TestFbSdkAppEventsLogger.throwOnCall = true;
        Assert.assertNull(deviceInfo.fbSdkAnonId());
        Assert.assertEquals(1, TestFbSdkAppEventsLogger.callCount.get());

        TestFbSdkAppEventsLogger.throwOnCall = false;
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;
        Assert.assertEquals(META_ANON_ID, deviceInfo.fbSdkAnonId());
        Assert.assertEquals(2, TestFbSdkAppEventsLogger.callCount.get());
    }

    @Test
    public void testRequestConstructorDoesNotReadMeta() throws Exception {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = META_ANON_ID;

        ServerRequest install = new ServerRequestRegisterInstall(getTestContext(), null, false);
        ServerRequest event = newEvent();
        Assert.assertEquals(0, TestFbSdkAppEventsLogger.callCount.get());
        Assert.assertFalse(install.getPost().has(ANON_ID_KEY));
        Assert.assertFalse(event.getPost().getJSONObject(Defines.Jsonkey.UserData.getKey()).has(ANON_ID_KEY));

        Assert.assertEquals(META_ANON_ID, sentPost(install).getString(ANON_ID_KEY));
        Assert.assertEquals(META_ANON_ID, sentPost(event).getJSONObject(Defines.Jsonkey.UserData.getKey()).getString(ANON_ID_KEY));
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
    public void testDeprecatedPrefHelperGetterReturnsNoValueWithoutMeta() {
        initWithStub();
        TestFbSdkAppEventsLogger.anonymousAppDeviceGUID = null;
        Assert.assertEquals(PrefHelper.NO_STRING_VALUE, prefHelper.getAnonID());
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
    }

    @Test
    public void testLegacyGuidDeletedWhenPrefHelperCreated() {
        // An upgraded app starts with the value an older SDK version stored, before any PrefHelper exists.
        SharedPreferences prefs = getTestContext().getSharedPreferences("branch_referral_shared_pref", Context.MODE_PRIVATE);
        prefs.edit().putString(PrefHelper.KEY_ANON_ID, LEGACY_GUID).commit();

        initWithStub();
        Assert.assertFalse(prefHelper.hasPrefValue(PrefHelper.KEY_ANON_ID));
        Assert.assertFalse(prefs.contains(PrefHelper.KEY_ANON_ID));
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
