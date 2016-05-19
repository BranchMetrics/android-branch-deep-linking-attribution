package io.branch.referral;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                        String appLinkDataStr = String.class.cast(getArgumentBundleMethod.invoke(appLinkDataClass));

                        if (appLinkDataStr != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(appLinkDataStr);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject innerObj = jsonArray.getJSONObject(i);
                                    if (innerObj.has(NATIVE_URL_KEY)) {
                                        appLinkUrl = innerObj.getString(NATIVE_URL_KEY);
                                        break;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
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

            fetchDeferredAppLinkDataMethod.invoke(null, context, context.getApplicationContext().getPackageName(), completionListenerInterface);

        } catch (Exception ex) {
            isRequestSucceeded = false;
        }
        return isRequestSucceeded;
    }

    public interface AppLinkFetchEvents {
        void onAppLinkFetchFinished(String nativeAppLinkUrl);
    }
}
