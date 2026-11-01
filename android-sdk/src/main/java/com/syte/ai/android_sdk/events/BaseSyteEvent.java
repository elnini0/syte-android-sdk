package com.syte.ai.android_sdk.events;

import com.google.gson.Gson;
import com.syte.ai.android_sdk.enums.EventsTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class represents Base Syte event. Extend this class and override {@link #getRequestBodyString()}
 * to create a custom event. Example can be found in the sample app.
 */
abstract public class BaseSyteEvent {

    protected transient final String mSyteUrlReferer;
    protected transient final String mName;
    protected transient final List<String> mEventsTags = new ArrayList<>();

    /**
     * @param name - event name
     * @param syteUrlReferer - Syte URL referer
     * @param tag - event tag {@link EventsTag}
     */
    public BaseSyteEvent(String name, String syteUrlReferer, String tag) {
        mName = name;
        mSyteUrlReferer = syteUrlReferer;
        mEventsTags.add(tag);
    }

    /**
     * @param name - event name
     * @param syteUrlReferer - Syte URL referer
     * @param tags - list of event tags
     */
    public BaseSyteEvent(String name, String syteUrlReferer, List<String> tags) {
        mName = name;
        mSyteUrlReferer = syteUrlReferer;
        mEventsTags.addAll(tags);
    }

    /**
     * Getter for request body string
     * @return request body string
     */
    public abstract String getRequestBodyString();

    /**
     * Ecommerce events carry their data as a JSON string in rawData; Syte ignores it anywhere else.
     */
    protected static String rawDataBody(Map<String, Object> payload) {
        Gson gson = new Gson();
        return gson.toJson(Collections.singletonMap("rawData", gson.toJson(payload)));
    }

    /**
     * Money amounts as Syte's event payloads expect them, e.g. "62.94".
     */
    protected static String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    protected static List<Map<String, Object>> productsPayload(List<Product> products, boolean priceAsString) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Product product : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sku", product.getSku());
            item.put("quantity", product.getQuantity());
            item.put("price", priceAsString ? formatAmount(product.getPrice()) : product.getPrice());
            list.add(item);
        }
        return list;
    }

    /**
     * Getter for event name
     * @return event name
     */
    public String getName() {
        return mName;
    }

    /**
     * Getter for list of tags
     * @return list of tags
     */
    public List<String> getTags() {
        return mEventsTags;
    }

    /**
     * Get list of tags converted to string (comma separated)
     * @return list of tags converted to string
     */
    public String getTagsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(String tag : mEventsTags) {
            stringBuilder.append(tag);
            stringBuilder.append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    /**
     * Getter for Syte Url referer
     * @return Syte Url referer
     */
    public String getSyteUrlReferer() {
        return mSyteUrlReferer;
    }

}
