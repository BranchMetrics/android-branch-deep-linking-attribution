package io.branch.referral;

/**
 * Returns a general error if the server back-end is down.
 */
public class BranchError {
    
    String errorMessage_ = "";
    int errorCode_ = ERR_BRANCH_NO_CONNECTIVITY;
    
    /* Error processing request since session not initialised yet. */
    public static final int ERR_NO_SESSION = -101;
    /* Error processing request since app doesn't have internet permission. */
    public static final int ERR_NO_INTERNET_PERMISSION = -102;
    /* Error processing request since referral code provided is invalid. */
    public static final int ERR_INVALID_REFERRAL_CODE = -103;
    /* Error processing request since Branch is not initialised. */
    public static final int ERR_BRANCH_INIT_FAILED = -104;
    /* Error processing request since alias is already used. */
    public static final int ERR_BRANCH_DUPLICATE_URL = -105;
    /* Error processing request since alias is already used. */
    public static final int ERR_BRANCH_DUPLICATE_REFERRAL_CODE = -106;
    /* Error redeeming rewards. */
    public static final int ERR_BRANCH_REDEEM_REWARD = -107;
    /* Error with API level below 14. */
    public static final int ERR_API_LVL_14_NEEDED = -108;
    /* Error Branch is not instantiated. */
    public static final int ERR_BRANCH_NOT_INSTANTIATED = -109;
    /* Error while creating share options. */
    public static final int ERR_BRANCH_NO_SHARE_OPTION = -110;
    /* Request Branch server timed out. */
    public static final int ERR_BRANCH_REQ_TIMED_OUT = -111;
    /* Request failed to hit branch servers */
    public static final int ERR_BRANCH_UNABLE_TO_REACH_SERVERS = -112;
    /* Request failed due to poor connectivity */
    public static final int ERR_BRANCH_NO_CONNECTIVITY = -113;
    /* Branch key is not specified or invalid */
    public static final int ERR_BRANCH_KEY_INVALID = -114;
    /* Request failed due to resource conflict */
    public static final int ERR_BRANCH_RESOURCE_CONFLICT = -115;
    /* Branch request is invalid */
    public static final int ERR_BRANCH_INVALID_REQUEST = -116;
    /* Tracking is disabled. Requested operations will not work when tracking is disabled */
    public static final int ERR_BRANCH_TRACKING_DISABLED = -117;
    /* Branch session is already initialized */
    public static final int ERR_BRANCH_ALREADY_INITIALIZED = -118;
    
    /**
     * <p>Returns the message explaining the error.</p>
     *
     * @return A {@link String} value that can be used in error logging or for dialog display
     * to the user.
     */
    public String getMessage() {
        return errorMessage_;
    }
    
    /**
     * <p>Returns an error code for this Branch Error. </p>
     *
     * @return An {@link Integer} specifying  the error code for this error. Value will be one of the error code defined in branch errors.
     */
    public int getErrorCode() {
        return errorCode_;
    }
    
    /**
     * <p>Overridden toString method for this object; returns the error message rather than the
     * object's address.</p>
     *
     * @return A {@link String} value representing the object's current state.
     */
    @Override
    public String toString() {
        return getMessage();
    }
    
    public BranchError(String failMsg, int statusCode) {
        errorMessage_ = failMsg + initErrorCodeAndGetLocalisedMessage(statusCode);
    }
    
