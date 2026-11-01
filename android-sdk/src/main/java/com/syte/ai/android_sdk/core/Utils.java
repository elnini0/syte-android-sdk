package com.syte.ai.android_sdk.core;

import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;

import com.syte.ai.android_sdk.data.result.account.SytePlatformSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Utils {

    public static String viewedProductsString(Set<String> viewedProducts) {
        if (viewedProducts.isEmpty()) {
            return null;
        }
        return TextUtils.join(",", viewedProducts);
    }

    static String textSearchTermsString(List<String> terms) {
        if (terms.isEmpty()) {
            return null;
        }

        return TextUtils.join(",", terms);
    }

    static String viewedProductsJSONArray(Set<String> viewedProducts) {
        return skusJSONArray(new ArrayList<>(viewedProducts));
    }

    static String viewedProductsJSONArray(String sku) {
        if (sku == null) {
            return null;
        }
        return skusJSONArray(Collections.singletonList(sku));
    }

    static String skusJSONArray(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return null;
        }
        return new Gson().toJson(skus);
    }

    static String generateFiltersString(Map<String, List<String>> filters) {
        if (filters.isEmpty()) {
            return null;
        }
        return new Gson().toJson(filters);
    }

}
