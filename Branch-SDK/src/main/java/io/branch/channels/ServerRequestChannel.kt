package io.branch.channels

import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.ServerRequest
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


suspend fun execute(request: ServerRequest) : ServerResponse {
    return withContext(Dispatchers.IO) {
        launch {
            requestChannel.send(request)
        }
        executeNetworkRequest(requestChannel.receive())
    }
}

private suspend fun executeNetworkRequest(request: ServerRequest): ServerResponse {
    var result: ServerResponse
    mutex.withLock {
        BranchLogger.v("ServerRequestChannel executeNetworkRequest: $request" + Thread.currentThread().name)

        request.onPreExecute()
        request.updateRequestData()
        request.updatePostData()

        if (TrackingController.isTrackingDisabled(Branch.getInstance().applicationContext) && !request.prepareExecuteWithoutTracking()) {
            return ServerResponse(
                request.requestPath,
                BranchError.ERR_BRANCH_TRACKING_DISABLED,
                ""
            )
        }
        val branchKey = Branch.getInstance().prefHelper.branchKey

        result = if (request.isGetRequest) {
            Branch.getInstance().branchRemoteInterface.make_restful_get(
                request.requestUrl, request.getParams, request.requestPath, branchKey
            )
        }
        else {
            Branch.getInstance().branchRemoteInterface.make_restful_post(
                request.post, request.requestUrl, request.requestPath, branchKey
            )
        }

        //TODO in between passing the result back and sending the next request
        // it is possible to race against setting on the prefhelper
        // must complete all writes before onPreExecute reads

    }

    return result
}