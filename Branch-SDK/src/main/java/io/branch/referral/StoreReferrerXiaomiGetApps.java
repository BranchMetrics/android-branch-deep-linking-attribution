package io.branch.referral;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class StoreReferrerXiaomiGetApps {

    public static void fetch(final Context context){
        try {
            final Class<?> getAppsReferrerClientClass = Class.forName("com.miui.referrer.api.GetAppsReferrerClient");

            Class<?> builderClass = Class.forName("com.miui.referrer.api.GetAppsReferrerClient$Builder");
            Constructor<?> builderConstructor = builderClass.getConstructor(Context.class);
            Method buildMethod = builderClass.getMethod("build");
            Object builderObject = builderConstructor.newInstance(context);

            final Object getAppsReferrerClientObject = buildMethod.invoke(builderObject);

            Class<?> getAppsReferrerStateListenerClass = Class.forName("com.miui.referrer.api.GetAppsReferrerStateListener");
            Method startConnectionMethod = getAppsReferrerClientClass.getMethod("startConnection", getAppsReferrerStateListenerClass);

            final Proxy proxy = (Proxy) Proxy.newProxyInstance(getAppsReferrerStateListenerClass.getClassLoader(), new Class[]{getAppsReferrerStateListenerClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                    if(method.getName().equals("onGetAppsReferrerSetupFinished")){
                        Integer responseCode = (Integer) args[0];
                        PrefHelper.Debug("Xiaomi GetApps onGetAppsReferrerSetupFinished, responseCode = " + responseCode);

                        Class<?> getAppsReferrerResponseClass = Class.forName("com.miui.referrer.annotation.GetAppsReferrerResponse$Companion");
                        int OK = getAppsReferrerResponseClass.getField("OK").getInt(null);

                        if(responseCode == OK) {
                            // com.miui.referrer.api.GetAppsReferrerClient getInstallReferrer() which is the object with all referrer data
                            Method getInstallReferrerMethod = getAppsReferrerClientClass.getMethod("getInstallReferrer");
                            Class<?> getAppsReferrerDetailsClass = Class.forName("com.miui.referrer.api.GetAppsReferrerDetails");

                            // com.miui.referrer.api.GetAppsReferrerDetails getInstallReferrer() which is the referrer string
                            Method getInstallReferrerStringMethod = getAppsReferrerDetailsClass.getMethod("getInstallReferrer");
                            Method getReferrerClickTimestampSecondsMethod = getAppsReferrerDetailsClass.getMethod("getReferrerClickTimestampSeconds");
                            Method getInstallBeginTimestampSecondsMethod = getAppsReferrerDetailsClass.getMethod("getInstallBeginTimestampSeconds");

                            Object installReferrerObject = getInstallReferrerMethod.invoke(getAppsReferrerClientObject);

                            String rawReferrerString = (String) getInstallReferrerStringMethod.invoke(installReferrerObject);
                            Long clickTimestamp = (Long) getReferrerClickTimestampSecondsMethod.invoke(installReferrerObject);
                            Long installBeginTimestamp = (Long) getInstallBeginTimestampSecondsMethod.invoke(installReferrerObject);

                            if(clickTimestamp == null){
                                clickTimestamp = 0L;
                            }

                            if(installBeginTimestamp == null){
                                installBeginTimestamp = 0L;
                            }

                            Method endConnectionMethod = getAppsReferrerClientClass.getMethod("endConnection");
                            endConnectionMethod.invoke(getAppsReferrerClientObject);

                            StoreAttribution.onReferrerClientFinished(context, rawReferrerString, clickTimestamp, installBeginTimestamp, getAppsReferrerClientClass.getName());
                        }
                        // To improve performance, we are not going to reflect out every field when our handling is the same for all other cases
                        else{
                            StoreAttribution.onReferrerClientError();
                        }
                    }
                    else if(method.getName().equals("onGetAppsServiceDisconnected")){
                        PrefHelper.Debug("Xiaomi GetApps onGetAppsServiceDisconnected");
                    }

                    return null;
                }
            });

            startConnectionMethod.invoke(getAppsReferrerClientObject, proxy);
        }
        catch (Exception e) {
            PrefHelper.Debug(e.getMessage());
            e.printStackTrace();
        }
    }
}
