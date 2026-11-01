package com.syte.ai.android_sdk.events;

import com.google.gson.Gson;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.syte.ai.android_sdk.enums.EventsTag;

import java.util.Arrays;
import java.util.List;

/**
 * This event should be sent to Syte every time a user clicks on product in Camera results
 */
public class EventOfferClick extends BaseSyteEvent {

    @SerializedName("sku")
    private final String mSku;

    @SerializedName("position")
    private final int mPosition;

    /**
     * @param sku Unique identifier of product sent to Syte before
     * @param position index of the result in the results list
     * @param pageName unique page name given by the app developer
     */
    @SerializedName("source_sku")
    private final String mSourceSku;

    @SerializedName("target_url")
    private final String mTargetUrl;

    public EventOfferClick(
            String sku,
            int position,
            String pageName) {
        this(sku, position, pageName, null, null);
    }

    /**
     * @param sourceSku SKU of the product the results were found for, if any. Syte matches the
     *                  click to the results event that carried the same SKU.
     * @param targetUrl link the shopper was taken to
     */
    public EventOfferClick(
            String sku,
            int position,
            String pageName,
            @Nullable String sourceSku,
            @Nullable String targetUrl) {
        super("fe_offer_click", pageName, Arrays.asList(EventsTag.DISCOVERY_BUTTON, EventsTag.CAMERA));
        mPosition = position;
        mSku = sku;
        mSourceSku = sourceSku;
        mTargetUrl = targetUrl;
    }

    @Override
    public String getRequestBodyString() {
        return new Gson().toJson(this, EventOfferClick.class);
    }

}
