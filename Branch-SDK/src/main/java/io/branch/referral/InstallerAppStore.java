package io.branch.referral;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

final class InstallerAppStore {

    private InstallerAppStore() {
    }

    /**
     * Returns the package name of the app that installed packageName, or null if it is unknown
     * or can't be read.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    static String getInstallerPackageName(PackageManager packageManager, String packageName, int sdkInt) {
        try {
            if (sdkInt >= Build.VERSION_CODES.R) {
                return packageManager.getInstallSourceInfo(packageName).getInstallingPackageName();
            } else {
                return packageManager.getInstallerPackageName(packageName);
            }
        } catch (Exception e) {
            BranchLogger.w("Caught exception reading installer package name: " + e);
            return null;
        }
    }

    /**
     * Maps an installer package name to its app_store value, or null if the installer isn't a mapped store.
     */
    static String installerToAppStore(String installer) {
        if (installer == null) {
            return null;
        }
        switch (installer) {
            case "com.android.vending":
                return Defines.Jsonkey.Google_Play_Store.getKey();
            case "com.huawei.appmarket":
                return Defines.Jsonkey.Huawei_App_Gallery.getKey();
            case "com.sec.android.app.samsungapps":
                return Defines.Jsonkey.Samsung_Galaxy_Store.getKey();
            case "com.xiaomi.mipicks":
            case "com.xiaomi.market":
                return Defines.Jsonkey.Xiaomi_Get_Apps.getKey();
            default:
                return null;
        }
    }

    /**
     * Returns the app_store value to store, given the stored value and the installer's store.
     * A stored Meta value is always kept on install. On open, it is kept unless the installer
     * is a store other than Google Play and differs from the installer at the previous init.
     */
    static String resolveAppStore(String stored, String installerStore, boolean installerChanged, boolean isInstall) {
        if (Defines.Jsonkey.Meta_Install_Referrer.getKey().equals(stored)
                && (isInstall || !installerChanged || installerStore == null
                    || Defines.Jsonkey.Google_Play_Store.getKey().equals(installerStore))) {
            return Defines.Jsonkey.Meta_Install_Referrer.getKey();
        }
        return installerStore;
    }

    /**
     * Reads the installer of this app and saves the resulting app_store value in prefHelper.
     * Does nothing while tracking is disabled.
     */
    static void updateAppStoreSource(Context context, PrefHelper prefHelper, boolean isInstall) {
        if (prefHelper.getBool(PrefHelper.KEY_TRACKING_STATE)) {
            BranchLogger.v("Tracking is disabled, not updating app_store");
            return;
        }
        String installer = getInstallerPackageName(context.getPackageManager(), context.getPackageName(), Build.VERSION.SDK_INT);
        String installerStore = installerToAppStore(installer);
        String recordedInstallerStore = installerStore == null ? PrefHelper.NO_STRING_VALUE : installerStore;
        boolean installerChanged = prefHelper.hasPrefValue(PrefHelper.KEY_INSTALLER_APP_STORE)
                && !recordedInstallerStore.equals(prefHelper.getString(PrefHelper.KEY_INSTALLER_APP_STORE));

        String stored = prefHelper.getAppStoreSource();
        if (PrefHelper.NO_STRING_VALUE.equals(stored)) {
            stored = null;
        }
        String appStore = resolveAppStore(stored, installerStore, installerChanged, isInstall);
        prefHelper.setAppStoreSource(appStore == null ? PrefHelper.NO_STRING_VALUE : appStore);
        prefHelper.setString(PrefHelper.KEY_INSTALLER_APP_STORE, recordedInstallerStore);
        BranchLogger.v("Installer package: " + installer + ", app_store: " + appStore);
    }
}
