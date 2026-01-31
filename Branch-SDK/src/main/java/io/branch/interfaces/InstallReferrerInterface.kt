package io.branch.interfaces

import android.content.Context

interface InstallReferrerInterface {
    fun fetchInstallReferrerData(context: Context, callback: InstallReferrerFetchEvents)
}
fun interface InstallReferrerFetchEvents {
    fun onInstallReferrersFinished()
}