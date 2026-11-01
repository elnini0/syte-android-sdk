package com.syte.ai.android_sdk.data.result.discovery;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A shoppable gallery: images with products tagged on them.
 */
public class ShoppableGalleryResult {

    /**
     * A product tagged on a gallery image, with its position as fractions of the image size.
     */
    public static class Tag {

        @Nullable private final String mSku;
        @Nullable private final String mTitle;
        @Nullable private final Double mX;
        @Nullable private final Double mY;
        private final boolean mIsAIDetected;

        Tag(JsonObject json) {
            mSku = Json.text(json, "sku");
            mTitle = Json.text(json, "title");
            JsonObject coordinates = Json.object(json, "coordinates");
            mX = Json.number(coordinates, "x");
            mY = Json.number(coordinates, "y");
            mIsAIDetected = "true".equals(Json.text(json, "isAIDetected"));
        }

        @Nullable public String getSku() { return mSku; }

        @Nullable public String getTitle() { return mTitle; }

        @Nullable public Double getX() { return mX; }

        @Nullable public Double getY() { return mY; }

        public boolean isAIDetected() { return mIsAIDetected; }

    }

    public static class Image {

        @Nullable private final String mId;
        @Nullable private final String mImageUrl;
        @Nullable private final String mAltText;
        @Nullable private final String mCaption;
        private final List<Tag> mTags = new ArrayList<>();

        Image(JsonObject json) {
            mId = Json.text(json, "id");
            mImageUrl = Json.text(json, "imageUrl");
            mAltText = Json.text(json, "altText");
            mCaption = Json.text(json, "caption");
            for (JsonElement tag : Json.array(json, "tags")) {
                JsonObject tagJson = Json.asObject(tag);
                if (tagJson != null) {
                    mTags.add(new Tag(tagJson));
                }
            }
        }

        @Nullable public String getId() { return mId; }

        @Nullable public String getImageUrl() { return mImageUrl; }

        @Nullable public String getAltText() { return mAltText; }

        @Nullable public String getCaption() { return mCaption; }

        public List<Tag> getTags() { return mTags; }

    }

    @Nullable private final String mId;
    @Nullable private final String mDisplayTitle;
    @Nullable private final String mGalleryName;
    @Nullable private final String mLayoutType;
    private final List<Image> mImages = new ArrayList<>();

    public ShoppableGalleryResult(JsonObject json) {
        mId = Json.text(json, "id");
        mDisplayTitle = Json.text(json, "displayTitle");
        mGalleryName = Json.text(json, "galleryName");
        mLayoutType = Json.text(Json.object(json, "layout"), "type");
        for (JsonElement image : Json.array(json, "images")) {
            JsonObject imageJson = Json.asObject(image);
            if (imageJson != null) {
                mImages.add(new Image(imageJson));
            }
        }
    }

    @Nullable public String getId() { return mId; }

    @Nullable public String getDisplayTitle() { return mDisplayTitle; }

    @Nullable public String getGalleryName() { return mGalleryName; }

    /**
     * @return layout set for the gallery in the Syte platform, e.g. "grid" or "carousel"
     */
    @Nullable public String getLayoutType() { return mLayoutType; }

    public List<Image> getImages() { return mImages; }

}
