package io.branch.referral;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import io.branch.referral.util.BranchEvent;

/**
 * BranchEvent class tests.
 */
@RunWith(AndroidJUnit4.class)
abstract class BranchTestRequestUtil {
    private static final String TAG = "BranchEventTestUtil";

    // can be pretty short because we mock remote interface and don't actually make async calls from the SDK
    public static final int TEST_REQUEST_TIMEOUT = 3000;
    public static final int TEST_INIT_SESSION_TIMEOUT = 15000;

    // Dig out the variable for isStandardEvent from the BranchEvent object.
    protected boolean isStandardEvent(BranchEvent event) throws Exception {
        // Use Reflection to find if it is considered a "Standard Event"
        Field f = event.getClass().getDeclaredField("isStandardEvent"); //NoSuchFieldException
        f.setAccessible(true);
        return (boolean) f.get(event); //IllegalAccessException
    }

    // Obtain the ServerRequest that is on the queue that matches the BranchEvent to be logged.
    protected ServerRequest logEvent(Context context, BranchEvent event) throws Exception {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);
        int queueSizeIn = queue.getSize();

        event.logEvent(context);
        ServerRequest queuedEvent = findRequestOnQueue(context, "name", event.getEventName());
        Assert.assertNotNull(queuedEvent);

        int queueSizeOut = queue.getSize();
        Assert.assertEquals(queueSizeOut, (queueSizeIn + 1));

        return doFinalUpdate(queuedEvent);
    }

    protected ServerRequest findRequestOnQueue(Context context, String key, String eventName) throws InterruptedException {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        int wait_remaining = 2000;
        final int interval = 50;

        while (wait_remaining > 0) {
            Thread.sleep(interval);
            if (queue.getSize() > 0) {
                int index = queue.getSize() - 1;

                ServerRequest request = queue.peekAt(index);
                JSONObject jsonObject = request.getGetParams();

                String name = jsonObject.optString(key);
                if (name.equals(eventName)) {
                    // Found it.
                    return request;
                }
            }
            wait_remaining -= interval;
        }

        return null;
    }

    protected ServerRequest getLastRequestOnQueue(Context context, int minimumQueueSize) {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        int size = queue.getSize();
        if (size >= minimumQueueSize) {
            return queue.peekAt(size - 1);
        }

        return null;
    }

    protected ServerRequest doFinalUpdate(ServerRequest request) {
        Log.i("BranchSDK", "doFinalUpdate");
        request.doFinalUpdateOnBackgroundThread();
        return request;
    }

    protected ServerRequest doFinalUpdateOnMainThread(ServerRequest request) {
        request.doFinalUpdateOnMainThread();
        return request;
    }
}

