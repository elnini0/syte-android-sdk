package com.syte.ai.android_sdk.data.result.discovery;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A product returned by the recently viewed, collection and gallery endpoints.
 * Common fields have getters; everything the API sent is in {@link #getFields()}.
 */
public class SyteProduct {

    private final JsonObject mFields;

    public SyteProduct(JsonObject json) {
        mFields = json;
    }

    static List<SyteProduct> list(JsonArray array) {
        List<SyteProduct> products = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = Json.asObject(element);
            if (json != null) {
                products.add(new SyteProduct(json));
            }
        }
        return products;
    }

    @Nullable public String getSku() { return Json.text(mFields, "sku"); }

    @Nullable public String getTitle() { return Json.text(mFields, "title"); }

    @Nullable public String getPrice() { return Json.text(mFields, "price"); }

    @Nullable public String getOriginalPrice() { return Json.text(mFields, "originalPrice"); }

    @Nullable public String getImageUrl() { return Json.text(mFields, "imageUrl"); }

    /**
     * @return product page URL
     */
    @Nullable public String getUrl() { return Json.text(mFields, "offer"); }

    @Nullable public String getBrand() { return Json.text(mFields, "brand"); }

    /**
     * @return every field the API returned for this product
     */
    public JsonObject getFields() { return mFields; }

}
