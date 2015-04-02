package io.branch.referral;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * <p>Class that provides a series of methods providing access to commonly used, device-wide 
 * attributes and parameters used by the Branch class, and made publically available for use by 
 * other classes.</p>
 */
public class SystemObserver {
	
	/**
	 * Default value for when no value has been returned by a system information call, but where 
	 * null is not supported or desired.
	 */
	public static final String BLANK = "bnc_no_value";

	private Context context_;
	
	/**
	 * <p>Indicates whether or not a real device ID is in use, or if a debug value is in use. This 
	 * value is referred to by the {@link SystemObserver#hasRealHardwareId()}, and querying of its 
	 * value allows debug ANDROID_ID values to be ignored from analytics by the Branch class and any 
	 * other analytics libraries.</p>
	 */
	private boolean isRealHardwareId;
	
	/**
	 * <p>Sole constructor method of the {@link SystemObserver} class. Instantiates the value of 
	 * <i>isRealHardware</i> {@link Boolean} value as <i>true</i>.</p>
	 * 
	 * @param context
	 */
	public SystemObserver(Context context) {
		context_ = context;
		isRealHardwareId = true;
	}
	
	/**
	 * <p>Gets the {@link String} value of the {@link Secure#ANDROID_ID} setting in the device. This 
	 * immutable value is generated upon initial device setup, and re-used throughout the life of 
	 * the device.</p>
	 * 
	 * <p>If <i>true</i> is provided as a parameter, the method will return a different, 
	 * randomly-generated value each time that it is called. This allows you to simulate many different 
	 * user devices with different ANDROID_ID values with a single physical device or emulator.</p>
	 * 
	 * @param debug A {@link Boolean} value indicating whether to run in <i>real</i> or <i>debug mode</i>.
	 * @return
	 * <p>A {@link String} value representing the unique ANDROID_ID of the device, or a randomly-generated 
	 * debug value in place of a real identifier.</p>
	 */
	public String getUniqueID(boolean debug) {
		if (context_ != null) { 
			String androidID = null;
			if (!debug) {
				androidID = Secure.getString(context_.getContentResolver(), Secure.ANDROID_ID);
			}
			if (androidID == null) {
				androidID = UUID.randomUUID().toString();
				isRealHardwareId = false;
			}
			return androidID;
		} else 
			return BLANK;
	}
	
	/**
	 * <p>Checks the value of the <i>isRealHardWareId</i> {@link Boolean} value within the current 
	 * instance of the class. If not set by the {@link SystemObserver#hasRealHardwareId()} 
	 * or will default to true upon class instantiation.</p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value indicating whether or not the current device has a hardware 
	 * identifier; {@link Secure#ANDROID_ID}</p>
	 */
	public boolean hasRealHardwareId() {
		return isRealHardwareId;
	}
	
	/**
	 * <p>Provides the package name of the current app, and passes it to the 
	 * {@link SystemObserver#getURIScheme(String)} method to enable the call without a {@link String} 
	 * parameter.</p>
	 * 
	 * <p>This method should be used for retrieving the URI scheme of the current application.</p>
	 * 
	 * @return A {@link String} value containing the response from {@link SystemObserver#getURIScheme(String)}.
	 */
	public String getURIScheme() {
	    return getURIScheme(context_.getPackageName());
	}
	
	/**
	 * <p>Gets the URI scheme of the specified package from its AndroidManifest.xml file.</p>
	 * 
	 * <p>This method should be used for retrieving the URI scheme of the another application of 
	 * which the package name is known.</p>
	 * 
	 * @param packageName A {@link String} containing the full package name of the app to check.
	 * @return
	 * <p>A {@link String} containing the output of {@link ApkParser#decompressXML(String)}.</p>
	 */
	public String getURIScheme(String packageName) {
		String scheme = BLANK;
		if (!isLowOnMemory()) {
			PackageManager pm = context_.getPackageManager();
			try {
		        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
		        String sourceApk = ai.publicSourceDir;
		        JarFile jf = null;
		        InputStream is = null;
		        byte[] xml = null;
		        try {
		            jf = new JarFile(sourceApk);
	            	is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
		            xml = new byte[is.available()];
	                //noinspection ResultOfMethodCallIgnored
	                is.read(xml);
		            scheme = new ApkParser().decompressXML(xml);
		        } catch (Exception ignored) {
                } catch (OutOfMemoryError ignored) {
		        } finally {
		        	xml = null;
		        	try {
		        		if (is != null) {
		        			is.close();
		        			is = null;
		        		}
		        		if (jf != null) {
		        			jf.close();	
		        			jf = null;
		        		}
		        	} catch (IOException ignored) {}
		        }
		    } catch (NameNotFoundException ignored) {
		    }
		}
		return scheme;
	}
	