    /*
     * <p> Provides localised error messages for the gives status code </p>
     *
     * @param status Http error code or Branch error codes
     *
     * @return A {@link String} with localised error message for the given status
     */
    private String initErrorCodeAndGetLocalisedMessage(int statusCode) {
        String errMsg;
        if (statusCode == ERR_BRANCH_NO_CONNECTIVITY) {
            errorCode_ = ERR_BRANCH_NO_CONNECTIVITY;
            errMsg = " Branch API Error: poor network connectivity. Please try again later.";
        } else if (statusCode == ERR_BRANCH_KEY_INVALID) {
            errorCode_ = ERR_BRANCH_KEY_INVALID;
            errMsg = " Branch API Error: Please enter your branch_key in your project's manifest file first.";
        } else if (statusCode == ERR_BRANCH_INIT_FAILED) {
            errorCode_ = ERR_BRANCH_INIT_FAILED;
            errMsg = " Did you forget to call init? Make sure you init the session before making Branch calls.";
        } else if (statusCode == ERR_NO_SESSION) {
            errorCode_ = ERR_NO_SESSION;
            errMsg = " Unable to initialize Branch. Check network connectivity or that your branch key is valid.";
        } else if (statusCode == ERR_NO_INTERNET_PERMISSION) {
            errorCode_ = ERR_NO_INTERNET_PERMISSION;
            errMsg = " Please add 'android.permission.INTERNET' in your applications manifest file.";
        } else if (statusCode == ERR_BRANCH_DUPLICATE_URL) {
            errorCode_ = ERR_BRANCH_DUPLICATE_URL;
            errMsg = " Unable to create a URL with that alias. If you want to reuse the alias, make sure to submit the same properties for all arguments and that the user is the same owner.";
        } else if (statusCode == ERR_BRANCH_DUPLICATE_REFERRAL_CODE) {
            errorCode_ = ERR_BRANCH_DUPLICATE_REFERRAL_CODE;
            errMsg = " That Branch referral code is already in use.";
        } else if (statusCode == ERR_BRANCH_REDEEM_REWARD) {
            errorCode_ = ERR_BRANCH_REDEEM_REWARD;
            errMsg = " Unable to redeem rewards. Please make sure you have credits available to redeem.";
        } else if (statusCode == ERR_API_LVL_14_NEEDED) {
            errorCode_ = ERR_API_LVL_14_NEEDED;
            errMsg = "BranchApp class can be used only" +
                    " with API level 14 or above. Please make sure your minimum API level supported is 14." +
                    " If you wish to use API level below 14 consider calling getInstance(Context) instead.";
        } else if (statusCode == ERR_BRANCH_NOT_INSTANTIATED) {
            errorCode_ = ERR_BRANCH_NOT_INSTANTIATED;
            errMsg = "Branch instance is not created." +
                    " Make  sure your Application class is an instance of BranchLikedApp.";
        } else if (statusCode == ERR_BRANCH_NO_SHARE_OPTION) {
            errorCode_ = ERR_BRANCH_NO_SHARE_OPTION;
            errMsg = " Unable create share options. Couldn't find applications on device to share the link.";
        } else if (statusCode == ERR_BRANCH_REQ_TIMED_OUT) {
            errorCode_ = ERR_BRANCH_REQ_TIMED_OUT;
            errMsg = " Request to Branch server timed out. Please check your internet connectivity";
        } else if (statusCode == ERR_BRANCH_TRACKING_DISABLED) {
            errorCode_ = ERR_BRANCH_TRACKING_DISABLED;
            errMsg = " Tracking is disabled. Requested operation cannot be completed when tracking is disabled";
        } else if (statusCode == ERR_BRANCH_ALREADY_INITIALIZED) {
            errorCode_ = ERR_BRANCH_ALREADY_INITIALIZED;
            errMsg = " Session initialization already happened. To force a new session, " +
                    "set intent extra, \"branch_force_new_session\", to true.";
        } else if (statusCode >= 500 || statusCode == ERR_BRANCH_UNABLE_TO_REACH_SERVERS) {
            errorCode_ = ERR_BRANCH_UNABLE_TO_REACH_SERVERS;
            errMsg = " Unable to reach the Branch servers, please try again shortly.";
        } else if (statusCode == 409 || statusCode == ERR_BRANCH_RESOURCE_CONFLICT) {
            errorCode_ = ERR_BRANCH_RESOURCE_CONFLICT;
            errMsg = " A resource with this identifier already exists.";
        } else if (statusCode >= 400 || statusCode == ERR_BRANCH_INVALID_REQUEST) {
            errorCode_ = ERR_BRANCH_INVALID_REQUEST;
            errMsg = " The request was invalid.";
        } else {
            errorCode_ = ERR_BRANCH_NO_CONNECTIVITY;
            errMsg = " Check network connectivity and that you properly initialized.";
        }
        return errMsg;
    }
}
