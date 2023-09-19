package io.branch.channels

import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.ServerRequest
import io.branch.referral.ServerResponse
import io.branch.referral.TrackingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

val requestChannel = Channel <ServerRequest>(capacity = 1)
val mutex = Mutex()


suspend fun enqueue(request: ServerRequest) : ServerResponse {
    return withContext(Dispatchers.IO) {
        launch {
            val timeStamp = System.currentTimeMillis()
            BranchLogger.i("requestChannel enqueuing: $request" + " at $timeStamp" + " on" + Thread.currentThread().name)
            requestChannel.send(request)
        }
        executeNetworkRequest(requestChannel.receive())
    }
}

private suspend fun executeNetworkRequest(request: ServerRequest): ServerResponse {
    var result: ServerResponse
    mutex.withLock {
        BranchLogger.i("requestChannel executeNetworkRequest: $request" + Thread.currentThread().name)

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
    }

    return result
}