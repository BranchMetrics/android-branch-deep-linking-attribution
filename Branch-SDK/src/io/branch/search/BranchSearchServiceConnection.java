package io.branch.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;

/**
 * Created by sojanpr on 9/26/16.
 * <p>
 * Service connection class for BranchSearchService.
 * Implements  {@link IBranchSearchServiceInterface} methods implemented by BranchSearchService.
 * </p>
 */
public class BranchSearchServiceConnection implements ServiceConnection {
    private static BranchSearchServiceConnection connection_;
    IBranchSearchServiceInterface branchSearchServiceInterface_;
    private Branch.IBranchSearchEvents searchEvents_;
    private Branch.IBranchAppRecommendationEvents appRecommendationEvent_;
    private Branch.IBranchContentRecommendationEvents contentRecommendationEvents_;
    String packageName_;

    public static BranchSearchServiceConnection getInstance() {
        if (connection_ == null) {
            connection_ = new BranchSearchServiceConnection();
        }
        return connection_;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d("Bridge_test", "onServiceConnected()");
        branchSearchServiceInterface_ = IBranchSearchServiceInterface.Stub.asInterface(service);
        try {
            branchSearchServiceInterface_.registerCallback(searchCallback, packageName_);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d("Bridge_test", "onServiceDisconnected()");
        branchSearchServiceInterface_ = null;

    }

    public void doBindService(Context context) {
        Log.d("Bridge_test", "doBindService");
        packageName_ = context.getPackageName();
        Intent intent = new Intent("BranchSearchServiceConn");
        intent.setPackage("io.branch.searchservice");
        Boolean serviceBound = context.bindService(intent, connection_, Context.BIND_AUTO_CREATE);
        Log.d("Bridge_test", "Service Bound :- " + serviceBound);
    }

    public boolean addToSharableContent(BranchUniversalObject contentBUO, String packageName, String contentUrl) {
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

    public boolean deleteContent(BranchUniversalObject buo, String packageName) {
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

    public boolean clearAllContents(String packageName){
        Log.d("Bridge_test", "deleteContent");
        boolean clearedAllContents = false;
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.deleteAllSearchableContent(packageName);
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

    public boolean getContentForKey(String key, int offset, int limit, Branch.IBranchSearchEvents callback) {
        boolean isServiceConnected = false;
        searchEvents_ = callback;
        if (branchSearchServiceInterface_ != null) {
            isServiceConnected = true;
            try {
                branchSearchServiceInterface_.searchContent(key, offset, limit, packageName_);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isServiceConnected;
    }

    public boolean getAppRecommendations(int maxAppCount, boolean skipSystemApps, Branch.IBranchAppRecommendationEvents callback) {
        boolean isServiceConnected = false;
        appRecommendationEvent_ = callback;
        if (branchSearchServiceInterface_ != null) {
            isServiceConnected = true;
            try {
                branchSearchServiceInterface_.getTopRecommendedApps(maxAppCount, skipSystemApps, packageName_);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isServiceConnected;
    }

    public boolean getContentRecommendations(int maxContentCount, Branch.IBranchContentRecommendationEvents callback) {
        boolean isServiceConnected = false;
        contentRecommendationEvents_ = callback;
        if (branchSearchServiceInterface_ != null) {
            isServiceConnected = true;
            try {
                branchSearchServiceInterface_.getTopRecommendedContents(maxContentCount, packageName_);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isServiceConnected;
    }

    public boolean searchAppForContents(String packageName, String appKeyword) {
        boolean isServiceConnected = false;
        if (branchSearchServiceInterface_ != null) {
            isServiceConnected = true;
            try {
                branchSearchServiceInterface_.searchInApp(packageName, appKeyword);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isServiceConnected;
    }

    private final IBranchSearchCallback.Stub searchCallback = new IBranchSearchCallback.Stub() {
        @Override
        public void onSearchResult(int offset, int limit, String searchKey, List<BranchSearchContent> searchResult) throws RemoteException {
            if (searchEvents_ != null) {
                searchEvents_.onSearchResult(offset, limit, searchKey, searchResult);
            }
        }

        @Override
        public void onRecommendedAppList(List<String> packageNames) throws RemoteException {
            if (appRecommendationEvent_ != null) {
                appRecommendationEvent_.onAppRecommendation(packageNames);
            }
        }

        @Override
        public void onRecommendedContent(List<BranchSearchContent> recommendedContents) throws RemoteException {
            if (contentRecommendationEvents_ != null) {
                contentRecommendationEvents_.onContentRecommendation(recommendedContents);
            }
        }
    };


}
