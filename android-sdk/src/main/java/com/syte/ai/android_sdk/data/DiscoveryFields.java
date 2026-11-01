package com.syte.ai.android_sdk.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Product fields requested from the recently viewed, collection and gallery endpoints unless you choose others.
 */
public final class DiscoveryFields {

    public static final List<String> DEFAULT = Collections.unmodifiableList(
            Arrays.asList("sku", "title", "price", "originalPrice", "imageUrl", "offer", "brand"));

    private DiscoveryFields() {}

}
