package io.branch.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;

/**
 * Created by sojanpr on 9/26/16.
 */
public class BranchSearchServiceConnection implements ServiceConnection {
    private static BranchSearchServiceConnection connection_;
    IBranchSearchServiceInterface branchSearchServiceInterface_;
    private Branch.IBranchSearchEvents searchEvents_;
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

    public void addToSharableContent(BranchUniversalObject contentBUO, String packageName, String contentUrl) {
        Log.d("Bridge_test", "addToSharableContent");
        if (branchSearchServiceInterface_ != null) {
            try {
                branchSearchServiceInterface_.addToSharableContent(contentBUO, packageName, contentUrl);
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

    private final IBranchSearchCallback.Stub searchCallback = new IBranchSearchCallback.Stub() {
        @Override
        public void onSearchResult(int offset, int limit, String searchKey, List<BranchSearchContent> searchResult) throws RemoteException {
            if (searchEvents_ != null) {
                searchEvents_.onSearchResult(offset, limit, searchKey, searchResult);
            }
        }
    };


}
