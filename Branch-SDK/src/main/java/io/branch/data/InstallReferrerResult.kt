package io.branch.data

data class InstallReferrerResult (var appStore: String?,
                                  var latestInstallTimestamp: Long,
                                  var latestRawReferrer: String?,
                                  var latestClickTimestamp: Long,
                                  var isClickThrough: Boolean = true)
