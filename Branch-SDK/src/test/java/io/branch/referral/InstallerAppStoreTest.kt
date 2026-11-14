package io.branch.referral

import android.content.Context
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import io.branch.coroutines.getLatestValidReferrerStore
import io.branch.data.InstallReferrerResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(JUnit4::class)
class InstallerAppStoreTest {

    private val packageName = "io.branch.test"

    @Test
    fun api30AndAboveUsesInstallSourceInfo() {
        val packageManager = mock(PackageManager::class.java)
        val installSourceInfo = mock(InstallSourceInfo::class.java)
        `when`(installSourceInfo.installingPackageName).thenReturn("com.android.vending")
        `when`(packageManager.getInstallSourceInfo(packageName)).thenReturn(installSourceInfo)

        assertEquals("com.android.vending", getInstallerPackageName(packageManager, packageName, 30))
        @Suppress("DEPRECATION")
        verify(packageManager, never()).getInstallerPackageName(anyString())
    }

    @Test
    fun belowApi30UsesGetInstallerPackageName() {
        val packageManager = mock(PackageManager::class.java)
        @Suppress("DEPRECATION")
        `when`(packageManager.getInstallerPackageName(packageName)).thenReturn("com.huawei.appmarket")

        assertEquals("com.huawei.appmarket", getInstallerPackageName(packageManager, packageName, 29))
        verify(packageManager, never()).getInstallSourceInfo(anyString())
    }

    @Test
    fun api30AndAboveErrorsReturnNull() {
        listOf(
            mock(PackageManager.NameNotFoundException::class.java),
            SecurityException("denied")
        ).forEach { error ->
            val packageManager = mock(PackageManager::class.java)
            `when`(packageManager.getInstallSourceInfo(packageName)).thenThrow(error)
            assertNull(getInstallerPackageName(packageManager, packageName, 30))
        }
    }

    @Test
    fun belowApi30ErrorsReturnNull() {
        listOf(
            mock(PackageManager.NameNotFoundException::class.java),
            SecurityException("denied"),
            IllegalArgumentException("unknown package")
        ).forEach { error ->
            val packageManager = mock(PackageManager::class.java)
            @Suppress("DEPRECATION")
            `when`(packageManager.getInstallerPackageName(packageName)).thenAnswer { throw error }
            assertNull(getInstallerPackageName(packageManager, packageName, 29))
        }
    }

    @Test
    fun installerMapping() {
        assertEquals("PlayStore", installerToAppStore("com.android.vending"))
        assertEquals("AppGallery", installerToAppStore("com.huawei.appmarket"))
        assertEquals("GalaxyStore", installerToAppStore("com.sec.android.app.samsungapps"))
        assertEquals("GetApps", installerToAppStore("com.xiaomi.mipicks"))
        assertEquals("GetApps", installerToAppStore("com.xiaomi.market"))
        assertNull(installerToAppStore("com.amazon.venezia"))
        assertNull(installerToAppStore("com.google.android.packageinstaller"))
        assertNull(installerToAppStore(""))
        assertNull(installerToAppStore(null))
    }

    @Test
    fun precedenceOnOpen() {
        assertEquals("Meta", resolveAppStore("Meta", "PlayStore", false))
        assertEquals("Meta", resolveAppStore("Meta", null, false))
        assertEquals("GalaxyStore", resolveAppStore("Meta", "GalaxyStore", false))
        assertEquals("PlayStore", resolveAppStore("AppGallery", "PlayStore", false))
        assertNull(resolveAppStore("PlayStore", null, false))
        assertNull(resolveAppStore(null, null, false))
        assertEquals("PlayStore", resolveAppStore(null, "PlayStore", false))
    }

    @Test
    fun precedenceOnInstall() {
        assertEquals("Meta", resolveAppStore("Meta", "PlayStore", true))
        assertEquals("Meta", resolveAppStore("Meta", null, true))
        assertEquals("Meta", resolveAppStore("Meta", "GalaxyStore", true))
        assertEquals("GalaxyStore", resolveAppStore(null, "GalaxyStore", true))
        assertNull(resolveAppStore(null, null, true))
    }

    @Test
    fun metaReferrerWinnerKeptWithPlayInstaller() {
        val metaClickThrough = InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, 1, 1, true)
        val playStore = InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play&utm_medium=cpc", 1700000000, 1, 1, true)

        val winner = getLatestValidReferrerStore(listOf(playStore, null, null, null, metaClickThrough))

        assertEquals(metaClickThrough, winner)
        assertEquals("Meta", resolveAppStore(winner!!.appStore, installerToAppStore("com.android.vending"), true))
    }

    @Test
    fun updateAppStoreSourceWritesInstallerStore() {
        val prefHelper = mockPrefHelper(PrefHelper.NO_STRING_VALUE)
        updateAppStoreSource(mockContext("com.sec.android.app.samsungapps"), prefHelper, false)
        verify(prefHelper).setAppStoreSource("GalaxyStore")
    }

    @Test
    fun updateAppStoreSourceKeepsMetaWithPlayInstaller() {
        val prefHelper = mockPrefHelper("Meta")
        updateAppStoreSource(mockContext("com.android.vending"), prefHelper, false)
        verify(prefHelper).setAppStoreSource("Meta")
    }

    @Test
    fun updateAppStoreSourceKeepsMetaOnInstallWithOtherStoreInstaller() {
        val prefHelper = mockPrefHelper("Meta")
        updateAppStoreSource(mockContext("com.sec.android.app.samsungapps"), prefHelper, true)
        verify(prefHelper).setAppStoreSource("Meta")
    }

    @Test
    fun updateAppStoreSourceClearsStoreForUnknownInstaller() {
        val prefHelper = mockPrefHelper("PlayStore")
        updateAppStoreSource(mockContext(null), prefHelper, false)
        verify(prefHelper).setAppStoreSource(PrefHelper.NO_STRING_VALUE)
    }

    @Test
    fun updateAppStoreSourceSkippedWhenTrackingDisabled() {
        val prefHelper = mockPrefHelper(PrefHelper.NO_STRING_VALUE)
        `when`(prefHelper.getBool(PrefHelper.KEY_TRACKING_STATE)).thenReturn(true)
        val context = mockContext("com.android.vending")

        updateAppStoreSource(context, prefHelper, false)

        verify(prefHelper, never()).setAppStoreSource(any())
        verify(context, never()).packageManager
    }

    private fun mockContext(installer: String?): Context {
        val packageManager = mock(PackageManager::class.java)
        @Suppress("DEPRECATION")
        `when`(packageManager.getInstallerPackageName(packageName)).thenReturn(installer)
        val context = mock(Context::class.java)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(context.packageName).thenReturn(packageName)
        return context
    }

    private fun mockPrefHelper(stored: String): PrefHelper {
        val prefHelper = mock(PrefHelper::class.java)
        `when`(prefHelper.appStoreSource).thenReturn(stored)
        return prefHelper
    }
}
