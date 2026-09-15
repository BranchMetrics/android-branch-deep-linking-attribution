package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BranchExceptionTest {

    @Test
    fun preservesTheOriginalBranchError() {
        val error = BranchError("Trouble creating a URL.", BranchError.ERR_BRANCH_INVALID_REQUEST)

        val exception = BranchException(error)

        assertSame(error, exception.branchError)
        assertEquals(BranchError.ERR_BRANCH_INVALID_REQUEST, exception.branchError.errorCode)
    }

    @Test
    fun exposesTheBranchErrorMessageAsTheExceptionMessage() {
        val error = BranchError("Trouble creating a URL.", BranchError.ERR_BRANCH_INVALID_REQUEST)

        val exception = BranchException(error)

        assertEquals(error.message, exception.message)
    }
}
