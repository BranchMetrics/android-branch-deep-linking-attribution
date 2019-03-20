package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Evan on 7/14/17.
 * <p>
 * Util class for Instant App functions.
 * </p>
 */

class InstantAppUtil {
    private static Boolean isInstantApp = null;
    private static Context lastApplicationContext = null;
    private static PackageManagerWrapper packageManagerWrapper = null;
    
    static boolean isInstantApp(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            return false;
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
                    isInstantApp = Boolean.TRUE;
                } catch (ClassNotFoundException var4) {
                    isInstantApp = Boolean.FALSE;
                }
            }
            
            return isInstantApp;
        }
    }
    
    private static boolean isAtLeastO() {
        return Build.VERSION.SDK_INT > 25 || isPreReleaseOBuild();
    }
    
    private static boolean isPreReleaseOBuild() {
        return !"REL".equals(Build.VERSION.CODENAME) && ("O".equals(Build.VERSION.CODENAME) || Build.VERSION.CODENAME.startsWith("OMR"));
    }
    
    @SuppressWarnings("ConstantConditions")
    static boolean doShowInstallPrompt(@NonNull Activity activity, int requestCode, @Nullable String referrer) {
        if (activity == null) {
            PrefHelper.Debug("Unable to show install prompt. Activity is null");
            return false;
        } else if (!isInstantApp(activity)) {
            PrefHelper.Debug("Unable to show install prompt. Application is not an instant app");
            return false;
        } else {
            Intent intent = (new Intent("android.intent.action.VIEW")).setPackage("com.android.vending").addCategory("android.intent.category.DEFAULT")
                    .putExtra("callerId", activity.getPackageName())
                    .putExtra("overlay", true);
            Uri.Builder uriBuilder = (new Uri.Builder()).scheme("market").authority("details").appendQueryParameter("id", activity.getPackageName());
            if (!TextUtils.isEmpty(referrer)) {
                uriBuilder.appendQueryParameter("referrer", referrer);
            }
            
            intent.setData(uriBuilder.build());
            activity.startActivityForResult(intent, requestCode);
            return true;
        }
    }
    
    @SuppressWarnings("RedundantArrayCreation")
    private static class PackageManagerWrapper {
        private final PackageManager packageManager;
        private static Method isInstantAppMethod;
        
        PackageManagerWrapper(PackageManager packageManager) {
            this.packageManager = packageManager;
        }
        
        Boolean isInstantApp() {
            if (!isAtLeastO()) {
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
