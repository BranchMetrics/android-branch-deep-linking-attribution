package io.branch.search;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sojanpr on 2/27/17.
 * <p>
 * Class to represent an application matching with Branch Search query
 * This points to an application that or its content matches the search query.
 * </p>
 */
public class AppResult implements Parcelable {

    private final String packageName;
    private final String appName;
    private final String appIconUrl;
    private final boolean installedOnDevice;
    private Drawable appIconDrawable;

    public AppResult(String packageName, String appName, String appIconUrl, boolean installedOnDevice) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIconUrl = appIconUrl;
        this.installedOnDevice = installedOnDevice;
    }

    /**
     * Get the application title
     *
     * @return {@link String} representing the Application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Get package name for the application
     *
     * @return {@link String} package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Get the url for the app icon. Use this URL to show the application Icon if this app is not installed on the device
     *
     * @return {@link String} A url pointing to application icon
     * @see {@link #getAppIconDrawable(Context context)} and {@link #isInstalledOnDevice()}
     */
    public String getAppIconUrl() {
        return appIconUrl;
    }

    /**
     * Checks if the application represented is locally installed  on the device
     *
     * @return {@code true} if the app is installed  on the device
     */
    public boolean isInstalledOnDevice() {
        return installedOnDevice;
    }

    /**
     * Get the icon drawable for this application. This will return null if the app is not installed
     *
     * @param context current context
     * @return {@link Drawable} application icon
     * @see {@link #getAppIconUrl()} and {@link #isInstalledOnDevice()}
     */
    public Drawable getAppIconDrawable(Context context) {
        if (appIconDrawable == null) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                appIconDrawable = context.getPackageManager().getApplicationIcon(info);
            } catch (PackageManager.NameNotFoundException ignore) {

            }
        }
        return appIconDrawable;
    }


    //------------------- parcelable implementation --------------------/

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.appName);
        dest.writeString(this.appIconUrl);
        dest.writeByte(this.installedOnDevice ? (byte) 1 : (byte) 0);
    }

    protected AppResult(Parcel in) {
        this.packageName = in.readString();
        this.appName = in.readString();
        this.appIconUrl = in.readString();
        this.installedOnDevice = in.readByte() != 0;
    }

    public static final Parcelable.Creator<AppResult> CREATOR = new Parcelable.Creator<AppResult>() {
        @Override
        public AppResult createFromParcel(Parcel source) {
            return new AppResult(source);
        }

        @Override
        public AppResult[] newArray(int size) {
            return new AppResult[size];
        }
    };
}
