package io.branch.referral;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

public class BranchQRCode {

    /* Primary color of the generated QR code itself. */
    private Color codeColor_;
    /* Secondary color used as the QR Code background. */
    private Color backgroundColor_;
    /* A URL of an image that will be added to the center of the QR code. Must be a PNG or JPEG. */
    private String centerLogo_;
    /* Output size of QR Code image. Min 500px. Max 2000px. */
    private Integer width_;
    /*  The number of pixels for the QR code's border.  Min 0px. Max 20px. */
    private Integer margin_;
    /* Image Format of the returned QR code. Can be a JPEG or PNG. */
    private IMAGE_FORMAT imageFormat_;

    public enum IMAGE_FORMAT {
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
     * @param codeColor A {@link Color} with value for the code color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setCodeColor(@NonNull Color codeColor) {
        this.codeColor_ = codeColor;
        return this;
    }

    /**
     * <p>
     * Set the background color for this BranchQRCode.
     * </p>
     *
     * @param backgroundColor A {@link Color} with value for the background color.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setBackgroundColor(@NonNull Color backgroundColor) {
        this.backgroundColor_ = backgroundColor;
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
        this.width_ = width;
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
        this.margin_ = margin;
        return this;
    }

    /**
     * <p>
     * Set the image format for this BranchQRCode.
     * </p>
     *
     * @param imageFormat A {@link IMAGE_FORMAT} with value for the image format.
     * @return This instance to allow for chaining of calls to set methods
     */
    public BranchQRCode setImageFormat(@NonNull IMAGE_FORMAT imageFormat) {
        this.imageFormat_ = imageFormat;
        return this;
    }

    public interface BranchQRCodeResultHandler<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }

    public void getQRCodeAsData(@NonNull Activity activity, @NonNull BranchUniversalObject branchUniversalObject, @NonNull LinkProperties linkProperties, @NonNull BranchQRCodeResultHandler callback) {
        Map<String,Object> settings =  new HashMap<String,Object>();
        if (this.codeColor_ != null) {
            settings.put("code_color", String.format("#%06X", 0xFFFFFF & codeColor_.hashCode()));
        }
        if (this.backgroundColor_ != null) {
            settings.put("background_color", String.format("#%06X", 0xFFFFFF & backgroundColor_.hashCode()));
        }
        if (this.width_ != null) {
            settings.put("width", width_);
        }
        if (this.margin_ != null) {
            settings.put("margin", margin_);
        }
        if (this.imageFormat_ == IMAGE_FORMAT.JPEG) {
            settings.put("image_format", "JPEG");
        } else {
            settings.put("image_format", "PNG");
        }

        if (this.centerLogo_ != null) {
            settings.put("center_logo_url", centerLogo_);
        }



    }

    public void getQRCodeAsImage(@NonNull Activity activity, @NonNull BranchUniversalObject branchUniversalObject, @NonNull LinkProperties linkProperties, @NonNull BranchQRCodeResultHandler callback) {

    }

    private void callQRCodeAPI(@NonNull Dictionary params, @NonNull BranchQRCodeResultHandler callback) {

    }
}
