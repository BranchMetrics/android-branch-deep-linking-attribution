package com.branch.installreferrer

data class InstallReferrerResult(
    var appStore: String?,
    var installBeginTimestampSeconds: Long,
    var installReferrer: String?,
    var referrerClickTimestampSeconds: Long,
    var installBeginTimestampServerSeconds: Long?,
    var referrerClickTimestampServerSeconds: Long?,
    var isClickThrough: Boolean = true,
    var responseCode: Int = 0,
    var debugMessage: String? = null
)