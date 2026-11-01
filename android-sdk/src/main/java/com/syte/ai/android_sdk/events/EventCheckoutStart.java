package com.syte.ai.android_sdk.events;

import com.google.gson.annotations.SerializedName;
import com.syte.ai.android_sdk.enums.EventsTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This event should be sent to Syte every time a user starts a checkout process
 */
public class EventCheckoutStart extends BaseSyteEvent {

    @SerializedName("products")
    private final List<Product> mProducts;

    @SerializedName("value")
    private final double mValue;

    @SerializedName("currency")
    private final String mCurrency;

    /**
     * @param price total cost of a cart for checkout including delivery fee
     * @param currency currency of checkout
     * @param productList array of product objects
     * @param pageName unique page name given by the app developer
     */
    public EventCheckoutStart(
            double price,
            String currency,
            List<Product> productList,
            String pageName) {
        super("checkout_start", pageName, EventsTag.ECOMMERCE);
        mProducts = productList;
        mValue = price;
        mCurrency = currency;
    }

    @Override
    public String getRequestBodyString() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("value", formatAmount(mValue));
        payload.put("currency", mCurrency);
        payload.put("products", productsPayload(mProducts, true));
        return rawDataBody(payload);
    }

}
