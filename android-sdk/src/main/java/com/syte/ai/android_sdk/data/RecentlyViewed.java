package com.syte.ai.android_sdk.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that is used to configure the recently viewed request. The list belongs to the SDK's shopper ID.
 */
public class RecentlyViewed {

    private List<String> mFields = new ArrayList<>(DiscoveryFields.DEFAULT);
    @Nullable private String mFeed = null;
    @Nullable private String mForceCurrency = null;
    private final Map<String, String> mOptions = new HashMap<>();

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
     * @param currency shows prices in this currency instead of the account's default, e.g. "USD"
     */
    public void setForceCurrency(@Nullable String currency) {
        mForceCurrency = currency;
    }

    @Nullable
    public String getForceCurrency() {
        return mForceCurrency;
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
