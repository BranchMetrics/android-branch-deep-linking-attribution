package io.branch.coroutines

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
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
        var result: String? = null
        try {
            v("Retrieving userAgent string from WebSettings on thread " + Thread.currentThread())
            result = WebSettings.getDefaultUserAgent(context)
        }
        catch (exception: Exception) {
            e("Failed to retrieve userAgent string. " + exception.message)
        }

        result
    }
}

fun getUserAgentSync(context: Context): String?{
    var result: String? = null

    if(isMainThread){
        result = getWebViewUserAgent(context)
    }
    else {
        Handler(Looper.getMainLooper()).post {
            result = getWebViewUserAgent(context)
        }
    }

    return result
}

var isMainThread =
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
        Looper.getMainLooper().isCurrentThread
    }
    else {
        Thread.currentThread() === Looper.getMainLooper().thread
    }

fun getWebViewUserAgent(context: Context): String? {
    val result: String?
    v("Retrieving user agent from WebView instance on " + Thread.currentThread())
    val w = WebView(context)
    result = w.settings.userAgentString
    w.destroy()

    return result
}
