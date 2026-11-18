package io.branch.referral

import android.content.Context
import android.text.TextUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class AppStoreReferrerTest {

    private val context = mock(Context::class.java)
    private val prefHelper = mock(PrefHelper::class.java)
    private lateinit var prefHelperStatic: MockedStatic<PrefHelper>
    private lateinit var textUtilsStatic: MockedStatic<TextUtils>

    @Before
    fun setUp() {
        prefHelperStatic = mockStatic(PrefHelper::class.java)
        prefHelperStatic.`when`<PrefHelper> { PrefHelper.getInstance(any()) }.thenReturn(prefHelper)
        textUtilsStatic = mockStatic(TextUtils::class.java)
        textUtilsStatic.`when`<Boolean> { TextUtils.isEmpty(any()) }
            .thenAnswer { (it.arguments[0] as CharSequence?).isNullOrEmpty() }
    }

    @After
    fun tearDown() {
        prefHelperStatic.close()
        textUtilsStatic.close()
    }

    @Test
    fun playReferrerSetsReferrerFieldsButNotAppStore() {
        AppStoreReferrer.processReferrerInfo(
            context,
            "link_click_id=123456&utm_source=google-play",
            1700000000,
            1700000030,
            "PlayStore",
            false,
            1700000031,
            1700000001
        )

        verify(prefHelper).setLinkClickIdentifier("123456")
        verify(prefHelper).setAppStoreReferrer("link_click_id=123456&utm_source=google-play")
        verify(prefHelper).setLong(PrefHelper.KEY_REFERRER_CLICK_TS, 1700000000)
        verify(prefHelper).setLong(PrefHelper.KEY_INSTALL_BEGIN_TS, 1700000030)
        verify(prefHelper).setLong(PrefHelper.KEY_INSTALL_BEGIN_SERVER_TS, 1700000031)
        verify(prefHelper).setLong(PrefHelper.KEY_REFERRER_CLICK_SERVER_TS, 1700000001)
        verify(prefHelper, never()).setAppStoreSource(anyString())
        verify(prefHelper, never()).setIsMetaClickThrough(anyBoolean())
    }

    @Test
    fun metaReferrerSetsAppStoreAndClickThrough() {
        AppStoreReferrer.processReferrerInfo(
            context,
            "utm_source=apps.facebook.com",
            1700000000,
            1700000050,
            "Meta",
            true,
            null,
            null
        )

        verify(prefHelper).setAppStoreSource("Meta")
        verify(prefHelper).setIsMetaClickThrough(true)
    }
}
