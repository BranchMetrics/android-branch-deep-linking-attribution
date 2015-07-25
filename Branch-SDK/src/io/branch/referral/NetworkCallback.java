package io.branch.referral;

/**
 *	<p>High-level interface for all network callback classes.</p>
 */
interface NetworkCallback {

    /**
     * <p>Called upon completion of {@link NetworkCallback} or a descendant thereof.</p>
     *
     * @param serverResponse	A {@link ServerResponse} object containing the result of the
     * 							{@link NetworkCallback} action.
     */
    public void finished(ServerResponse serverResponse);
}
