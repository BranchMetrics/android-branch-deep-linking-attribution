package io.branch.referral;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
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
	private static final String BLANK = "bnc_no_value";

	private Context context_;
	
	public SystemObserver(Context context) {
		context_ = context;
	}
	
	public String getUniqueID() {
		if (context_ != null) { 
			String androidID = Secure.getString(context_.getContentResolver(), Secure.ANDROID_ID);
			if (androidID == null) {
				androidID = BLANK;
			}
			return androidID;
		} else 
			return BLANK;
	}
	
	public String getAppVersion() {
		 try {
			 PackageInfo packageInfo = context_.getPackageManager().getPackageInfo(context_.getPackageName(), 0);
			 if (packageInfo.versionName != null)
				 return packageInfo.versionName;
			 else
				 return BLANK;
		 } catch (NameNotFoundException e) {
			 e.printStackTrace();
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
        } catch (SecurityException e) {
            e.printStackTrace();
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean getTelephonePresent() {
		try {
			return context_.getPackageManager().hasSystemFeature("android.hardware.telephony");
		} catch (Exception e) {
			e.printStackTrace();
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
}
