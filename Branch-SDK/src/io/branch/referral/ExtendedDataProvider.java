package io.branch.referral;

import android.content.Context;

/**
 * Created by sojanpr on 6/8/16.
 * <p>
 * Singleton class for used by Branch SDK report events to added third party SDK
 * </p>
 */
class ExtendedDataProvider {

    private static ExtendedDataProvider thisInstance_;
    PrefHelper prefHelper_;

    private ExtendedDataProvider(Context context) {
        prefHelper_ = PrefHelper.getInstance(context);
    }

    public static ExtendedDataProvider getInstance(Context context) {
        if (thisInstance_ == null) {
            thisInstance_ = new ExtendedDataProvider(context);
        }
        return thisInstance_;
    }

    /**
     * Pass the Branch event to integrated third party SDKs
     *
     * @param request  {@link ServerRequest} associated with the Branch event
     * @param response {@link ServerResponse} associated with the Branch event
     */
    public void provideData(ServerRequest request, ServerResponse response) {
        // If Fabric is enabled
        if (prefHelper_.getIsFabricEnabled()) {
            ExtendedAnswerProvider.provideData(request, response);
        }
    }
}
