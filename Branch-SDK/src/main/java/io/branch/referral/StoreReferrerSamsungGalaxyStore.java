package io.branch.referral;

import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class StoreReferrerSamsungGalaxyStore {
    public static void fetch(final Context context) {
        try{
            final Class<?> installReferrerClientClass = Class.forName("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient");

            Method newBuilder = installReferrerClientClass.getMethod("newBuilder", Context.class);
            Object builderObject = newBuilder.invoke(installReferrerClientClass, context);
            Class<?> builderClass = Class.forName("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient$Builder");
            Method buildMethod = builderClass.getMethod("build");

            final Object installReferrerClientObject = buildMethod.invoke(builderObject);

            Class<?> installReferrerStateListenerClass = Class.forName("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerStateListener");
            final Method startConnectionMethod = installReferrerClientClass.getMethod("startConnection", installReferrerStateListenerClass);

            // Unlike the other implementations, this callback returns on a Binder thread instead of main.
            // Operation still complete normally because of the install referrer fetch lock on the request
            // Assuming it's within time
            final Proxy proxy = (Proxy) Proxy.newProxyInstance(installReferrerStateListenerClass.getClassLoader(), new Class[]{installReferrerStateListenerClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, final Method method, final Object[] args) throws Exception {
                    if (method.getName().equals("onInstallReferrerSetupFinished")) {
                        Integer responseCode = (Integer) args[0];
                        PrefHelper.Debug("Samsung Galaxy Store onInstallReferrerSetupFinished, responseCode = " + responseCode);

                        Class<?> installReferrerResponseClass = Class.forName("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient$InstallReferrerResponse");
                        int OK = installReferrerResponseClass.getField("OK").getInt(null);

                        if (responseCode == OK) {
                            //com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient getInstallReferrer() which returns the object with all the referrer data
                            Method getInstallReferrerMethod = installReferrerClientClass.getMethod("getInstallReferrer");
                            Class<?> referrerDetailsClass = Class.forName("com.sec.android.app.samsungapps.installreferrer.api.ReferrerDetails");

                            // com.sec.android.app.samsungapps.installreferrer.api.ReferrerDetails getInstallReferrer() which is the referrer string
                            Method getInstallReferrerStringMethod = referrerDetailsClass.getMethod("getInstallReferrer");
                            Method getReferrerClickTimestampSecondsMethod = referrerDetailsClass.getMethod("getReferrerClickTimestampSeconds");
                            Method getInstallBeginTimestampSecondsMethod = referrerDetailsClass.getMethod("getInstallBeginTimestampSeconds");

                            Object installReferrerObject = getInstallReferrerMethod.invoke(installReferrerClientObject);

                            String rawReferrerString = (String) getInstallReferrerStringMethod.invoke(installReferrerObject);
                            Long clickTimestamp = (Long) getReferrerClickTimestampSecondsMethod.invoke(installReferrerObject);
                            Long installBeginTimestamp = (Long) getInstallBeginTimestampSecondsMethod.invoke(installReferrerObject);

                            if (clickTimestamp == null) {
                                clickTimestamp = 0L;
                            }

                            if (installBeginTimestamp == null) {
                                installBeginTimestamp = 0L;
                            }

                            Method endConnectionMethod = installReferrerClientClass.getMethod("endConnection");
                            endConnectionMethod.invoke(installReferrerClientObject);

                            StoreReferrer.onReferrerClientFinished(context, rawReferrerString, clickTimestamp, installBeginTimestamp, installReferrerClientClass.getName());
                        }
                        // To improve performance, we are not going to reflect out every field when our handling is the same for all other cases
                        else {
                            StoreReferrer.onReferrerClientError();
                        }
                    }
                    else if (method.getName().equals("onInstallReferrerServiceDisconnected")) {
                        PrefHelper.Debug("onInstallReferrerServiceDisconnected");
                    }

                    return null;
                }
            });

            startConnectionMethod.invoke(installReferrerClientObject, proxy);
        }
        catch (Exception e) {
            PrefHelper.Debug(e.getMessage());
            e.printStackTrace();
        }
    }
}
