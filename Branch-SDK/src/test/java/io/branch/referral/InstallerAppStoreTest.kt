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
    fun precedence() {
        assertEquals("Meta", resolveAppStore("Meta", "PlayStore"))
        assertEquals("Meta", resolveAppStore("Meta", null))
        assertEquals("GalaxyStore", resolveAppStore("Meta", "GalaxyStore"))
        assertEquals("PlayStore", resolveAppStore("AppGallery", "PlayStore"))
        assertNull(resolveAppStore("PlayStore", null))
        assertNull(resolveAppStore(null, null))
        assertEquals("PlayStore", resolveAppStore(null, "PlayStore"))
    }

    @Test
    fun metaReferrerWinnerKeptWithPlayInstaller() {
        val metaClickThrough = InstallReferrerResult("Meta", 1700000050, "referrer", 1700000000, 1, 1, true)
        val playStore = InstallReferrerResult("PlayStore", 1700000030, "utm_source=google-play&utm_medium=cpc", 1700000000, 1, 1, true)

        val winner = getLatestValidReferrerStore(listOf(playStore, null, null, null, metaClickThrough))

        assertEquals(metaClickThrough, winner)
        assertEquals("Meta", resolveAppStore(winner!!.appStore, installerToAppStore("com.android.vending")))
    }

    @Test
    fun updateAppStoreSourceWritesInstallerStore() {
        val prefHelper = mockPrefHelper(PrefHelper.NO_STRING_VALUE)
        updateAppStoreSource(mockContext("com.sec.android.app.samsungapps"), prefHelper)
        verify(prefHelper).setAppStoreSource("GalaxyStore")
    }

    @Test
    fun updateAppStoreSourceKeepsMetaWithPlayInstaller() {
        val prefHelper = mockPrefHelper("Meta")
        updateAppStoreSource(mockContext("com.android.vending"), prefHelper)
        verify(prefHelper).setAppStoreSource("Meta")
    }

    @Test
    fun updateAppStoreSourceClearsStoreForUnknownInstaller() {
        val prefHelper = mockPrefHelper("PlayStore")
        updateAppStoreSource(mockContext(null), prefHelper)
        verify(prefHelper).setAppStoreSource(PrefHelper.NO_STRING_VALUE)
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
