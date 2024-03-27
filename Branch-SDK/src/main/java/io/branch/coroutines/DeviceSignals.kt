package io.branch.coroutines

import android.content.Context
import android.text.TextUtils
import android.webkit.WebSettings
import android.webkit.WebView
import io.branch.referral.Branch
import io.branch.referral.BranchLogger.e
import io.branch.referral.BranchLogger.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

val mutex = Mutex()

/**
 * Returns the user agent string on a background thread via static class WebSettings
 * This is the default behavior.
 *
 * Use a mutex to ensure only one is executed at a time.
 * Successive calls will return the cached value.
 *
 * For performance, this is called at the end of the init, or while awaiting init if enqueued prior.
 */
suspend fun getUserAgentAsync(context: Context): String? {
    return withContext(Dispatchers.Default) {
        mutex.withLock {
            var result: String? = null

            if (!TextUtils.isEmpty(Branch._userAgentString)) {
                v("UserAgent cached " + Branch._userAgentString)
                result = Branch._userAgentString
            }
            else {
                try {
                    v("Begin getUserAgentAsync " + Thread.currentThread())
                    result = WebSettings.getDefaultUserAgent(context)
                    v("End getUserAgentAsync " + Thread.currentThread() + " " + result)
                }
                catch (exception: Exception) {
                    e("Failed to retrieve userAgent string. " + exception.message)
                }
            }

            result
        }
    }
}

/**
 * Returns the user agent string on the main thread via WebView instance.
 * Use when facing errors with the WebSettings static API.
 * https://bugs.chromium.org/p/chromium/issues/detail?id=1279562
 * https://bugs.chromium.org/p/chromium/issues/detail?id=1271617
 *
 *
 * Because there is only one main thread, this function will only execute one at a time.
 * Successive calls will return the cached value.
 */
suspend fun getUserAgentSync(context: Context): String?{
    return withContext(Dispatchers.Main){
        var result: String? = null

        if(!TextUtils.isEmpty(Branch._userAgentString)){
            v("UserAgent cached " + Branch._userAgentString)
            result = Branch._userAgentString
        }
        else {
            try {
                v("Begin getUserAgentSync " + Thread.currentThread())
                val w = WebView(context)
                result = w.settings.userAgentString
                w.destroy()
                v("End getUserAgentSync " + Thread.currentThread() + " " + result)
            }
            catch (ex: Exception) {
                e("Failed to retrieve userAgent string. " + ex.message)
            }
        }

        result
    }
}