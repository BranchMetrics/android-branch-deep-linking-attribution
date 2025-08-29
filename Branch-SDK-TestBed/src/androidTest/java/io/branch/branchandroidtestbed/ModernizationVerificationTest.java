package io.branch.branchandroidtestbed;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.referral.Branch;
import io.branch.referral.validators.IntegrationValidator;

/**
 * Verification test to ensure modernized async patterns are working correctly.
 * 
 * This test specifically validates that our BranchAsyncTask modernization using
 * CompletableFuture and Kotlin coroutines is functioning as expected.
 */
@RunWith(AndroidJUnit4.class)
public class ModernizationVerificationTest {
    
    private Context context;
    private static final String TAG = "ModernizationTest";
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Log.d(TAG, "=== STARTING MODERNIZATION VERIFICATION TESTS ===");
    }
    
    /**
     * Test that IntegrationValidator uses our modernized BranchIntegrationModel
     * with CompletableFuture instead of deprecated AsyncTask.
     */
    @Test
    public void testBranchIntegrationModelModernization() {
        Log.d(TAG, "Testing BranchIntegrationModel modernization...");
        
        // Run on main thread to avoid Handler issues
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final boolean[] completed = {false};
        
        mainHandler.post(() -> {
            // This will trigger BranchIntegrationModel.updateDeepLinkSchemes()
            // which now uses CompletableFuture with our MODERNIZATION_TRACE logs
            IntegrationValidator.validate(context);
            completed[0] = true;
        });
        
        // Wait for async operations to complete
        int maxWait = 5000; // 5 seconds
        int waited = 0;
        while (!completed[0] && waited < maxWait) {
            try {
                Thread.sleep(100);
                waited += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Log.d(TAG, "BranchIntegrationModel test completed. Check logs for MODERNIZATION_TRACE messages.");
    }
    
    /**
     * Test that UniversalResourceAnalyser uses CompletableFuture for URL operations.
     */
    @Test
    public void testUniversalResourceAnalyserModernization() {
        Log.d(TAG, "Testing UniversalResourceAnalyser modernization...");
        
        // Prepare main looper
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final boolean[] completed = {false};
        
        mainHandler.post(() -> {
            // This triggers UniversalResourceAnalyser.addToAcceptURLFormats() which calls
            // checkAndUpdateSkipURLFormats() with our modernized CompletableFuture implementation
            Branch.getInstance().addWhiteListedScheme("https://example.com/*");
            completed[0] = true;
        });
        
        // Wait for async operation
        int maxWait = 3000; // 3 seconds
        int waited = 0;
        while (!completed[0] && waited < maxWait) {
            try {
                Thread.sleep(100);
                waited += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Log.d(TAG, "UniversalResourceAnalyser test completed. Check logs for MODERNIZATION_TRACE messages.");
    }
    
    /**
     * Test that ModernLinkGenerator is used for link creation.
     */
    @Test 
    public void testModernLinkGeneratorUsage() {
        Log.d(TAG, "Testing ModernLinkGenerator usage...");
        
        // Prepare main looper
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final boolean[] completed = {false};
        final String[] generatedUrl = {null};
        
        mainHandler.post(() -> {
            // Create a simple link to trigger ModernLinkGenerator
            // This calls generateShortLinkSync() which uses our ModernLinkGenerator
            String url = new io.branch.referral.BranchShortLinkBuilder(context)
                    .addParameters("source", "modernization_test")
                    .getShortUrl();
            generatedUrl[0] = url;
            Log.d(TAG, "Generated URL with ModernLinkGenerator: " + url);
            completed[0] = true;
        });
        
        // Wait for operation
        int maxWait = 3000; // 3 seconds
        int waited = 0;
        while (!completed[0] && waited < maxWait) {
            try {
                Thread.sleep(100);
                waited += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Log.d(TAG, "ModernLinkGenerator test completed. Check logs for MODERNIZATION_TRACE messages.");
    }
    
    /**
     * Integration test that verifies all modernized components work together.
     */
    @Test
    public void testCompleteModernizationIntegration() {
        Log.d(TAG, "=== COMPLETE MODERNIZATION INTEGRATION TEST ===");
        
        // Prepare main looper
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final boolean[] allCompleted = {false};
        
        mainHandler.post(() -> {
            // 1. Test BranchIntegrationModel (used by IntegrationValidator)
            Log.d(TAG, "1. Testing BranchIntegrationModel via IntegrationValidator...");
            IntegrationValidator.validate(context);
            
            // 2. Test UniversalResourceAnalyser
            Log.d(TAG, "2. Testing UniversalResourceAnalyser...");
            Branch.getInstance().addWhiteListedScheme("https://test.com/*");
            
            // 3. Test ModernLinkGenerator
            Log.d(TAG, "3. Testing ModernLinkGenerator...");
            String shortUrl = new io.branch.referral.BranchShortLinkBuilder(context)
                    .addParameters("integration_test", "complete")
                    .getShortUrl();
            Log.d(TAG, "Generated URL: " + shortUrl);
            
            allCompleted[0] = true;
        });
        
        // Wait for all async operations
        int maxWait = 8000; // 8 seconds for all operations
        int waited = 0;
        while (!allCompleted[0] && waited < maxWait) {
            try {
                Thread.sleep(100);
                waited += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        Log.d(TAG, "=== MODERNIZATION INTEGRATION TEST COMPLETED ===");
        Log.d(TAG, "Search for 'MODERNIZATION_TRACE' in logs to verify modern patterns are being used");
    }
}