package com.syte.ai.android_sdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.syte.ai.android_sdk.events.BaseSyteEvent;
import com.syte.ai.android_sdk.events.EventCheckoutComplete;
import com.syte.ai.android_sdk.events.EventCheckoutStart;
import com.syte.ai.android_sdk.events.EventProductsAddedToCart;
import com.syte.ai.android_sdk.events.Product;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

/**
 * Offline checks for request shapes. No network, no device.
 */
public class OfflineTest {

    private static JsonObject rawData(BaseSyteEvent event) {
        JsonObject outer = JsonParser.parseString(event.getRequestBodyString()).getAsJsonObject();
        assertEquals("Ecommerce events send only rawData in the body", 1, outer.size());
        return JsonParser.parseString(outer.get("rawData").getAsString()).getAsJsonObject();
    }

    private static List<Product> products(double price) {
        return Collections.singletonList(new Product("ABC123", 1, price));
    }

    @Test
    public void cartEventSendsProductsInsideRawData() {
        JsonObject payload = rawData(new EventProductsAddedToCart(products(49.99), "cart"));
        JsonObject product = payload.getAsJsonArray("products").get(0).getAsJsonObject();
        assertEquals("ABC123", product.get("sku").getAsString());
        assertEquals(1, product.get("quantity").getAsInt());
        assertEquals(49.99, product.get("price").getAsDouble(), 0.0001);
    }

    @Test
    public void checkoutStartSendsTotalsAsStrings() {
        JsonObject payload = rawData(new EventCheckoutStart(62.94, "EUR", products(59.99), "checkout"));
        assertEquals("62.94", payload.get("value").getAsString());
        assertEquals("EUR", payload.get("currency").getAsString());
        JsonArray items = payload.getAsJsonArray("products");
        assertEquals("59.99", items.get(0).getAsJsonObject().get("price").getAsJsonPrimitive().getAsString());
    }

    @Test
    public void purchaseCarriesOrderIdInsideRawData() {
        JsonObject payload = rawData(new EventCheckoutComplete("ORDER-1001", 23.79, "EUR", products(19.99), "thank-you"));
        assertEquals("ORDER-1001", payload.get("id").getAsString());
        assertEquals("23.79", payload.get("value").getAsString());
    }

    @Test
    public void searchFiltersAreValidJson() {
        assertNull(Utils.generateFiltersString(new HashMap<>()));
        Map<String, List<String>> filters = new LinkedHashMap<>();
        filters.put("size", Arrays.asList("L", "M"));
        filters.put("color", Collections.singletonList("say \"red\""));
        assertEquals("{\"size\":[\"L\",\"M\"],\"color\":[\"say \\\"red\\\"\"]}", Utils.generateFiltersString(filters));
    }

    @Test
    public void skuListsAreValidJson() {
        // Unquoted SKUs like [AB-12] made the personalization API answer 400.
        assertEquals("[\"AB-12\"]", Utils.viewedProductsJSONArray("AB-12"));
        assertEquals("[\"AB-12\",\"CD-34\"]", Utils.skusJSONArray(Arrays.asList("AB-12", "CD-34")));
        assertNull(Utils.skusJSONArray(Collections.emptyList()));
    }

    @Test
    public void uploadedPhotoResponseIsRekeyed() {
        // Uploaded photos come back keyed by a storage URL the SDK can't know in advance.
        String response = "{\"https://uploads.example.com/abc_1\":[{\"label\":\"Coats\",\"offers\":\"https://x/offers\"}]}";
        JsonObject normalized = JsonParser.parseString(SyteRemoteDataSource.normalizeBoundsJson(response)).getAsJsonObject();
        assertEquals("Coats", normalized.getAsJsonArray("bounds").get(0).getAsJsonObject().get("label").getAsString());
    }

    @Test
    public void recommendationAndEventCallsGetKeyHeader() {
        assertEquals("key", RetrofitBuilder.apiKeyHeaderFor(HttpUrl.get("https://cdn-api.syteapi.com/v1.1/similars?account_id=1&sig=key")));
        assertEquals("key", RetrofitBuilder.apiKeyHeaderFor(HttpUrl.get("https://cdn-api.syteapi.com/v1.1/offers/bb?sig=key")));
        assertEquals("key", RetrofitBuilder.apiKeyHeaderFor(HttpUrl.get("https://syteapi.com/et?sig=key")));
    }

    @Test
    public void textSearchCallsDoNotGetKeyHeader() {
        // The text search endpoints reject x-api-key with a 403.
        assertNull(RetrofitBuilder.apiKeyHeaderFor(HttpUrl.get("https://cdn-api.syteapi.com/search/1/items?sig=key")));
        assertNull(RetrofitBuilder.apiKeyHeaderFor(HttpUrl.get("https://cdn-api.syteapi.com/accounts/1")));
    }

}
