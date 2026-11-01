package com.syte.ai.android_sdk.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that is used to configure a merchandising collection request.
 */
public class MerchandisingCollection {

    private final String mId;
    private int mFrom = 0;
    private int mSize = 20;
    @Nullable private String mFilters = null;
    @Nullable private String mSku = null;
    private List<String> mFields = new ArrayList<>(DiscoveryFields.DEFAULT);
    @Nullable private String mFeed = null;
    private final Map<String, String> mOptions = new HashMap<>();

    /**
     * @param id collection ID, as set up in the Syte platform
     */
    public MerchandisingCollection(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    /**
     * @param from pagination offset
     */
    public void setFrom(int from) {
        mFrom = from;
    }

    public int getFrom() {
        return mFrom;
    }

    /**
     * @param size page size
     */
    public void setSize(int size) {
        mSize = size;
    }

    public int getSize() {
        return mSize;
    }

    /**
     * @param filters server-side filter expression, e.g. "color:blue"
     */
    public void setFilters(@Nullable String filters) {
        mFilters = filters;
    }

    @Nullable
    public String getFilters() {
        return mFilters;
    }

    /**
     * @param sku restricts or biases the collection to this product
     */
    public void setSku(@Nullable String sku) {
        mSku = sku;
    }

    @Nullable
    public String getSku() {
        return mSku;
    }

    /**
     * @param fields product fields to return. Defaults to {@link DiscoveryFields#DEFAULT}.
     */
    public void setFields(List<String> fields) {
        mFields = new ArrayList<>(fields);
    }

    public List<String> getFields() {
        return mFields;
    }

    /**
     * @param feed overrides the account's default feed
     */
    public void setFeed(@Nullable String feed) {
        mFeed = feed;
    }

    @Nullable
    public String getFeed() {
        return mFeed;
    }

    /**
     * Add additional query parameters to request.
     * @param key query name
     * @param value query value
     */
    public void addOption(String key, String value) {
        mOptions.put(key, value);
    }

    public Map<String, String> getOptions() {
        return mOptions;
    }

}
