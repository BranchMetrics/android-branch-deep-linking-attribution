package io.branch.referral;

import android.content.Context;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub for the Meta SDK's {@code com.facebook.appevents.AppEventsLogger}. It has a different name so it never
 * collides with the real {@code facebook-core} if that is on the test classpath. Point
 * {@link DeviceInfo#fbSdkLoggerClassName} at this class to use it.
 */
public class TestFbSdkAppEventsLogger {
    static volatile String anonymousAppDeviceGUID;
    static final AtomicInteger callCount = new AtomicInteger();

    static void reset() {
        anonymousAppDeviceGUID = null;
        callCount.set(0);
    }

    public static String getAnonymousAppDeviceGUID(Context context) {
        callCount.incrementAndGet();
        return anonymousAppDeviceGUID;
    }
}
