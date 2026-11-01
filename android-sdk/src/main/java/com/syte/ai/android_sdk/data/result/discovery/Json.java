package com.syte.ai.android_sdk.data.result.discovery;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Lenient readers: the API sends some fields as numbers on one account and strings on another.
 */
final class Json {

    private Json() {}

    @Nullable
    static String text(@Nullable JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    @Nullable
    static Double number(@Nullable JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return null;
        }
        try {
            return json.get(key).getAsDouble();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    static JsonObject object(@Nullable JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonObject()) {
            return null;
        }
        return json.getAsJsonObject(key);
    }

    static JsonArray array(@Nullable JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return json.getAsJsonArray(key);
    }

    @Nullable
    static JsonObject asObject(JsonElement element) {
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

}
