package io.branch.referral.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Evan on 7/14/17.
 * <p>
 * Util class for Instant App functions.
 * </p>
 */

public class InstantAppUtil {
    private static Boolean isInstantApp = null;
    private static Context lastApplicationContext = null;
    private static PackageManagerWrapper packageManagerWrapper = null;

    public static boolean isInstantApp(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null");
        } else {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext == null) {
                throw new IllegalStateException("Application context is null!");
            } else if (isInstantApp != null && applicationContext.equals(lastApplicationContext)) {
                return isInstantApp;
            } else {
                isInstantApp = null;
                Boolean isInstantAppResult = null;
                if (isAtLeastO()) {
                    if (packageManagerWrapper == null || !applicationContext.equals(lastApplicationContext)) {
                        packageManagerWrapper = new PackageManagerWrapper(applicationContext.getPackageManager());
                    }
                    isInstantAppResult = packageManagerWrapper.isInstantApp();
                }
                lastApplicationContext = applicationContext;
                if (isInstantAppResult != null) {
                    isInstantApp = isInstantAppResult;
                } else {
                    try {
                        applicationContext.getClassLoader().loadClass("com.google.android.instantapps.supervisor.InstantAppsRuntime");
                        isInstantApp = true;
                    } catch (ClassNotFoundException var4) {
                        isInstantApp = false;
                    }
                }
                return isInstantApp;
            }
        }
    }

    private static boolean isAtLeastO() {
        return Build.VERSION.SDK_INT > 25 || isPreReleaseOBuild();
    }

    private static boolean isPreReleaseOBuild() {
        return !"REL".equals(Build.VERSION.CODENAME) && ("O".equals(Build.VERSION.CODENAME) || Build.VERSION.CODENAME.startsWith("OMR"));
    }

    @SuppressWarnings("RedundantArrayCreation")
    private static class PackageManagerWrapper {
        private final PackageManager packageManager;
        private static Method isInstantAppMethod;

        PackageManagerWrapper(PackageManager packageManager) {
            this.packageManager = packageManager;
        }

        Boolean isInstantApp() {
            if (isAtLeastO()) {
                return null;
            } else {
                if (isInstantAppMethod == null) {
                    try {
                        isInstantAppMethod = PackageManager.class.getDeclaredMethod("isInstantApp", new Class[0]);
                    } catch (NoSuchMethodException var3) {
                        return null;
                    }
                }
                try {
                    return (Boolean) isInstantAppMethod.invoke(this.packageManager, new Object[0]);
                } catch (IllegalAccessException var2) {
                    return null;
                } catch (InvocationTargetException var2) {
                    return null;
                }
            }
        }
    }
}
