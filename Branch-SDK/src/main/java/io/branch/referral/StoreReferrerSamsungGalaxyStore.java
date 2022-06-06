package io.branch.referral;

import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Timer;
import java.util.TimerTask;

public class StoreReferrerSamsungGalaxyStore extends AppStoreReferrer{
    private static ISamsungInstallReferrerEvents callback_ = null;
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    public static void fetch(final Context context, ISamsungInstallReferrerEvents iSamsungInstallReferrerEvents) {
        hasBeenUsed = true;
        callback_ = iSamsungInstallReferrerEvents;

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

                            onReferrerClientFinished(context, rawReferrerString, clickTimestamp, installBeginTimestamp, installReferrerClientClass.getName());
                        }
                        // To improve performance, we are not going to reflect out every field when our handling is the same for all other cases
                        else {
                            onReferrerClientError();
                        }
                    }
                    else if (method.getName().equals("onInstallReferrerServiceDisconnected")) {
                        PrefHelper.Debug("onInstallReferrerServiceDisconnected");
                    }

                    return null;
                }
            });

            startConnectionMethod.invoke(installReferrerClientObject, proxy);

            new Timer().schedule(new TimerTask() {
                @Override public void run() {
                    PrefHelper.Debug("Samsung Store Referrer fetch lock released by timer");
                    reportInstallReferrer();
                }
            }, 1500);
        }
        catch (Exception e) {
            PrefHelper.Debug(e.getMessage());
            e.printStackTrace();
            onReferrerClientError();
        }
    }

    private static void onReferrerClientError() {
        erroredOut = true;
        reportInstallReferrer();
    }

    interface ISamsungInstallReferrerEvents {
        void onSamsungInstallReferrerEventsFinished();
    }


    public static void reportInstallReferrer() {
        if (callback_ != null) {
            callback_.onSamsungInstallReferrerEventsFinished();
            callback_ = null;
        }
    }

    protected static void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS, String clientName) {
        PrefHelper.Debug(clientName + " onReferrerClientFinished()");
        processReferrerInfo(context, rawReferrerString, clickTS, InstallBeginTS);
        reportInstallReferrer();
    }
}
