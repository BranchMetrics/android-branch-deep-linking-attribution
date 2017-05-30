package io.branch.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import io.branch.indexing.BranchUniversalObject;

/**
 * Created by sojanpr on 9/26/16.
 * <p>
 * Service connection class for BranchSearchService.
 * Implements  {@link IBranchListContentInterface} methods implemented by BranchSearchService.
 * </p>
 */
public class BranchListContentConnection implements ServiceConnection {
    private static BranchListContentConnection connection_;
    IBranchListContentInterface branchSearchServiceInterface_;
    String packageName_;

    public static BranchListContentConnection getInstance() {
        if (connection_ == null) {
            connection_ = new BranchListContentConnection();
        }
        return connection_;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d("Bridge_test", "onServiceConnected()");
        branchSearchServiceInterface_ = IBranchListContentInterface.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d("Bridge_test", "onServiceDisconnected()");
        branchSearchServiceInterface_ = null;

    }

    public void doBindService(Context context) {
        Log.d("Bridge_test", "doBindService");
        packageName_ = context.getPackageName();
        Intent intent = new Intent("BranchListContent");
        intent.setPackage("io.branch.searchservice");
        Boolean serviceBound = context.bindService(intent, connection_, Context.BIND_AUTO_CREATE);
        Log.d("Bridge_test", "Service Bound :- " + serviceBound);
    }

    public boolean addToIndex(BranchUniversalObject contentBUO, String packageName, String contentUrl) {
        Log.d("Bridge_test", "addToSharableContent");
        boolean isContentAdded = false;
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.addToIndex(contentBUO, packageName, contentUrl);
                isContentAdded = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isContentAdded;
    }

    public boolean deleteFromIndex(BranchUniversalObject buo, String packageName) {
        Log.d("Bridge_test", "deleteContent");
        boolean isContentDeleted = false;
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.deleteFromIndex(buo, packageName);
                isContentDeleted = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isContentDeleted;
    }

    public boolean deleteAllFromIndex(String packageName) {
        Log.d("Bridge_test", "deleteContent");
        boolean clearedAllContents = false;
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.deleteAllFromIndex(packageName);
                clearedAllContents = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return clearedAllContents;
    }


    /**
     * Add the user interaction event to the on-device index that will then later be used to influence
     * search ranking
     *
     * @param contentBUO  The content BUO to be added to the index
     * @param packageName The application that this content belongs to
     * @param userAction  The user interaction event
     * @param contentUrl  The deeplink url to open the piece of content.
     */
    public void addUserInteraction(BranchUniversalObject contentBUO, String packageName, String userAction, String contentUrl) {
        Log.d("Bridge_test", "addUserInteraction");
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.addUserInteraction(contentBUO, packageName, userAction, contentUrl);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
