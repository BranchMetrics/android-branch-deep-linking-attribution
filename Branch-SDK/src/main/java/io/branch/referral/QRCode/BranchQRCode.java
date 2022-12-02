package io.branch.referral.QRCode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchQRCodeCache;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerResponse;
import io.branch.referral.util.LinkProperties;

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
    /* The style of code pattern used to generate the QR code. */
    private BranchQRCodePattern pattern_;
    /* The style of finder pattern used to generate the QR code. */
    private BranchQRCodeFinderPattern finderPattern_;
    /* Color of the QR code's finder pattern. */
    private String finderPatternColor_;
    /* A URL of an image that will be added to the background of the QR code. Must be a PNG or JPEG. */
    private String backgroundImage_;
    /* Adjusts the opacity of the background image from 1-99. */
    private Integer backgroundImageOpacity_;
    /* A URL of an image to be used as the code-pattern itself on the QR Code.. Must be a PNG or JPEG. */
    private String patternImage_;
    /* Color of the  interior part of a QR code’s finder pattern. */
    private String finderEyeColor_;

    public enum BranchImageFormat {
        JPEG, /* QR code is returned as a JPEG */
        PNG /*QR code is returned as a PNG */
    }

    public enum BranchQRCodePattern {
        Standard,
        Squares,
        Circles,
        Triangles,
        Diamonds,
        Hexagons,
        Octagons
    }

    public enum BranchQRCodeFinderPattern {
        Square,
        RoundedRectangle,
        Circle
    }

    public BranchQRCode() {
        codeColor_ = null;
        backgroundColor_ = null;
        centerLogo_ = null;
        width_ = null;
        margin_ = null;
        imageFormat_ = null;
        pattern_ = null;
        finderPattern_ = null;
        finderPatternColor_ = null;
        backgroundImage_ = null;
        backgroundImageOpacity_ = null;
        patternImage_ = null;
        finderEyeColor_ = null;
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

    /**
     * <p>
     * Set the pattern for this BranchQRCode.
     * </p>
     *
     * @param pattern A {@link BranchQRCodePattern} with value for the pattern.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setPattern(@NonNull BranchQRCodePattern pattern) {
        this.pattern_ = pattern;
        return this;
    }

    /**
     * <p>
     * Set the finder pattern for this BranchQRCode.
     * </p>
     *
     * @param finderPattern A {@link BranchQRCodeFinderPattern} with value for the finder pattern.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setFinderPattern(@NonNull BranchQRCodeFinderPattern finderPattern) {
        this.finderPattern_ = finderPattern;
        return this;
    }

    /**
     * <p>
     * Set the finder pattern color for this BranchQRCode.
     * </p>
     *
     * @param finderPatternColor A {@link int} with value for the finder pattern color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setFinderPatternColor(@NonNull int finderPatternColor) {
        String finderPatternColorString = String.format("#%06X", 0xFFFFFF & finderPatternColor);
        return setFinderPatternColor(finderPatternColorString);
    }

    /**
     * <p>
     * Set the finder pattern color for this BranchQRCode.
     * </p>
     *
     * @param hexFinderPatternColor A {@link String} with value for the finder pattern color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setFinderPatternColor(@NonNull String hexFinderPatternColor) {
        this.finderPatternColor_ = hexFinderPatternColor;
        return this;
    }

    /**
     * <p>
     * Set the background image for this BranchQRCode.
     * </p>
     *
     * @param backgroundImage A {@link String} with value for the background image.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setBackgroundImage(@NonNull String backgroundImage) {
        this.backgroundImage_ = backgroundImage;
        return this;
    }

    /**
     * <p>
     * Set the background image opacity for this BranchQRCode.
     * </p>
     *
     * @param backgroundImageOpacity An {@link Integer} with value for the background image opacity from 1-99.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setBackgroundImageOpacity(@NonNull Integer backgroundImageOpacity) {
        if (backgroundImageOpacity > 99) {
            PrefHelper.Debug("Background image opacity was reduced to the maximum of 99.");
            this.backgroundImageOpacity_ = 99;
        } else if (backgroundImageOpacity < 1) {
            PrefHelper.Debug("Background image opacity was increased to the minimum of 1.");
            this.backgroundImageOpacity_ = 1;
        } else {
            this.backgroundImageOpacity_ = backgroundImageOpacity;
        }
        return this;
    }

    /**
     * <p>
     * Set the pattern image for this BranchQRCode.
     * </p>
     *
     * @param patternImage A {@link String} with value for the pattern image.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setPatternImage(@NonNull String patternImage) {
        this.patternImage_ = patternImage;
        return this;
    }

    /**
     * <p>
     * Set the finder eye color for this BranchQRCode.
     * </p>
     *
     * @param finderEyeColor A {@link int} with value for the finder eye color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setFinderEyeColor(@NonNull int finderEyeColor) {
        String finderEyeColorString = String.format("#%06X", 0xFFFFFF & finderEyeColor);
        return setFinderEyeColor(finderEyeColorString);
    }

    /**
     * <p>
     * Set the finder eye color for this BranchQRCode.
     * </p>
     *
     * @param hexFinderEyeColor A {@link String} with value for the finder eye color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setFinderEyeColor(@NonNull String hexFinderEyeColor) {
        this.finderEyeColor_ = hexFinderEyeColor;
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

        if (this.pattern_ != null) {
            settings.put(Defines.Jsonkey.CodePattern.getKey(), pattern_.ordinal() + 1);
        }
        if (this.finderPattern_ != null) {
            settings.put(Defines.Jsonkey.FinderPattern.getKey(), finderPattern_.ordinal() + 1);
        }
        if (this.finderPatternColor_ != null) {
            settings.put(Defines.Jsonkey.FinderPatternColor.getKey(), finderPatternColor_);
        }
        if (this.backgroundImage_ != null) {
            settings.put(Defines.Jsonkey.BackgroundImage.getKey(), backgroundImage_);
        }
        if (this.backgroundImageOpacity_ != null) {
            settings.put(Defines.Jsonkey.BackgroundImageOpacity.getKey(), backgroundImageOpacity_);
        }
        if (this.patternImage_ != null) {
            settings.put(Defines.Jsonkey.PatternImage.getKey(), patternImage_);
        }
        if (this.finderEyeColor_ != null) {
            settings.put(Defines.Jsonkey.FinderEyeColor.getKey(), finderEyeColor_);
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
            callback.onSuccess(cachedQRCode);
            return;
        } else {
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
}
