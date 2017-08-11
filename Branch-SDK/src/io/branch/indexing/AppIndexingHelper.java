package io.branch.indexing;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.branch.referral.PrefHelper;
import io.branch.referral.util.LinkProperties;

/**
 * Created by sojanpr on 5/18/16.
 * <p>
 * Helper class for publishing BUO with google app indexing
 * </p>
 */
class AppIndexingHelper {
    private static FirebaseUserActions fireBaseUserActionsInstance = null;
    

    static void addToAppIndex(final Context context, final BranchUniversalObject buo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //noinspection all
                try {
                    fireBaseUserActionsInstance = FirebaseUserActions.getInstance();
                } catch (NoClassDefFoundError ignore) {
                    // Expected when Firebase app indexing dependency is not available
                } catch (Throwable ignore) {
                    // unexpected exception
                }
                String contentUrl = buo.getShortUrl(context, new LinkProperties().setChannel("google_search"));
                if (!TextUtils.isEmpty(contentUrl)) {
                    try {
                        if (fireBaseUserActionsInstance != null) {
                            addToAppIndexUsingFireBase(contentUrl, buo);
                        } else {
                            //noinspection deprecation
                            listOnGoogleSearch(contentUrl, context, buo);
                        }
                    } catch (Throwable e) {
                        PrefHelper.Debug("BranchSDK", "Branch Warning: Unable to list your content in google search. Please make sure you have added latest firebase app indexing SDK to your project dependencies.");
                    }
                }
            }
        }).run();
    }
    
    private static void addToAppIndexUsingFireBase(String contentUrl, BranchUniversalObject buo) {
        Indexable noteToIndex = Indexables.newSimple(buo.getTitle(), contentUrl);
        Task<Void> task = FirebaseAppIndex.getInstance().update(noteToIndex);
        
        Action action = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(buo.getTitle(), contentUrl)
                .setMetadata(new Action.Metadata.Builder()
                        .setUpload(buo.isPublicallyIndexable()))
                .build();
        
        fireBaseUserActionsInstance.end(action);
    }
    
    /**
     * Method to push content to Google app indexing.
     * This methods has deprecated with firebase-appindexing. This is kept to maintain backward compatibility
     * TODO this method should be removed after keeping for a while
     *
     * @deprecated gms.appindexing is deprecated with firebase.appindexing
     */
    private static void listOnGoogleSearch(String shortLink, Context context, BranchUniversalObject branchUniversalObject) throws Exception {
        // Create a Thing instance for the BUO with the link to the BUO
        Class<?> ThingClass = Class.forName("com.google.android.gms.appindexing.Thing");
        Class<?> ThingBuilderClass = Class.forName("com.google.android.gms.appindexing.Thing$Builder");
        Constructor<?> constructor = ThingBuilderClass.getConstructor();
        Object thingBuilder = constructor.newInstance();
        
        Method setNameMethod = ThingBuilderClass.getMethod("setName", String.class);
        Method setDescMethod = ThingBuilderClass.getMethod("setDescription", String.class);
        Method setUrlMethod = ThingBuilderClass.getMethod("setUrl", Uri.class);
        Method thingBuildMethod = ThingBuilderClass.getMethod("build");
        
        setNameMethod.invoke(thingBuilder, branchUniversalObject.getTitle());
        setDescMethod.invoke(thingBuilder, branchUniversalObject.getDescription());
        setUrlMethod.invoke(thingBuilder, Uri.parse(shortLink));
        Object thingObj = thingBuildMethod.invoke(thingBuilder);
        
        // Now Create an View Action for the Thing created
        Class<?> ThingActionClass = Class.forName("com.google.android.gms.appindexing.Action");
        Class<?> ThingActionBuilderClass = Class.forName("com.google.android.gms.appindexing.Action$Builder");
        Constructor<?> thingActionBuilderConstructor = ThingActionBuilderClass.getConstructor(String.class);
        Object actionBuilder = thingActionBuilderConstructor.newInstance((String) ThingActionClass.getDeclaredField("TYPE_VIEW").get(null));
        
        Method setObjectMethod = ThingActionBuilderClass.getMethod("setObject", ThingClass);
        Method setActionStatusMethod = ThingActionBuilderClass.getMethod("setActionStatus", String.class);
        Method actionBuildMethod = ThingActionBuilderClass.getMethod("build");
        
        setObjectMethod.invoke(actionBuilder, thingObj);
        setActionStatusMethod.invoke(actionBuilder, (String) ThingActionClass.getDeclaredField("STATUS_TYPE_COMPLETED").get(null));
        Object actionObj = actionBuildMethod.invoke(actionBuilder);
        
        // Create and connect with Api Client
        Class<?> AppIndexClass = Class.forName("com.google.android.gms.appindexing.AppIndex");
        Class<?> ApiClass = Class.forName("com.google.android.gms.common.api.Api");
        Class<?> GoogleApiClientClass = Class.forName("com.google.android.gms.common.api.GoogleApiClient");
        Class<?> GoogleApiClientBuilderClass = Class.forName("com.google.android.gms.common.api.GoogleApiClient$Builder");
        Constructor<?> googleApiClientBuilderConstructor = GoogleApiClientBuilderClass.getConstructor(Context.class);
        Object apiClientBuilder = googleApiClientBuilderConstructor.newInstance(context);
        
        Method addApiMethod = GoogleApiClientBuilderClass.getMethod("addApi", ApiClass);
        Method apiClientBuildMethod = GoogleApiClientBuilderClass.getMethod("build");
        Method apiClientConnectMethod = GoogleApiClientClass.getMethod("connect");
        Method apiClientDisConnectMethod = GoogleApiClientClass.getMethod("disconnect");
        
        
        addApiMethod.invoke(apiClientBuilder, ApiClass.cast(AppIndexClass.getDeclaredField("API").get(null)));
        Object googleApiClientApiClientObj = apiClientBuildMethod.invoke(apiClientBuilder);
        
        // Connect API Client, add to app index and then disconnect
        apiClientConnectMethod.invoke(googleApiClientApiClientObj);
        
        
        Class<?> AppIndexApiClass = Class.forName("com.google.android.gms.appindexing.AppIndexApi");
        Object appIndexApiObj = AppIndexClass.getDeclaredField("AppIndexApi").get(null);
        Method startMethod = AppIndexApiClass.getMethod("start", GoogleApiClientClass, ThingActionClass);
        startMethod.invoke(appIndexApiObj, googleApiClientApiClientObj, actionObj);
        
        apiClientDisConnectMethod.invoke(googleApiClientApiClientObj);
    }
    
}
