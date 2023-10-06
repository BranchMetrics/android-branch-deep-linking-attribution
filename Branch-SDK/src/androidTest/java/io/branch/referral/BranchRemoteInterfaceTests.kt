package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.branch.referral.network.BranchRemoteInterfaceUrlConnection
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BranchRemoteInterfaceTests: BranchTest() {
    private lateinit var branchRemoteInterface: BranchRemoteInterfaceUrlConnection

    @Before
    override fun setUp() {
        super.setUp()
        initBranchInstance()
        branchRemoteInterface = BranchRemoteInterfaceUrlConnection(branch)
    }

    @Test
    fun catch404(){
        branchRemoteInterface.make_restful_post(JSONObject(), "http://httpbin.org/status/404", "", "")
    }
}