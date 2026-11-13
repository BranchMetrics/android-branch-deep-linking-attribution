package io.branch.referral.test.mock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for the Samsung MAPS app tracking provider. Static fields set its answer and how long it blocks.
 */
public class MockSamsungMapsProvider extends ContentProvider {
    private static final String[] COLUMNS = {
            "RESULT", "REASON", "APP_TRACKING_ID", "MAPS_ID", "INSTALLED_TIME_TEXT",
            "ACTIVATED_TIME_TEXT", "COUNTRY", "DEVICE_NAME", "FIRST_QUERY"
    };

    public static volatile String result = "true";
    public static volatile CountDownLatch block = null;
    public static volatile String lastSelection = null;
    public static volatile String[] lastSelectionArgs = null;
    public static final AtomicInteger queryCount = new AtomicInteger(0);

    public static void reset() {
        result = "true";
        release();
        block = null;
        lastSelection = null;
        lastSelectionArgs = null;
        queryCount.set(0);
    }

    public static void release() {
        CountDownLatch latch = block;
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        queryCount.incrementAndGet();
        lastSelection = selection;
        lastSelectionArgs = selectionArgs;

        CountDownLatch latch = block;
        if (latch != null) {
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        String appTrackingId = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : null;
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        cursor.addRow(new Object[]{
                result, "test", appTrackingId, "12345", "26:10:01:10:00",
                "26:10:01:10:05", "USA", "SM-S928U", "true"
        });
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
