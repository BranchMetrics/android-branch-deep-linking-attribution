package io.branch.referral;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * <p>
 * Convenient class for handling ASync task with pool executor depending on the SDK platform
 * </p>
 */
public abstract class BranchAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    /**
     * Execute Params in back ground depending on the platform version. This executes task in parallel with the {@link AsyncTask#THREAD_POOL_EXECUTOR}
     *
     * @param params Params for executing this Async task
     * @return This object for method chaining
     */
    public final AsyncTask<Params, Progress, Result> executeTask(Params... params) {
        try {
            return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } catch (Exception t) {
            return execute(params);
        }
    }

    @Override
    public void onCancelled(){
        Log.i("BranchSDK", "async task onCancelled");
        super.onCancelled();
    }

    @Override
    public void onCancelled(Result result){
        Log.i("BranchSDK", "async task onCancelled with result " + result);
        super.onCancelled(result);
    }
}
