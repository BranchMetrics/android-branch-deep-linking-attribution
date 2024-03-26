package io.branch.coroutines

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import io.branch.referral.BranchLogger.e
import io.branch.referral.BranchLogger.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Returns the user agent string on a background thread via static class WebSettings
 */
suspend fun getUserAgentAsync(context: Context): String? {
    return withContext(Dispatchers.Default) {
        v("Begin getUserAgentAsync " + Thread.currentThread())

        var result: String? = null
        try {
            result = WebSettings.getDefaultUserAgent(context)
        }
        catch (exception: Exception) {
            e("Failed to retrieve userAgent string. " + exception.message)
        }

        v("End getUserAgentAsync " + Thread.currentThread() + " " + result)
        result
    }
}

/**
 * Returns the user agent string on the main thread via WebView instance.
 */
suspend fun getUserAgentSync(context: Context): String?{
    return withContext(Dispatchers.Main){
        v("Begin getUserAgentSync " + Thread.currentThread())

        var result: String? = null
        try {
            val w = WebView(context)
            result = w.settings.userAgentString
            w.destroy()
        }
        catch (ex: Exception){
            e("Failed to retrieve userAgent string. " + ex.message)
        }

        v("End getUserAgentSync " + Thread.currentThread() + " " + result)
        result
    }
}