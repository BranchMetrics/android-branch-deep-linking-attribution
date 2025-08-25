package io.branch.referral.validators;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;

public class DeepLinkRoutingValidator {
    private static final String VALIDATE_SDK_LINK_PARAM_KEY = "bnc_validate";
    private static final String VALIDATE_LINK_PARAM_KEY = "validate";
    private static final String BRANCH_VALIDATE_TEST_KEY = "_branch_validate";
    private static final int BRANCH_VALIDATE_TEST_VALUE = 60514;
    private static final String URI_REDIRECT_KEY = "$uri_redirect_mode";
    private static final String URI_REDIRECT_MODE = "2";
    private static final int LAUNCH_TEST_TEMPLATE_DELAY = 500; // .5 sec delay to settle any auto deep linking
    private static WeakReference<Activity> current_activity_reference = null;
    
    // Static handler for lifecycle-aware delayed operations
    private static Handler staticHandler;
    
    // Task management for cancellation support
    private static final ConcurrentHashMap<Integer, Runnable> pendingTasks = new ConcurrentHashMap<>();
    private static final AtomicInteger taskIdCounter = new AtomicInteger(0);

    public static void validate(final WeakReference<Activity> activity) {
        current_activity_reference = activity;
        String latestReferringLink = getLatestReferringLink();
        if (!TextUtils.isEmpty(latestReferringLink) && activity != null) {
            final JSONObject response_data = Branch.getInstance().getLatestReferringParams();
            if (response_data.optInt(BRANCH_VALIDATE_TEST_KEY) == BRANCH_VALIDATE_TEST_VALUE) {
                if (response_data.optBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                    validateDeeplinkRouting(response_data);
                } else {
                    displayErrorMessage();
                }
            } else if (response_data.optBoolean(VALIDATE_SDK_LINK_PARAM_KEY)) {
                scheduleTask(new ValidatorRunnable(response_data), LAUNCH_TEST_TEMPLATE_DELAY);
            }
        }
    }

    /**
     * Schedules a task with proper lifecycle management and cancellation support
     * @param runnable The task to execute
     * @param delay Delay in milliseconds
     * @return Task ID for cancellation purposes
     */
    private static int scheduleTask(Runnable runnable, long delay) {
        int taskId = taskIdCounter.incrementAndGet();
        pendingTasks.put(taskId, runnable);
        
        Runnable wrappedRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Remove from pending tasks when executed
                    pendingTasks.remove(taskId);
                    runnable.run();
                } catch (Exception e) {
                    BranchLogger.e("Error executing scheduled validator task: " + e.getMessage());
                }
            }
        };
        
        getStaticHandler().postDelayed(wrappedRunnable, delay);
        return taskId;
    }

    /**
     * Cancels a scheduled task by ID
     * @param taskId The task ID to cancel
     */
    public static void cancelTask(int taskId) {
        Runnable task = pendingTasks.remove(taskId);
        if (task != null) {
            getStaticHandler().removeCallbacks(task);
        }
    }

    /**
     * Cancels all pending validator tasks - useful for cleanup
     */
    public static void cancelAllTasks() {
        pendingTasks.clear();
        if (staticHandler != null) {
            staticHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Lazy initialization of static handler to avoid issues in unit tests
     */
    private static Handler getStaticHandler() {
        if (staticHandler == null) {
            staticHandler = new Handler(Looper.getMainLooper());
        }
        return staticHandler;
    }

    /**
     * Lifecycle-aware Runnable that uses WeakReference to prevent memory leaks
     */
    private static class ValidatorRunnable implements Runnable {
        private final JSONObject responseData;

        ValidatorRunnable(JSONObject responseData) {
            this.responseData = responseData;
        }

        @Override
        public void run() {
            // Check if activity is still valid before proceeding
            if (current_activity_reference != null && current_activity_reference.get() != null) {
                launchTestTemplate(getUpdatedLinkWithTestStat(responseData, ""));
            } else {
                BranchLogger.d("Activity reference lost, skipping validator task execution");
            }
        }
    }

    private static void validateDeeplinkRouting(final JSONObject validate_json) {
        AlertDialog.Builder builder;
        if(current_activity_reference.get() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(current_activity_reference.get(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(current_activity_reference.get());
            }
            builder.setTitle("Branch Deeplinking Routing")
                    .setMessage("Good news - we got link data. Now a question for you, astute developer: did the app deep link to the specific piece of content you expected to see?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Test Succeeded
                            String launch_link = getUpdatedLinkWithTestStat(validate_json, "g");
                            launchTestTemplate(launch_link);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Test Failed
                            String launch_link = getUpdatedLinkWithTestStat(validate_json, "r");
                            launchTestTemplate(launch_link);
                        }
                    })
                    .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .setCancelable(false)
                    .setIcon(android.R.drawable.sym_def_app_icon)
                    .show();
        }
    }

    private static void launchTestTemplate(String url) {
        if(current_activity_reference.get() != null) {
            Uri launch_url = Uri.parse(url)
                    .buildUpon()
                    .appendQueryParameter(URI_REDIRECT_KEY,URI_REDIRECT_MODE)
                    .build();
            Intent i = new Intent(Intent.ACTION_VIEW, launch_url);
//        i.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setPackage("com.android.chrome");
            current_activity_reference.get().getPackageManager().queryIntentActivities(i, 0);
            try {

                current_activity_reference.get().startActivity(i);
            } catch (ActivityNotFoundException e) {
                // Chrome is probably not installed
                // Try with the default browser
                i.setPackage(null);
                current_activity_reference.get().startActivity(i);
            }
        }
    }

    private static String getUpdatedLinkWithTestStat(JSONObject blob, String result) {
        String link = "";
        try {
            link = blob.getString("~" + Defines.Jsonkey.ReferringLink.getKey());
            link = link.split("\\?")[0];
        } catch (Exception e) {
            BranchLogger.v("Failed to get referring link");
        }
        link += "?" + VALIDATE_LINK_PARAM_KEY + "=true";
        if (!TextUtils.isEmpty(result)) {
            try {
                link += blob.getString("ct").equals("t1") ? "&t1=" + result : "&t1=" + blob.getString("t1");
                link += blob.getString("ct").equals("t2") ? "&t2=" + result : "&t2=" + blob.getString("t2");
                link += blob.getString("ct").equals("t3") ? "&t3=" + result : "&t3=" + blob.getString("t3");
                link += blob.getString("ct").equals("t4") ? "&t4=" + result : "&t4=" + blob.getString("t4");
                link += blob.getString("ct").equals("t5") ? "&t5=" + result : "&t5=" + blob.getString("t5");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        link += "&os=android";
        return link;
    }

    private static void displayErrorMessage() {
        AlertDialog.Builder builder;
        if(current_activity_reference.get() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(current_activity_reference.get(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(current_activity_reference.get());
            }
            builder.setTitle("Branch Deeplink Routing Support")
                    .setMessage("Bummer. It seems like +clicked_branch_link is false - we didn't deep link.  Double check that the link you're clicking has the same branch_key that is being used in your Manifest file. Return to Chrome when you're ready to test again.")
                    .setNeutralButton("Got it", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    })
                    .setCancelable(false)
                    .setIcon(android.R.drawable.sym_def_app_icon)
                    .show();
        }
    }

    private static String getLatestReferringLink() {
        String latestReferringLink = "";
        if (Branch.getInstance() != null && Branch.getInstance().getLatestReferringParams() != null) {
            latestReferringLink = Branch.getInstance().getLatestReferringParams().optString("~" + Defines.Jsonkey.ReferringLink.getKey());
        }
        return latestReferringLink;
    }

}
