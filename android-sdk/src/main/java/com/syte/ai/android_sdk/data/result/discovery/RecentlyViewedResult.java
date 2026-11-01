package com.syte.ai.android_sdk.data.result.discovery;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * The shopper's recently viewed products, most recent first.
 */
public class RecentlyViewedResult {

    private final List<SyteProduct> mProducts;
    @Nullable private final String mCurrency;
    @Nullable private final String mCurrencySymbol;

    public RecentlyViewedResult(JsonObject json) {
        mProducts = SyteProduct.list(Json.array(json, "results"));
        JsonObject currency = Json.object(json, "currency");
        mCurrency = Json.text(currency, "currency");
        mCurrencySymbol = Json.text(currency, "symbol");
    }

    public List<SyteProduct> getProducts() { return mProducts; }

    /**
     * @return currency code the prices are in, e.g. "EUR"
     */
    @Nullable public String getCurrency() { return mCurrency; }

    @Nullable public String getCurrencySymbol() { return mCurrencySymbol; }

}
