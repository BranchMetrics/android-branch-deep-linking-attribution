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

/**
 * Created by sojanpr on 9/26/16.
 */
public class BranchSearchServiceConnection implements ServiceConnection {
    private static BranchSearchServiceConnection connection_;
    IBridgeInterface bridgeInterface_;

    public static BranchSearchServiceConnection getInstance() {
        if (connection_ == null) {
            connection_ = new BranchSearchServiceConnection();
        }
        return connection_;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        bridgeInterface_ = IBridgeInterface.Stub.asInterface(service);
        Log.d("Bridge_test", "onServiceConnected()");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d("Bridge_test", "onServiceDisconnected()");
        bridgeInterface_ = null;
    }

    public void doBindService(Context context) {
        Log.d("Bridge_test", "doBindService");
        Intent intent = new Intent("BranchSearchServiceConn");
        intent.setPackage("io.branch.searchservice");
        Boolean serviceBound = context.bindService(intent, connection_, Context.BIND_AUTO_CREATE);
        Log.d("Bridge_test", "Service Bound :- " + serviceBound);
    }

    public void addToSharableContent(BranchUniversalObject contentBUO, String packageName, String contentUrl) {
        Log.d("Bridge_test", "addToSharableContent");
        if (bridgeInterface_ != null) {
            try {
                bridgeInterface_.addToSharableContent(contentBUO, packageName, contentUrl);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public List<BranchSearchContent> getContentForKey(String key) {
        List<BranchSearchContent> searchResult = new ArrayList<>();
        if (bridgeInterface_ != null) {
            try {
                searchResult = bridgeInterface_.searchContent(key);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return searchResult;
    }


}
