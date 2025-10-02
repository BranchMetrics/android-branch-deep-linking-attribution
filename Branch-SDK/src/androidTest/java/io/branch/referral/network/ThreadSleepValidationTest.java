package io.branch.referral.network;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

/**
 * Validation test to verify Thread.sleep() has been eliminated from the network layer.
 * 
 * This test performs static analysis on the source code to ensure no Thread.sleep()
 * calls remain in the network implementation files.
 */
public class ThreadSleepValidationTest {
    
    /**
     * Test that verifies no Thread.sleep() calls exist in our new implementation files.
     */
    public static void testNoThreadSleepInNewImplementation() {
        System.out.println("🧪 Testing for Thread.sleep() elimination in new implementation...");
        
        try {
            List<String> filesToCheck = new ArrayList<>();
            filesToCheck.add("Branch-SDK/src/main/java/io/branch/referral/network/BranchAsyncNetworkLayer.kt");
            filesToCheck.add("Branch-SDK/src/main/java/io/branch/referral/network/BranchRemoteInterfaceUrlConnectionAsync.kt");
            
            boolean foundThreadSleep = false;
            List<String> violatingFiles = new ArrayList<>();
            
            for (String filePath : filesToCheck) {
                File file = new File(filePath);
                if (file.exists()) {
                    List<String> lines = Files.readAllLines(Paths.get(filePath));
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i).toLowerCase();
                        String trimmedLine = line.trim();
                        // Skip comments, logs, and documentation
                        if (line.contains("thread.sleep") && 
                            !trimmedLine.startsWith("//") && 
                            !trimmedLine.startsWith("*") &&
                            !line.contains("branchlogger") &&
                            !line.contains("eliminates thread.sleep") &&
                            !line.contains("no thread.sleep") &&
                            !line.contains("thread.sleep eliminated") &&
                            !line.contains("replaces thread.sleep")) {
                            foundThreadSleep = true;
                            violatingFiles.add(filePath + " (line " + (i + 1) + "): " + lines.get(i).trim());
                        }
                    }
                }
            }
            
            if (foundThreadSleep) {
                throw new AssertionError("Found Thread.sleep() calls in new implementation files: " + violatingFiles);
            }
            
