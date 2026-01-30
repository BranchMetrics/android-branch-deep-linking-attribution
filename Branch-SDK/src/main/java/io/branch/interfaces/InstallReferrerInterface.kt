package io.branch.interfaces

import android.content.Context

interface InstallReferrerInterface {
    fun connect()
    fun fetchInstallReferrerData(context: Context, callback: InstallReferrerFetchEvents)
}
interface InstallReferrerFetchEvents {
    fun onInstallReferrersFinished()
}