package com.syte.ai.android_sdk.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.syte.ai.android_sdk.enums.ImageScale;
import com.syte.ai.android_sdk.exceptions.SyteGeneralException;
import com.syte.ai.android_sdk.exceptions.SyteWrongInputException;
import com.syte.ai.android_sdk.util.SyteLogger;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


class ImageProcessor {

    private static final String TAG = ImageProcessor.class.getSimpleName();

    /** Uploads above this many bytes are saved again at a lower quality. */
    static final int MAX_UPLOAD_BYTES = 300_000;
    static final int START_QUALITY = 90;
    static final int MIN_QUALITY = 20;
    static final int QUALITY_STEP = 10;

    static final float SMALL_IMAGE_MAX_WIDTH = 500f;
    static final float SMALL_IMAGE_MAX_HEIGHT = 1000f;
    static final float MEDIUM_IMAGE_MAX_WIDTH = 1400f;
    static final float MEDIUM_IMAGE_MAX_HEIGHT = 1400f;
    static final float LARGE_IMAGE_MAX_WIDTH = 2000f;
    static final float LARGE_IMAGE_MAX_HEIGHT = 2000f;

    interface Encoder {
        byte[] encode(int quality);
    }

    /**
     * The bytes to upload: the photo fitted inside the size for {@code scale}, keeping its
     * proportions and never enlarged, saved at the best JPEG quality that stays under
     * {@link #MAX_UPLOAD_BYTES}. Same approach as the iOS SDK.
     */
    byte[] compress(Bitmap bitmap, ImageScale scale) {
        float maxWidth;
        float maxHeight;
        switch (scale) {
            case SMALL:
                maxWidth = SMALL_IMAGE_MAX_WIDTH;
                maxHeight = SMALL_IMAGE_MAX_HEIGHT;
                break;
            case LARGE:
                maxWidth = LARGE_IMAGE_MAX_WIDTH;
                maxHeight = LARGE_IMAGE_MAX_HEIGHT;
                break;
            default:
                maxWidth = MEDIUM_IMAGE_MAX_WIDTH;
                maxHeight = MEDIUM_IMAGE_MAX_HEIGHT;
        }
        int[] size = fitInside(bitmap.getWidth(), bitmap.getHeight(), maxWidth, maxHeight);
        Bitmap scaled = size[0] == bitmap.getWidth() && size[1] == bitmap.getHeight()
                ? bitmap
                : Bitmap.createScaledBitmap(bitmap, size[0], size[1], true);

        int[] usedQuality = {START_QUALITY};
        byte[] bytes = encodeUnder(MAX_UPLOAD_BYTES, quality -> {
            usedQuality[0] = quality;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out);
            return out.toByteArray();
        });
        SyteLogger.i(TAG, "Photo upload: " + size[0] + "x" + size[1] + " at quality " + usedQuality[0] + ", " + bytes.length + " bytes");
        return bytes;
    }

    /** Width and height that fit inside the box with the same proportions, never larger than they were. */
    static int[] fitInside(int width, int height, float maxWidth, float maxHeight) {
        float ratio = Math.min(1f, Math.min(maxWidth / width, maxHeight / height));
        return new int[]{Math.max(1, Math.round(width * ratio)), Math.max(1, Math.round(height * ratio))};
    }

    /** Encodes at {@link #START_QUALITY}, then lower in steps of 10 until it fits, stopping at {@link #MIN_QUALITY}. */
    static byte[] encodeUnder(int maxBytes, Encoder encoder) {
        int quality = START_QUALITY;
        byte[] bytes = encoder.encode(quality);
        while (bytes.length > maxBytes && quality - QUALITY_STEP >= MIN_QUALITY) {
            quality -= QUALITY_STEP;
            bytes = encoder.encode(quality);
        }
        return bytes;
    }

    @Nullable
    Bitmap rotateImageIfNeeded(Context context, Uri uri) throws SyteGeneralException {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            throw new SyteGeneralException("Could not open stream for the URI: " + e.getMessage());
        }

        Matrix matrix = handleRotation(inputStream);
        Bitmap srcBitmap = getSourceBitmap(context, uri);

        if (srcBitmap != null) {
            return Bitmap.createBitmap(
                    srcBitmap,
                    0,
                    0,
                    srcBitmap.getWidth(),
                    srcBitmap.getHeight(),
                    matrix,
                    true);
        }

        return null;
    }

    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Matrix handleRotation(InputStream inputStream) throws SyteGeneralException {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(inputStream);
        } catch (IOException e) {
            throw new SyteGeneralException("Could not handle image rotation: " + e.getMessage());
        }
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        int rotation = exifToDegrees(orientation);

        SyteLogger.i(TAG, "Orientation: " + orientation);
        SyteLogger.i(TAG, "Rotation: " + rotation);

        Matrix matrix = new Matrix();
        if (rotation != 0) {
            matrix.postRotate(rotation);
        }

        return matrix;
    }

    @Nullable
    private Bitmap getSourceBitmap(Context context, Uri uri) throws SyteGeneralException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri));
            } catch (IOException e) {
                throw new SyteGeneralException("Could not decode bitmap: " + e.getMessage());
            }
        } else {
            try {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            } catch (IOException e) {
                throw new SyteGeneralException("Could not decode bitmap: " + e.getMessage());
            }
        }
    }

}
