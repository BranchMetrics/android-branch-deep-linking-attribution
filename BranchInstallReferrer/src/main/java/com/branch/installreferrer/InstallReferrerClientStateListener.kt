package com.branch.installreferrer

interface InstallReferrerClientStateListener {
    fun onInstallReferrerServiceDisconnected()

    fun onInstallReferrerSetupFinished(var1: InstallReferrerResult)
}