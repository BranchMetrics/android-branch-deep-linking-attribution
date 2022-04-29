package io.branch.referral.QRCode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
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
    /* Output size of QR Code image. Min 500px. Max 2000px. */
    private Integer width_;
    /*  The number of pixels for the QR code's border.  Min 0px. Max 20px. */
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
        } else if (width < 500) {
            PrefHelper.Debug("Width was increased to the minimum of 500.");
            this.width_ = 500;
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
        } else if (margin < 0) {
            PrefHelper.Debug("Margin was increased to the minimum of 0.");
            this.margin_ = 0;
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

        Map<String, Object> parameters = new HashMap<String, Object>();

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

        JSONObject paramsJSON = new JSONObject(parameters);

        ServerRequestCreateQRCode req = new ServerRequestCreateQRCode(Defines.RequestPath.QRCode, paramsJSON, context, new BranchQRCodeRequestHandler() {
            @Override
            public void onDataReceived(ServerResponse data) {
                try {
                    String qrCodeString = data.getObject().getString("QRCodeData");
                    byte[] qrCodeBytes = qrCodeString.getBytes("UTF-8");
                    callback.onSuccess(qrCodeBytes);
                } catch (UnsupportedEncodingException | JSONException e) {
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
                //Convert byteArray to Image
                Bitmap bmp = BitmapFactory.decodeByteArray(qrCodeData, 0, qrCodeData.length);
                callback.onSuccess(bmp);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void showShareSheetWithQRCode(@NonNull final Activity activity, @NonNull final BranchUniversalObject branchUniversalObject, @NonNull final LinkProperties linkProperties, @NonNull final BranchQRCodeImageHandler callback) throws IOException {
        getQRCodeAsData(activity, branchUniversalObject, linkProperties, new BranchQRCodeDataHandler() {
            @Override
            public void onSuccess(byte[] qrCodeData) {
                //Convert byteArray to Image
                Bitmap bmp = BitmapFactory.decodeByteArray(qrCodeData, 0, qrCodeData.length);
                //Show share sheet

//                ShareSheetStyle shareSheetStyle = new ShareSheetStyle(activity, "My Sharing Message Title", "My Sharing message body")
//                        //.setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Save this URl", "Link added to clipboard")
//                        //.setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
//                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
//                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
//                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
//                        .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER)
//                        .setAsFullWidthStyle(true)
//                        .setSharingTitle("Share With")
//                        .includeInShareSheet(bmp);
//                branchUniversalObject.showShareSheet(activity, linkProperties, shareSheetStyle, new Branch.BranchLinkShareListener() {
//
//                    @Override
//                    public void onShareLinkDialogLaunched() {
//
//                    }
//
//                    @Override
//                    public void onShareLinkDialogDismissed() {
//
//                    }
//
//                    @Override
//                    public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
//
//                    }
//
//                    @Override
//                    public void onChannelSelected(String channelName) {
//
//                    }
//                });


                callback.onSuccess(bmp);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }


}
