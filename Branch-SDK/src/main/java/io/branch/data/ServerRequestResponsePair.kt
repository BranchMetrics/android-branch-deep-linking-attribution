package io.branch.data

import io.branch.referral.ServerRequest
import io.branch.referral.ServerResponse

data class ServerRequestResponsePair(
    var serverRequest: ServerRequest,
    var serverResponse: ServerResponse?
)
