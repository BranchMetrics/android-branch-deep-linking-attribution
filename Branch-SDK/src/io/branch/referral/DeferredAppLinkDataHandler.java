package io.branch.referral;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by sojanpr on 5/19/16.
 * Class for handling deferred app links
 */
class DeferredAppLinkDataHandler {
    private static final String NATIVE_URL_KEY = "com.facebook.platform.APPLINK_NATIVE_URL";

    public static Boolean fetchDeferredAppLinkData(Context context, final AppLinkFetchEvents callback) {
        boolean isRequestSucceeded = true;
        try {
            // Init FB SDK
            Class<?> FacebookSdkClass = Class.forName("com.facebook.FacebookSdk");
            Method initSdkMethod = FacebookSdkClass.getMethod("sdkInitialize", Context.class);
            initSdkMethod.invoke(null, context);

            final Class<?> AppLinkDataClass = Class.forName("com.facebook.applinks.AppLinkData");
            Class<?> AppLinkDataCompletionHandlerClass = Class.forName("com.facebook.applinks.AppLinkData$CompletionHandler");
            Method fetchDeferredAppLinkDataMethod = AppLinkDataClass.getMethod("fetchDeferredAppLinkData", Context.class, String.class, AppLinkDataCompletionHandlerClass);

            InvocationHandler ALDataCompletionHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("onDeferredAppLinkDataFetched") && args[0] != null) {
                        String appLinkUrl = null;
                        Object appLinkDataClass = AppLinkDataClass.cast(args[0]);
                        Method getArgumentBundleMethod = AppLinkDataClass.getMethod("getArgumentBundle");
                        Bundle appLinkDataBundle = Bundle.class.cast(getArgumentBundleMethod.invoke(appLinkDataClass));

                        if (appLinkDataBundle != null) {
                            appLinkUrl = appLinkDataBundle.getString(NATIVE_URL_KEY);
                        }

                        if (callback != null) {
                            callback.onAppLinkFetchFinished(appLinkUrl);
                        }

                    } else {
                        if (callback != null) {
                            callback.onAppLinkFetchFinished(null);
                        }
                    }
                    return null;
                }
            };

            Object completionListenerInterface = Proxy.newProxyInstance(AppLinkDataCompletionHandlerClass.getClassLoader()
                    , new Class<?>[]{AppLinkDataCompletionHandlerClass}
                    , ALDataCompletionHandler);

            String fbAppID = context.getString(context.getResources().getIdentifier("facebook_app_id", "string", context.getPackageName()));
            if (TextUtils.isEmpty(fbAppID)) {
                isRequestSucceeded = false;
            } else {
                fetchDeferredAppLinkDataMethod.invoke(null, context, fbAppID, completionListenerInterface);
            }

        } catch (Exception ex) {
            isRequestSucceeded = false;
        }
        return isRequestSucceeded;
    }

    public interface AppLinkFetchEvents {
        void onAppLinkFetchFinished(String nativeAppLinkUrl);
    }
}
