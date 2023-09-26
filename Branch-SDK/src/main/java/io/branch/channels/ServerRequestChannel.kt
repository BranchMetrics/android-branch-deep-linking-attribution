package io.branch.channels

import io.branch.data.ServerRequestResponsePair
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.ServerRequest
import io.branch.referral.ServerRequestQueue
import io.branch.referral.ServerResponse
import io.branch.referral.TrackingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

val requestChannel = Channel <ServerRequest>(RENDEZVOUS)
val mutex = Mutex()


suspend fun execute(request: ServerRequest) : ServerRequestResponsePair {
    return withContext(Dispatchers.Main) {
        launch {
            BranchLogger.v("ServerRequestChannel sending " + request + " on thread " + Thread.currentThread().name )
            requestChannel.send(request)
        }
        executeNetworkRequest(requestChannel.receive())
    }
}

private suspend fun executeNetworkRequest(request: ServerRequest): ServerRequestResponsePair {
    var result: ServerResponse
    mutex.withLock {
        BranchLogger.v("ServerRequestChannel executeNetworkRequest: $request on thread " + Thread.currentThread().name)

        request.onPreExecute()
        request.updateRequestData()
        request.updatePostData()

        if (TrackingController.isTrackingDisabled(Branch.getInstance().applicationContext) && !request.prepareExecuteWithoutTracking()) {
            return ServerRequestResponsePair(request, ServerResponse(
                request.requestPath,
                BranchError.ERR_BRANCH_TRACKING_DISABLED,
                ""
            ))
        }

        val branchKey = Branch.getInstance().prefHelper.branchKey

        result = withContext(Dispatchers.IO) {
            if (request.isGetRequest) {
                Branch.getInstance().branchRemoteInterface.make_restful_get(
                    request.requestUrl, request.getParams, request.requestPath, branchKey
                )
            }
            else {
                Branch.getInstance().branchRemoteInterface.make_restful_post(
                    request.post, request.requestUrl, request.requestPath, branchKey
                )
            }
        }

        ServerRequestQueue.getInstance(Branch.getInstance().applicationContext).onPostExecuteInner(request, result)
    }

    return ServerRequestResponsePair(request, result)
}