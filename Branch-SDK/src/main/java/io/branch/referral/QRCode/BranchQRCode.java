package io.branch.referral.QRCode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchQRCodeCache;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerResponse;
import io.branch.referral.SharingHelper;
import io.branch.referral.network.BranchRemoteInterface;
import io.branch.referral.network.BranchRemoteInterfaceUrlConnection;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

public class BranchQRCode {

    /* Primary color of the generated QR code itself. */
    private String codeColor_;
    /* Secondary color used as the QR Code background. */
    private String backgroundColor_;
    /* A URL of an image that will be added to the center of the QR code. Must be a PNG or JPEG. */
    private String centerLogo_;
    /* Output size of QR Code image. Min 300px. Max 2000px. */
    private Integer width_;
    /*  The number of pixels for the QR code's border.  Min 1px. Max 20px. */
    private Integer margin_;
    /* Image Format of the returned QR code. Can be a JPEG or PNG. */
    private BranchImageFormat imageFormat_;

    public enum BranchImageFormat {
        JPEG, /* QR code is returned as a JPEG */
        PNG /*QR code is returned as a PNG */
    }

    public BranchQRCode() {
        codeColor_ = null;
        backgroundColor_ = null;
        centerLogo_ = null;
        width_ = null;
        margin_ = null;
        imageFormat_ = null;
    }

