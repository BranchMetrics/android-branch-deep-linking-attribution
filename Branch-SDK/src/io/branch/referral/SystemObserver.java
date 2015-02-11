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

public class SystemObserver {
	public static final String BLANK = "bnc_no_value";

	private Context context_;
	private boolean isRealHardwareId;
	
	public SystemObserver(Context context) {
		context_ = context;
		isRealHardwareId = true;
	}
	
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
	
	public boolean hasRealHardwareId() {
		return isRealHardwareId;
	}
	
	public String getURIScheme() {
	    return getURIScheme(context_.getPackageName());
	}
	
	public String getURIScheme(String packageName) {
		String scheme = BLANK;
		PackageManager pm = context_.getPackageManager();
		try {
	        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
	        String sourceApk = ai.publicSourceDir;
	        JarFile jf = null;
	        InputStream is = null;
	        byte[] xml = null;
	        try {
	            jf = new JarFile(sourceApk);
//	            if (jf.getEntry("AndroidManifest.xml").getSize() < 20000) {		// uncomment of we want to limit on manifest size
	            	is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
		            xml = new byte[is.available()];
	                //noinspection ResultOfMethodCallIgnored
	                is.read(xml);
		            scheme = new ApkParser().decompressXML(xml);
//	            }
	        } catch (Exception ignored) {
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
		return scheme;
	}
	
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
	
	public String getCarrier() {
        TelephonyManager telephonyManager = (TelephonyManager) context_.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
        	String ret = telephonyManager.getNetworkOperatorName();
            if (ret != null)
            	return ret;
        }
        return BLANK;
	}
	
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
	
	public boolean getNFCPresent() {
		try {
			return context_.getPackageManager().hasSystemFeature("android.hardware.nfc");
		} catch (Exception ignored ) {
		}
		return false;
	}
	
	public boolean getTelephonePresent() {
		try {
			return context_.getPackageManager().hasSystemFeature("android.hardware.telephony");
		} catch (Exception ignored ) {
		}
		return false;
	}
	
	public String getPhoneBrand() {
		return android.os.Build.MANUFACTURER;
	}
	
	public String getPhoneModel() {
		return android.os.Build.MODEL;
	}
	
	public String getOS() {
		return "Android";
	}
	
	public int getOSVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}
	
	public boolean isSimulator() {
		return android.os.Build.FINGERPRINT.contains("generic");
	}
	
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
	
	public DisplayMetrics getScreenDisplay() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		Display display = ((WindowManager) context_.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);
        return displayMetrics;
	}
	
	public boolean getWifiConnected() {
		if (PackageManager.PERMISSION_GRANTED == context_.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context_.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiInfo.isConnected();
        }
		return false;
	}
	
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
