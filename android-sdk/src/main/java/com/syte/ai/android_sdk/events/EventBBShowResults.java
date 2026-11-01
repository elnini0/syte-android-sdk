package com.syte.ai.android_sdk.events;

import com.google.gson.Gson;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import com.syte.ai.android_sdk.enums.EventsTag;

/**
 * This event should be sent to Syte every time a results for specific bound from the image are shown
 */
public class EventBBShowResults extends BaseSyteEvent {

    @SerializedName("image_url")
    private String mImageUrl;

    @SerializedName("category")
    private String mCategory;

    @SerializedName("results_count")
    private int mResultsCount;

    /**
     * @param imageUrl Url of image used
     * @param category The bound category as returned in the bounds request
     * @param resultsCount The number of returned results in the offers request
     * @param pageName unique page name given by the app developer
     */
    @SerializedName("skus")
    private String mSkus;

    @SerializedName("sku")
    private String mSku;

    @SerializedName("bound_index")
    private Integer mBoundIndex;

    public EventBBShowResults(
            String imageUrl,
            String category,
            int resultsCount,
            String pageName) {
        super("fe_bb_show_results", pageName, EventsTag.CAMERA);
        mCategory = category;
        mImageUrl = imageUrl;
        mResultsCount = resultsCount;
    }

    /**
     * @param skus SKUs of the results shown, in order
     * @param sku SKU of the product whose image was searched, or null for a photo
     * @param boundIndex index of the bound the results belong to
     */
    public EventBBShowResults(
            String imageUrl,
            String category,
            int resultsCount,
            String pageName,
            @Nullable List<String> skus,
            @Nullable String sku,
            @Nullable Integer boundIndex) {
        this(imageUrl, category, resultsCount, pageName);
        // Syte expects the result SKUs as one comma-separated string.
        if (skus != null) {
            StringBuilder joined = new StringBuilder();
            for (String item : skus) {
                if (joined.length() > 0) joined.append(',');
                joined.append(item);
            }
            mSkus = joined.toString();
        }
        mSku = sku;
        mBoundIndex = boundIndex;
    }

    @Override
    public String getRequestBodyString() {
        return new Gson().toJson(this, EventBBShowResults.class);
    }

}
