package com.syte.ai.android_sdk.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that is used to configure a shoppable gallery request.
 */
public class ShoppableGallery {

    private final String mId;
    private List<String> mFields = new ArrayList<>(DiscoveryFields.DEFAULT);
    @Nullable private String mFeed = null;

    /**
     * @param id gallery ID, as set up in the Syte platform
     */
    public ShoppableGallery(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    /**
     * @param fields product fields to return for tagged products. A small default set is always included.
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

}