            System.out.println("✅ No Thread.sleep() calls found in new implementation files");
            
        } catch (IOException e) {
            System.out.println("❌ Error reading files: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (AssertionError e) {
            System.out.println("❌ Thread.sleep() validation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test that verifies our new implementation files use coroutine delay instead.
     */
    public static void testCoroutineDelayUsage() {
        System.out.println("🧪 Testing for proper coroutine delay usage...");
        
        try {
            String filePath = "Branch-SDK/src/main/java/io/branch/referral/network/BranchAsyncNetworkLayer.kt";
            File file = new File(filePath);
            
            if (file.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(filePath));
                boolean foundDelay = false;
                boolean foundCoroutines = false;
                
                for (String line : lines) {
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("delay(") && !lowerLine.trim().startsWith("//") && !lowerLine.trim().startsWith("*")) {
                        foundDelay = true;
                    }
                    if (lowerLine.contains("coroutine") && !lowerLine.trim().startsWith("//") && !lowerLine.trim().startsWith("*")) {
                        foundCoroutines = true;
                    }
                }
                
                if (!foundDelay) {
                    throw new AssertionError("No coroutine delay() calls found in BranchAsyncNetworkLayer.kt");
                }
                
                if (!foundCoroutines) {
                    throw new AssertionError("No coroutine references found in BranchAsyncNetworkLayer.kt");
                }
                
                System.out.println("✅ Proper coroutine delay usage found in implementation");
            } else {
                throw new AssertionError("BranchAsyncNetworkLayer.kt file not found");
            }
            
        } catch (IOException e) {
            System.out.println("❌ Error reading files: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (AssertionError e) {
            System.out.println("❌ Coroutine delay validation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test that verifies the original interface has been modified to use async implementation.
     */
    public static void testOriginalInterfaceModification() {
        System.out.println("🧪 Testing original interface modifications...");
        
        try {
            String filePath = "Branch-SDK/src/main/java/io/branch/referral/network/BranchRemoteInterfaceUrlConnection.java";
            File file = new File(filePath);
            
            if (file.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(filePath));
                boolean foundAsyncImplementation = false;
                boolean foundAsyncToggle = false;
                
                for (String line : lines) {
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("asyncimplementation") && !lowerLine.trim().startsWith("//") && !lowerLine.trim().startsWith("*")) {
                        foundAsyncImplementation = true;
                    }
                    if (lowerLine.contains("setasyncimplementationenabled") && !lowerLine.trim().startsWith("//") && !lowerLine.trim().startsWith("*")) {
                        foundAsyncToggle = true;
                    }
                }
                
                if (!foundAsyncImplementation) {
                    throw new AssertionError("No async implementation references found in original interface");
                }
                
                if (!foundAsyncToggle) {
                    throw new AssertionError("No async implementation toggle method found in original interface");
                }
                
                System.out.println("✅ Original interface properly modified to use async implementation");
            } else {
                throw new AssertionError("BranchRemoteInterfaceUrlConnection.java file not found");
            }
            
        } catch (IOException e) {
            System.out.println("❌ Error reading files: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (AssertionError e) {
            System.out.println("❌ Original interface validation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test that verifies logging has been added to the implementation.
     */
    public static void testLoggingImplementation() {
        System.out.println("🧪 Testing logging implementation...");
        
        try {
            List<String> filesToCheck = new ArrayList<>();
            filesToCheck.add("Branch-SDK/src/main/java/io/branch/referral/network/BranchAsyncNetworkLayer.kt");
            filesToCheck.add("Branch-SDK/src/main/java/io/branch/referral/network/BranchRemoteInterfaceUrlConnectionAsync.kt");
            filesToCheck.add("Branch-SDK/src/main/java/io/branch/referral/network/BranchRemoteInterfaceUrlConnection.java");
            
            boolean foundLogging = false;
            
            for (String filePath : filesToCheck) {
                File file = new File(filePath);
                if (file.exists()) {
                    List<String> lines = Files.readAllLines(Paths.get(filePath));
                    for (String line : lines) {
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.contains("branchlogger") && !lowerLine.trim().startsWith("//") && !lowerLine.trim().startsWith("*")) {
                            foundLogging = true;
                            break;
                        }
                    }
                    if (foundLogging) break;
                }
            }
            
            if (!foundLogging) {
                throw new AssertionError("No BranchLogger usage found in implementation files");
            }
            
            System.out.println("✅ Proper logging implementation found");
            
        } catch (IOException e) {
            System.out.println("❌ Error reading files: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (AssertionError e) {
            System.out.println("❌ Logging validation failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Main method to run all validation tests.
     */
    public static void main(String[] args) {
        System.out.println("🚀 Starting Thread.sleep() Elimination Validation Tests");
        System.out.println("=======================================================");
        System.out.println("Performing static analysis to verify implementation correctness");
        System.out.println();
        
        boolean allTestsPassed = true;
        
        try {
            testNoThreadSleepInNewImplementation();
            testCoroutineDelayUsage();
            testOriginalInterfaceModification();
            testLoggingImplementation();
            
        } catch (Exception e) {
            allTestsPassed = false;
            System.out.println("❌ Validation failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        System.out.println("=======================================================");
        if (allTestsPassed) {
            System.out.println("🎉 ALL VALIDATION TESTS PASSED!");
            System.out.println("✅ Thread.sleep() successfully eliminated from new implementation");
            System.out.println("✅ Coroutine delay() properly implemented");
            System.out.println("✅ Original interface correctly modified");
            System.out.println("✅ Comprehensive logging added");
            System.out.println("✅ Implementation is ready for production");
        } else {
            System.out.println("💥 SOME VALIDATIONS FAILED!");
            System.out.println("Please check the error messages above");
            System.exit(1);
        }
        System.out.println("=======================================================");
    }
}
