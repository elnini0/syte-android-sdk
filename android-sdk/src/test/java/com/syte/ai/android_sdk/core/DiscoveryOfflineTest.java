package com.syte.ai.android_sdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParser;
import com.syte.ai.android_sdk.data.result.discovery.CollectionResult;
import com.syte.ai.android_sdk.data.result.discovery.RecentlyViewedResult;
import com.syte.ai.android_sdk.data.result.discovery.ShoppableGalleryResult;

import org.junit.Test;

import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Request shapes and response parsing for recently viewed, collections and galleries,
 * checked against Syte's API docs. No network.
 */
public class DiscoveryOfflineTest {

    private final DiscoveryService mService =
            RetrofitBuilder.build("https://cdn-api.syteapi.com").create(DiscoveryService.class);

    @Test
    public void recentlyViewedCallShape() {
        Request request = mService.getRecentlyViewed("123", "key", "shopper-1", "fr-FR", "sku,price",
                null, null, Collections.emptyMap()).request();
        assertEquals("GET", request.method());
        assertEquals("/123/v2/recently-viewed", request.url().encodedPath());
        assertEquals("key", request.header("syte-api-key"));
        assertEquals("shopper-1", request.url().queryParameter("user_uuid"));
        assertEquals("fr-FR", request.url().queryParameter("locale"));
        assertNull("these endpoints take the key only as a header", request.url().queryParameter("sig"));
        assertNull(request.url().queryParameter("feed"));
    }

    @Test
    public void removeAndMergeCallShapes() throws Exception {
        Request remove = mService.removeRecentlyViewed("123", "key", "shopper-1", "AB-12").request();
        assertEquals("DELETE", remove.method());
        assertEquals("AB-12", remove.url().queryParameter("sku"));

        Request merge = mService.mergeRecentlyViewed("123", "key",
                RequestBody.create(MediaType.parse("application/json"), "{\"source_uuid\":\"old\",\"target_uuid\":\"new\"}")).request();
        assertEquals("PATCH", merge.method());
        assertEquals("/123/v2/recently-viewed/sync", merge.url().encodedPath());
        Buffer body = new Buffer();
        merge.body().writeTo(body);
        assertEquals("old", JsonParser.parseString(body.readUtf8()).getAsJsonObject().get("source_uuid").getAsString());
    }

    @Test
    public void collectionAndGalleryCallShapes() {
        Request collection = mService.getCollection("123", "key", "summer", "shopper-1", "en-US", 0, 20,
                "sku", "color:blue", null, null, null, Collections.emptyMap()).request();
        assertEquals("/123/v2/collections", collection.url().encodedPath());
        assertEquals("summer", collection.url().queryParameter("collection_id"));
        assertEquals("color:blue", collection.url().queryParameter("filters"));

        Request gallery = mService.getShoppableGallery("123", "64f1c2a9e7b3d4f10a2b5c3d", "key", "en-US", "sku", null).request();
        assertEquals("/123/v2/galleries/shoppable/64f1c2a9e7b3d4f10a2b5c3d", gallery.url().encodedPath());
        assertEquals("key", gallery.header("syte-api-key"));
    }

    @Test
    public void recentlyViewedParsing() {
        RecentlyViewedResult result = new RecentlyViewedResult(JsonParser.parseString(
                "{\"results\":[{\"sku\":\"AB-12\",\"title\":\"Coat\",\"price\":49.99,\"offer\":\"https://x/p/ab-12\"}]," +
                "\"currency\":{\"currency\":\"EUR\",\"symbol\":\"€\",\"rate\":1},\"requestId\":\"r1\",\"feed\":null}").getAsJsonObject());
        assertEquals("AB-12", result.getProducts().get(0).getSku());
        assertEquals("numeric prices come back as text", "49.99", result.getProducts().get(0).getPrice());
        assertEquals("https://x/p/ab-12", result.getProducts().get(0).getUrl());
        assertEquals("€", result.getCurrencySymbol());
    }

    @Test
    public void collectionParsingInBothLayouts() {
        CollectionResult items = new CollectionResult(JsonParser.parseString(
                "{\"result\":{\"hits\":[{\"sku\":\"A\"},{\"sku\":\"B\"}],\"total\":\"42\"},\"message\":null}").getAsJsonObject());
        assertEquals(2, items.getProducts().size());
        assertEquals(Integer.valueOf(42), items.getTotal());

        CollectionResult carousel = new CollectionResult(JsonParser.parseString(
                "{\"results\":{\"products\":[{\"sku\":\"C\"}],\"total\":7,\"displayTitle\":\"Summer\"}}").getAsJsonObject());
        assertEquals("C", carousel.getProducts().get(0).getSku());
        assertEquals(Integer.valueOf(7), carousel.getTotal());
        assertEquals("Summer", carousel.getDisplayTitle());
    }

    @Test
    public void galleryParsing() {
        ShoppableGalleryResult gallery = new ShoppableGalleryResult(JsonParser.parseString(
                "{\"id\":\"g1\",\"layout\":{\"type\":\"grid\"},\"images\":[{\"id\":\"i1\",\"imageUrl\":\"https://x/1.jpg\"," +
                "\"tags\":[{\"sku\":\"AB-12\",\"isAIDetected\":true,\"coordinates\":{\"x\":0.4,\"y\":0.6}}]}]}").getAsJsonObject());
        assertEquals("grid", gallery.getLayoutType());
        ShoppableGalleryResult.Tag tag = gallery.getImages().get(0).getTags().get(0);
        assertEquals("AB-12", tag.getSku());
        assertEquals(0.4, tag.getX(), 0.0001);
        assertTrue(tag.isAIDetected());
    }

}
