package io.branch.referral.indexing;

import java.util.HashMap;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

/**
 * <p>
 * Builder for creating a Registering content views to Branch.
 * Builds a RegisterView request wih given options and update Branch with the request.
 * </p>
 */
public class RegisterViewBuilder {
    private final String contentId_;
    private final String contentTitle_;
    private final String contentDesc_;
    private final String contentImgUrl_;
    private final HashMap<String, String> additionalParams_;
    private RegisterViewStatusListener callback_;

    /**
     * Created a builder for Registering view of contents with the given params
     *
     * @param contentId     A {@link String} with value of ID for the content
     * @param title         A {@link String} with value of title for the content
     * @param description   A {@link String} with value of description for the content
     * @param contentImgUrl A url associated with the content
     */
    public RegisterViewBuilder(String contentId, String title, String description, String contentImgUrl) {
        contentId_ = contentId;
        contentTitle_ = title;
        contentDesc_ = description;
        contentImgUrl_ = contentImgUrl;
        additionalParams_ = new HashMap<>();
    }

    /**
     * Adds any additional params that is needed to append to the content view event
     *
     * @param extraKey   A {@link String} with value of the key for the extra param
     * @param extraValue A {@link String} with extra param value
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @SuppressWarnings("unused")
    public RegisterViewBuilder addExtra(String extraKey, String extraValue) {
        additionalParams_.put(extraKey, extraValue);
        return this;
    }

    /**
     * Sets a callback to listen status events with register content view operation
     *
     * @param reportContentListener Callback for register view events
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @SuppressWarnings("unused")
    public RegisterViewBuilder setReportContentListener(RegisterViewStatusListener reportContentListener) {
        callback_ = reportContentListener;
        return this;
    }

    /**
     * Reports the content view to branch with the given parameters
     *
     * @return A {@link Boolean} whose value is false if unable to register the content view due any error in parameters
     * or if Branch is not initialised
     */
    @SuppressWarnings("unused")
    public boolean register() {
        boolean isRequestCreated = false;
        if (Branch.getInstance() != null) {
            isRequestCreated = true;
            Branch.getInstance().reportContentView(this);
        } else {
            if (callback_ != null) {
                callback_.onRegisterViewFinished(false, new BranchError("Register view error", BranchError.ERR_BRANCH_NOT_INSTANTIATED));
            }
        }

        return isRequestCreated;
    }

    /**
     * <p>
     * Callback interface for listening register content view status
     * </p>
     */
    public interface RegisterViewStatusListener {
        /**
         * Called on finishing the the register view process
         *
         * @param registered A {@link Boolean} which is set to true if register content view succeeded
         * @param error      An instance of {@link BranchError} to notify any error occurred during registering a content view event.
         *                   A null value is set if the registering content view succeeds
         */
        void onRegisterViewFinished(boolean registered, BranchError error);
    }

    public String getContentId() {
        return contentId_;
    }

    public HashMap<String, String> getAdditionalParams() {
        return additionalParams_;
    }

    public RegisterViewStatusListener getCallback() {
        return callback_;
    }

    public String getContentImgUrl() {
        return contentImgUrl_;
    }

    public String getContentTitle() {
        return contentTitle_;
    }


    public String getContentDesc() {
        return contentDesc_;
    }
}
