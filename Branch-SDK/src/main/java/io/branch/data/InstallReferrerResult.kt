package io.branch.data

data class InstallReferrerResult (var appStore: String?,
                                  var installBeginTimestampSeconds: Long,
                                  var installReferrer: String?,
                                  var referrerClickTimestampSeconds: Long,
                                  var installBeginTimestampServerSeconds: Long?,
                                  var referrerClickTimestampServerSeconds: Long?,
                                  var isClickThrough: Boolean = true)
