package com.syte.ai.android_sdk.data.result.discovery;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A page of a merchandising collection's products.
 */
public class CollectionResult {

    private final List<SyteProduct> mProducts;
    @Nullable private final Integer mTotal;
    @Nullable private final String mDisplayTitle;

    public CollectionResult(JsonObject json) {
        // The API answers in one of two layouts depending on response_format.
        JsonObject items = Json.object(json, "result");
        JsonObject carousel = Json.object(json, "results");
        mProducts = SyteProduct.list(items != null ? Json.array(items, "hits") : Json.array(carousel, "products"));
        Double total = Json.number(items != null ? items : carousel, "total");
        mTotal = total == null ? null : total.intValue();
        mDisplayTitle = Json.text(carousel, "displayTitle");
    }

    public List<SyteProduct> getProducts() { return mProducts; }

    @Nullable public Integer getTotal() { return mTotal; }

    /**
     * @return title set for the collection, when the API sends one
     */
    @Nullable public String getDisplayTitle() { return mDisplayTitle; }

}