	/**
	 * <p>Checks the current device's {@link ActivityManager} system service and returns the value 
	 * of the lowMemory flag.</p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value representing the low memory flag of the current device.</p>
	 * 
	 * <ul>
	 * <li><i>true</i> - the free memory on the current device is below the system-defined threshold 
	 * that triggers the low memory flag.</li>
	 * <li><i>false</i> - the device has plenty of free memory.</li>
	 * </ul>
	 */
	private boolean isLowOnMemory() {
		ActivityManager activityManager = (ActivityManager) context_.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		activityManager.getMemoryInfo(mi);
		return mi.lowMemory;
	}
	
	/**
	 * <p>Gets a {@link JSONArray} object containing a list of the applications that are installed 
	 * on the current device.</p>
	 * 
	 * <p>The method gets a handle on the {@link PackageManager} and calls the 
	 * {@link PackageManager#getInstalledApplications(int)} method to retrieve a {@link List} of 
	 * {@link ApplicationInfo} objects, each representing a single application that is installed on 
	 * the current device.</p>
	 * 
	 * <p>For each of these items, the method gets the attributes shown below, and constructs a 
	 * {@link JSONArray} representation of the list, which can be consumed by a JSON parser on the 
	 * server.</p>
	 * 
	 * <ul>
	 * <li>loadLabel</li>
	 * <li>packageName</li>
	 * <li>publicSourceDir</li>
	 * <li>sourceDir</li>
	 * <li>publicSourceDir</li>
	 * <li>uriScheme</li>
	 * </ul>
	 * 
	 * @return
	 * <p>A {@link JSONArray} containing information about all of the applications installed on the 
	 * current device.</p>
	 */
	@SuppressLint("NewApi")
	public JSONArray getListOfApps() {
		JSONArray arr = new JSONArray();
		PackageManager pm = context_.getPackageManager();
		
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		if (packages != null) {
			for (ApplicationInfo appInfo : packages) {
				if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
					JSONObject packObj = new JSONObject();
					try {
						String label = appInfo.loadLabel(pm).toString();
						if (label != null)
							packObj.put("name", label);
						String packName = appInfo.packageName;
						if (packName != null) {
							packObj.put("app_identifier", packName);
							String uriScheme = getURIScheme(packName);
							if (!uriScheme.equals(SystemObserver.BLANK))
								packObj.put("uri_scheme", uriScheme);
						}
						String pSourceDir = appInfo.publicSourceDir;
						if (pSourceDir != null)
							packObj.put("public_source_dir", pSourceDir);
						String sourceDir = appInfo.sourceDir;
						if (sourceDir != null)
							packObj.put("source_dir", sourceDir);
						
						PackageInfo packInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
						if (packInfo != null) {
							if (packInfo.versionCode >= 9) {
								packObj.put("install_date", packInfo.firstInstallTime);
								packObj.put("last_update_date", packInfo.lastUpdateTime);
							}
							packObj.put("version_code", packInfo.versionCode);
							if (packInfo.versionName != null)
								packObj.put("version_name", packInfo.versionName);
						}
						packObj.put("os", this.getOS());
						
						arr.put(packObj);
					} catch(JSONException ignore) {
					} catch(NameNotFoundException ignore) {			
					}
				}
			}
		}
		return arr;
	}
	
	/**
	 * <p>Gets the package name of the current application that the SDK is integrated with.</p>
	 * 
	 * @return
	 * <p>A {@link String} value containing the full package name of the application that the SDK is 
	 * currently integrated into.</p>
	 */
	public String getAppVersion() {
		 try {
			 PackageInfo packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
			 if (packageInfo.versionName != null)
				 return packageInfo.versionName;
			 else
				 return BLANK;
		 } catch (NameNotFoundException ignored ) {
		 }
		 return BLANK;
	}
	
	/**
	 * <p>Gets the name of the network operator that the current telephony-capable device is 
	 * connected to.
	 * </p>
	 * 
	 * @return
	 * <p>A {@link String} value containing the network-provided name of the telephony carrier that 
	 * the current device is connected to.</p>
	 */
	public String getCarrier() {
        TelephonyManager telephonyManager = (TelephonyManager) context_.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
        	String ret = telephonyManager.getNetworkOperatorName();
            if (ret != null)
            	return ret;
        }
        return BLANK;
	}
	
	/**
	 * <p>Checks whether the current device has Bluetooth capability of any kind by attempting to 
	 * instantiate a {@link BluetoothAdapter} object that points to the default Bluetooth adapter 
	 * of the device. If Bluetooth is not supported, the assignment will be null and false will be 
	 * returned as a result.
	 * </p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value indicating whether or not Bluetooth is supported <b>and enabled</b> 
	 * on the current device.</p>
	 * 
	 * <ul>
	 * <li><i>true</i> - the device supports Bluetooth, and the adapter is enabled.</li>
	 * <li><i>false</i> - the device does not support Bluetooth, or the adapter is disabled.</li>
	 * </ul>
	 */
	public boolean getBluetoothPresent() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                return bluetoothAdapter.isEnabled();
            }
        } catch (SecurityException ignored ) {
        }
        return false;
	}
	
	/**
	 * <p>Checks whether the current device supports 
	 * <a href="https://developer.android.com/guide/topics/connectivity/bluetooth-le.html">
	 * 		Bluetooth Low Energy (LE)</a>.
	 * </p>
	 * 
	 * <p>This is determined by checking the SDK version available on the current device. BTLE was 
	 * introduced in Android 4.3 (API Level 18), so if the current device reports that it supports 
	 * the highest platform version that is 17 or lower, the device does not support the required 
	 * platform hooks to communicate with capable devices via the Bluetooth Low-Energy profile.</p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value indicating whether or not Bluetooth LE is available on the current 
	 * device.</p>
	 * 
	 * <ul>
	 * <li><i>true</i> - this device supports Bluetooth LE.</li>
	 * <li><i>false</i> - this device does not support Bluetooth LE.</li>
	 * </ul>
	 */
	public String getBluetoothVersion() {
        if (android.os.Build.VERSION.SDK_INT >= 8) {
            if(android.os.Build.VERSION.SDK_INT >= 18 &&
                    context_.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                return "ble";
            } else if(context_.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                return "classic";
            }
        }
        return BLANK;
	}
	
	/**
	 * <p>Checks whether or not the current device has near-field communication capabilities.</p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value indicating whether or not NFC capabilities exist in the current 
	 * device.</p>
	 * 
	 * <ul>
	 *     <li><i>true</i> - Current device has NFC capabilities.</li>
	 *     <li><i>false</i> - Current device has no NFC capabilities.</li>
	 * </ul>
	 */
	public boolean getNFCPresent() {
		try {
			return context_.getPackageManager().hasSystemFeature("android.hardware.nfc");
		} catch (Exception ignored ) {
		}
		return false;
	}
	
	/**
	 * <p>Checks whether or not the current device has telephone capabilities; i.e. whether or not 
	 * it is able to connect to a mobile or cellphone network and send or recieve SMS messages, 
	 * and or make calls.
	 * </p>
	 * 
	 * @return 
	 * <p>A {@link Boolean} value indicating whether telephony capabilities exist on the 
	 * current device.</p>
	 * 
	 * <ul>
	 * 	<li><i>true</i> - the device has telephony capabilities; is a phone or phablet.</li>
	 *  <li><i>false</i> - the device has telephony capabilities; is a WiFi tablet or other non-phone device.</li>
	 * </ul>
	 */
	public boolean getTelephonePresent() {
		try {
			return context_.getPackageManager().hasSystemFeature("android.hardware.telephony");
		} catch (Exception ignored ) {
		}
		return false;
	}
	
	/**
	 * <p>Returns the hardware manufacturer of the current device, as defined by the manufacturer.
	 * </p>
	 * 
	 * @see 
	 * <a href="http://developer.android.com/reference/android/os/Build.html#MANUFACTURER">
	 * Build.MANUFACTURER</a>
	 * 
	 * @return A {@link String} value containing the hardware manufacturer of the current device.
	 */
	public String getPhoneBrand() {
		return android.os.Build.MANUFACTURER;
	}
	
	/**
	 * <p>Returns the hardware model of the current device, as defined by the manufacturer.</p>
	 * 
	 * @see
	 * <a href="http://developer.android.com/reference/android/os/Build.html#MODEL">
	 * Build.MODEL
	 * </a>
	 * 
	 * @return A {@link String} value containing the hardware model of the current device.
	 */
	public String getPhoneModel() {
		return android.os.Build.MODEL;
	}
	
	/**
	 * <p>Hard-coded value, used by the Branch object to differentiate between iOS, Web and Android 
	 * SDK versions.</p>
	 * 
	 * <p>Not of practical use in your application.</p>
	 * 
	 * @return A {@link String} value that indicates the broad OS type that is in use on the device.
	 */
	public String getOS() {
		return "Android";
	}
	
	/**
	 * Returns the Android API version of the current device as an {@link Integer}.
	 * 
	 * Common values:
	 * <ul>
	 * <li>22 - Android 5.1, Lollipop MR1</li>
	 * <li>21 - Android 5.0, Lollipop</li>
	 * <li>19 - Android 4.4, Kitkat</li>
	 * <li>18 - Android 4.3, Jellybean</li>
	 * <li>15 - Android 4.0.4, Ice Cream Sandwich MR1</li>
	 * <li>13 - Android 3.2, Honeycomb MR2</li>
	 * <li>10 - Android 2.3.4, Gingerbread MR1</li>
	 * </ul>
	 * 
	 * @see 
	 * <a href="http://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels">
	 * Android Developers - API Level and Platform Version</a>
	 * 
	 * @return An {@link Integer} value representing the SDK/Platform Version of the OS of the 
	 * current device.
	 */
	public int getOSVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}
	
	/**
	 * <p>As the Build fingerprint is determined by the OS fingerprint, this will identify whether a 
	 * an emulator is being used where the developer of said emulator has followed convention and 
	 * used <b>generic</b> as the first segment of the virtual device fingerprint.</p>
	 * 
	 * <p>
	 * Example of a <u>real device</u> (Google Nexus 5, Android 5.1):<br/>
	 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
	 * <b>google</b>/hammerhead/hammerhead:5.1/LMY47D/1743759:user/release-keys</pre>
	 * </p>
	 * 
	 * <p>
	 * Example of an <u>emulator</u> (Genymotion Nexus 6 AVD, Android 5.0):<br/>
	 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
	 * <b>generic</b>/vbox86p/vbox86p:5.0/LRX21M/buildbot12160004:userdebug/test-keys</pre>
	 * </p>
	 * 
	 * @return
	 * <p>A {@link Boolean} value indicating whether the device upon which the app is being run is 
	 * a simulated platform, i.e. an emulator. Or a real, hardware device.</p>
	 * 
	 * <ul>
	 * <li><i>true</i> - the app is running on an emulator</li>
	 * <li><i>false</i> - the app is running on a physical device (or a badly configured AVD)</li>
	 * </ul>
	 */
	public boolean isSimulator() {
		return android.os.Build.FINGERPRINT.contains("generic");
	}
	
	/**
	 * <p>This method returns an {@link Integer} value dependent on whether the application has been 
	 * updated since its installation on the device. If the application has just been installed and 
	 * launched immediately, this will always return 1.</p>
	 * 
	 * <p>If however the application has already been installed for more than the duration of a 
	 * single update cycle, and has received one or more updates, the time in 
	 * {@link PackageInfo#firstInstallTime} will be different from that in 
	 * {@link PackageInfo#lastUpdateTime} so the return value will be 0; indicative of an update 
	 * having occurred whilst the app has been installed.</p>
	 * 
	 * <p>This is useful to know when the manner of handling of deep-link data has changed betwen 
	 * application versions and where migration of SharedPrefs may be required. This method provides 
	 * a condition upon which a consistency check or migration validation operation can be carried 
	 * out.</p>
	 * 
	 * <p>This will not work on Android SDK versions < 9, as the {@link PackageInfo#firstInstallTime} 
	 * and {@link PackageInfo#lastUpdateTime} values did not exist in older versions of the 
	 * {@link PackageInfo} class.</p>
	 * 
	 * @return
	 * <p>A {@link Integer} value indicating the update state of the application package.</p>
	 * <ul>
	 * <li><i>1</i> - App not updated since install.</li>
	 * <li><i>0</i> - App has been updated since initial install.</li>
	 * </ul>
	 */
	@SuppressLint("NewApi")
	public int getUpdateState() {
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			try {
				PackageInfo packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
				if (packageInfo.lastUpdateTime != packageInfo.firstInstallTime) {
					return 1;
				} else {
					return 0;
				}
			} catch (NameNotFoundException ignored ) {
			}
		}
		return 0;
	}
	
	/**
	 * <p>This method returns a {@link DisplayMetrics} object that contains the attributes of the 
	 * default display of the device that the SDK is running on. Use this when you need to know the 
	 * dimensions of the screen, density of pixels on the display or any other information that 
	 * relates to the device screen.</p>
	 * 
	 * <p>Especially useful when operating without an Activity context, e.g. from a background 
	 * service.</p>
	 * 
	 * @see DisplayMetrics
	 * 
	 * @return
	 * <p>A {@link DisplayMetrics} object representing the default display of the device.</p>
	 */
	public DisplayMetrics getScreenDisplay() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		Display display = ((WindowManager) context_.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);
        return displayMetrics;
	}
	
	/**
	 * <p>Use this method to query the system state to determine whether a WiFi connection is 
	 * available for use by applications installed on the device.</p>
	 * 
	 * This applies only to WiFi connections, and does not indicate whether there is 
	 * a viable Internet connection available; if connected to an offline WiFi router for instance, 
	 * the boolean will still return <i>true</i>.</p>
	 * 
	 * @return 
	 * <p>
	 * A {@link boolean} value that indicates whether a WiFi connection exists and is open.
	 * </p>
	 * <ul>
	 * <li><i>true</i> - A WiFi connection exists and is open.</li>
	 * <li><i>false</i> - Not connected to WiFi.</li>
	 * </ul>
	 */
	public boolean getWifiConnected() {
		if (PackageManager.PERMISSION_GRANTED == context_.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context_.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo.isConnected();
        }
		return false;
	}
	
	/**
	 * <p>Google now requires that all apps use a standardised Advertising ID for all ad-based 
	 * actions within Android apps.</p>
	 * 
	 * <p>The Google Play services APIs expose the advertising tracking ID as UUID such as this:</p>
	 * 
	 * <pre>38400000-8cf0-11bd-b23e-10b96e40000d</pre>
	 * 
	 * @see <a href="https://developer.android.com/google/play-services/id.html">
	 * Android Developers - Advertising ID</a>
	 * 
	 * @return
	 * <p>A {@link String} value containing the client ad UUID as supplied by Google Play.</p>
	 */
	public String getAdvertisingId() {
		String advertisingId = null;
		
		try {
		    Class<?> AdvertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
		    Method getAdvertisingIdInfoMethod = AdvertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context.class);
		    Object adInfoObj = getAdvertisingIdInfoMethod.invoke(null, context_);
		    Method getIdMethod = adInfoObj.getClass().getMethod("getId");
		    advertisingId = (String) getIdMethod.invoke(adInfoObj);
		} catch(IllegalStateException ex) {
			ex.printStackTrace();
		} catch(Exception ignore) {
		}
		
		return advertisingId;
	}
}
