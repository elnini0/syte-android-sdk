package com.syte.ai.android_sdk.data;

import com.syte.ai.android_sdk.enums.RecommendationReturnField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that is used to configure "frequently bought together" requests.
 * Pass the products in the shopper's cart, or the product they are looking at.
 */
public class BoughtTogether {

    private final List<String> mSkus;
    private String mSyteUrlReferer = "mobile_sdk";
    private int mLimit = 7;
    private String mModelVersion = "A";
    private RecommendationReturnField mFieldsToReturn = RecommendationReturnField.ALL;
    private final Map<String, String> mOptions = new HashMap<>();

    /**
     * @param skus products to find frequently-bought-together items for
     */
    public BoughtTogether(List<String> skus) {
        mSkus = skus == null ? new ArrayList<>() : new ArrayList<>(skus);
    }

    public List<String> getSkus() {
        return mSkus;
    }

    /**
     * Configure what fields must be returned in response. All fields will be returned by default.
     * In case the value is changed, the result will only contain the chosen fields. All other ones will be null!
     * @param fieldsToReturn - {@link RecommendationReturnField}
     */
    public void setFieldsToReturn(RecommendationReturnField fieldsToReturn) {
        mFieldsToReturn = fieldsToReturn;
    }

    public RecommendationReturnField getFieldsToReturn() {
        return mFieldsToReturn;
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

    public String getSyteUrlReferer() {
        return mSyteUrlReferer;
    }

    /**
     * @param syteUrlReferer page name
     */
    public void setSyteUrlReferer(String syteUrlReferer) {
        mSyteUrlReferer = syteUrlReferer;
    }

    public int getLimit() {
        return mLimit;
    }

    /**
     * @param limit number of results to return
     */
    public void setLimit(int limit) {
        mLimit = limit;
    }

    public String getModelVersion() {
        return mModelVersion;
    }

    /**
     * @param modelVersion model version
     */
    public void setModelVersion(String modelVersion) {
        mModelVersion = modelVersion;
    }

}