    /**
     * <p>
     * Set the code color for this BranchQRCode.
     * </p>
     *
     * @param codeColor A {@link int} with value for the code color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setCodeColor(@NonNull int codeColor) {
        String codeColorString = String.format("#%06X", 0xFFFFFF & codeColor);
        return setCodeColor(codeColorString);
    }

    /**
     * <p>
     * Set the code color for this BranchQRCode.
     * </p>
     *
     * @param hexCodeColor A {@link String} with value for the code color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setCodeColor(@NonNull String hexCodeColor) {
        this.codeColor_ = hexCodeColor;
        return this;
    }

    /**
     * <p>
     * Set the background color for this BranchQRCode.
     * </p>
     *
     * @param backgroundColor A {@link int} with value for the background color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setBackgroundColor(@NonNull int backgroundColor) {
        String backgroundColorString = String.format("#%06X", 0xFFFFFF & backgroundColor);
        return setBackgroundColor(backgroundColorString);
    }

    /**
     * <p>
     * Set the background color for this BranchQRCode.
     * </p>
     *
     * @param hexBackgroundColor A {@link String} with value for the background color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setBackgroundColor(@NonNull String hexBackgroundColor) {
        this.backgroundColor_ = hexBackgroundColor;
        return this;
    }

    /**
     * <p>
     * Set the center logo for this BranchQRCode.
     * </p>
     *
     * @param centerLogo A {@link String} with value for the center logo.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setCenterLogo(@NonNull String centerLogo) {
        this.centerLogo_ = centerLogo;
        return this;
    }

    /**
     * <p>
     * Set the image width for this BranchQRCode.
     * </p>
     *
     * @param width A {@link Integer} with value for the width.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setWidth(@NonNull Integer width) {
        if (width > 2000) {
            PrefHelper.Debug("Width was reduced to the maximum of 2000.");
            this.width_ = 2000;
        } else if (width < 300) {
            PrefHelper.Debug("Width was increased to the minimum of 300.");
            this.width_ = 300;
        } else {
            this.width_ = width;
        }
        return this;
    }

    /**
     * <p>
     * Set the border margin for this BranchQRCode.
     * </p>
     *
     * @param margin A {@link Integer} with value for the margin.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setMargin(@NonNull Integer margin) {
        if (margin > 20) {
            PrefHelper.Debug("Margin was reduced to the maximum of 20.");
            this.margin_ = 20;
        } else if (margin < 1) {
            PrefHelper.Debug("Margin was increased to the minimum of 1.");
            this.margin_ = 1;
        } else {
            this.margin_ = margin;
        }
        return this;
    }

    /**
     * <p>
     * Set the image format for this BranchQRCode.
     * </p>
     *
     * @param imageFormat A {@link BranchImageFormat} with value for the image format.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setImageFormat(@NonNull BranchImageFormat imageFormat) {
        this.imageFormat_ = imageFormat;
        return this;
    }

    public interface BranchQRCodeDataHandler<T> {
        void onSuccess(byte[] qrCodeData);

        void onFailure(Exception e);
    }

    public interface BranchQRCodeImageHandler<T> {
        void onSuccess(Bitmap qrCodeImage);

        void onFailure(Exception e);
    }

    public interface BranchQRCodeRequestHandler<T> {
        void onDataReceived(ServerResponse data);

        void onFailure(Exception e);
    }

    public void getQRCodeAsData(@NonNull Context context, @NonNull BranchUniversalObject branchUniversalObject, @NonNull LinkProperties linkProperties, @NonNull final BranchQRCodeDataHandler callback) throws IOException {
        Map<String, Object> settings = new HashMap<String, Object>();
        if (this.codeColor_ != null) {
            settings.put(Defines.Jsonkey.CodeColor.getKey(), codeColor_);
        }
        if (this.backgroundColor_ != null) {
            settings.put(Defines.Jsonkey.BackgroundColor.getKey(), backgroundColor_);
        }
        if (this.width_ != null) {
            settings.put(Defines.Jsonkey.Width.getKey(), width_);
        }
        if (this.margin_ != null) {
            settings.put(Defines.Jsonkey.Margin.getKey(), margin_);
        }
        if (this.imageFormat_ == BranchImageFormat.JPEG) {
            settings.put(Defines.Jsonkey.ImageFormat.getKey(), "JPEG");
        } else {
            settings.put(Defines.Jsonkey.ImageFormat.getKey(), "PNG");
        }
        if (this.centerLogo_ != null) {
            settings.put(Defines.Jsonkey.CenterLogo.getKey(), centerLogo_);
        }

        final Map<String, Object> parameters = new HashMap<String, Object>();

        if (linkProperties.getChannel() != null) {
            parameters.put(Defines.LinkParam.Channel.getKey(), linkProperties.getChannel());
        }
        if (linkProperties.getFeature() != null) {
            parameters.put(Defines.LinkParam.Feature.getKey(), linkProperties.getFeature());
        }
        if (linkProperties.getCampaign() != null) {
            parameters.put(Defines.LinkParam.Campaign.getKey(), linkProperties.getCampaign());
        }
        if (linkProperties.getStage() != null) {
            parameters.put(Defines.LinkParam.Stage.getKey(), linkProperties.getStage());
        }
        if (linkProperties.getTags() != null) {
            parameters.put(Defines.LinkParam.Tags.getKey(), linkProperties.getTags());
        }

        parameters.put(Defines.Jsonkey.QRCodeSettings.getKey(), settings);
        parameters.put(Defines.Jsonkey.QRCodeData.getKey(), branchUniversalObject.convertToJson());
        parameters.put(Defines.Jsonkey.QRCodeBranchKey.getKey(), PrefHelper.getInstance(context).getBranchKey());

        final JSONObject paramsJSON = new JSONObject(parameters);

        byte[] cachedQRCode = BranchQRCodeCache.getInstance().checkQRCodeCache(paramsJSON);
        if (cachedQRCode != null) {
            Log.d("QR Code Cache", "Found params in the cache. Returning QR Code");
            callback.onSuccess(cachedQRCode);
            return;
        } else {
            Log.d("QR Code Cache", "Did not find these params in the cache.");
        }

        ServerRequestCreateQRCode req = new ServerRequestCreateQRCode(Defines.RequestPath.QRCode, paramsJSON, context, new BranchQRCodeRequestHandler() {
            @Override
            public void onDataReceived(ServerResponse data) {
                try {
                    String qrCodeString = data.getObject().getString(Defines.Jsonkey.QRCodeResponseString.getKey());
                    byte[] qrCodeBytes = Base64.decode(qrCodeString, Base64.DEFAULT);

                    final JSONObject cacheParamsJSON = new JSONObject(parameters);
                    BranchQRCodeCache.getInstance().addQRCodeToCache(cacheParamsJSON, qrCodeBytes);

                    callback.onSuccess(qrCodeBytes);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
        Branch.getInstance().handleNewRequest(req);
    }

    public void getQRCodeAsImage(@NonNull Activity activity, @NonNull BranchUniversalObject branchUniversalObject, @NonNull LinkProperties linkProperties, @NonNull final BranchQRCodeImageHandler callback) throws IOException {
        getQRCodeAsData(activity, branchUniversalObject, linkProperties, new BranchQRCodeDataHandler() {
            @Override
            public void onSuccess(byte[] qrCodeData) {
                Bitmap bmp = BitmapFactory.decodeByteArray(qrCodeData, 0, qrCodeData.length);
                callback.onSuccess(bmp);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void showShareSheetWithQRCode(@NonNull final Activity activity, @NonNull final BranchUniversalObject branchUniversalObject, @NonNull final LinkProperties linkProperties, final String title, @NonNull final BranchQRCodeImageHandler callback) throws IOException {

        getQRCodeAsData(activity, branchUniversalObject, linkProperties, new BranchQRCodeDataHandler() {
            @Override
            public void onSuccess(byte[] qrCodeData) {
                Bitmap bmp = BitmapFactory.decodeByteArray(qrCodeData, 0, qrCodeData.length);

                String path = MediaStore.Images.Media.insertImage(activity.getContentResolver(), bmp, title, null);
                Uri uri = Uri.parse(path);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);

                activity.startActivity(Intent.createChooser(intent, "Share QR Code"));

                callback.onSuccess(bmp);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
