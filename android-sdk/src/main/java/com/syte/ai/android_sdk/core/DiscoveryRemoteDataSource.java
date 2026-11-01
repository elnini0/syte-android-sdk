package com.syte.ai.android_sdk.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.syte.ai.android_sdk.SyteCallback;
import com.syte.ai.android_sdk.data.MerchandisingCollection;
import com.syte.ai.android_sdk.data.RecentlyViewed;
import com.syte.ai.android_sdk.data.ShoppableGallery;
import com.syte.ai.android_sdk.data.result.SyteResult;
import com.syte.ai.android_sdk.data.result.discovery.CollectionResult;
import com.syte.ai.android_sdk.data.result.discovery.RecentlyViewedResult;
import com.syte.ai.android_sdk.data.result.discovery.ShoppableGalleryResult;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class DiscoveryRemoteDataSource extends BaseRemoteDataSource {

    private static final String SYTE_URL = "https://cdn-api.syteapi.com";

    private interface ResultBuilder<T> {
        T build(JsonObject json);
    }

    private final DiscoveryService mService;

    DiscoveryRemoteDataSource(SyteConfiguration configuration) {
        mService = RetrofitBuilder.build(SYTE_URL).create(DiscoveryService.class);
        mConfiguration = configuration;
    }

    /**
     * These endpoints take BCP-47 locales ("en-US"); the SDK's locale uses an underscore.
     */
    private String locale() {
        return mConfiguration.getLocale().replace('_', '-');
    }

    Call<ResponseBody> recentlyViewedCall(RecentlyViewed request) {
        return mService.getRecentlyViewed(mConfiguration.getAccountId(), mConfiguration.getApiSignature(),
                mConfiguration.getUserId(), locale(), TextUtils.join(",", request.getFields()),
                request.getFeed(), request.getForceCurrency(), request.getOptions());
    }

    Call<ResponseBody> removeRecentlyViewedCall(String sku) {
        return mService.removeRecentlyViewed(mConfiguration.getAccountId(), mConfiguration.getApiSignature(),
                mConfiguration.getUserId(), sku);
    }

    Call<ResponseBody> mergeRecentlyViewedCall(String fromUserId) {
        JsonObject body = new JsonObject();
        body.addProperty("source_uuid", fromUserId);
        body.addProperty("target_uuid", mConfiguration.getUserId());
        return mService.mergeRecentlyViewed(mConfiguration.getAccountId(), mConfiguration.getApiSignature(),
                RequestBody.create(MediaType.parse("application/json"), body.toString()));
    }

    Call<ResponseBody> collectionCall(MerchandisingCollection request) {
        return mService.getCollection(mConfiguration.getAccountId(), mConfiguration.getApiSignature(),
                request.getId(), mConfiguration.getUserId(), locale(), request.getFrom(), request.getSize(),
                TextUtils.join(",", request.getFields()), request.getFilters(), request.getSku(), request.getFeed(),
                mConfiguration.isLocalStorageEnabled() ? Utils.viewedProductsString(mConfiguration.getViewedProducts()) : null,
                request.getOptions());
    }

    Call<ResponseBody> shoppableGalleryCall(ShoppableGallery request) {
        return mService.getShoppableGallery(mConfiguration.getAccountId(), request.getId(),
                mConfiguration.getApiSignature(), locale(), TextUtils.join(",", request.getFields()), request.getFeed());
    }

    SyteResult<RecentlyViewedResult> getRecentlyViewed(RecentlyViewed request) {
        return execute(recentlyViewedCall(request), RecentlyViewedResult::new);
    }

    void getRecentlyViewedAsync(RecentlyViewed request, SyteCallback<RecentlyViewedResult> callback) {
        enqueue(recentlyViewedCall(request), RecentlyViewedResult::new, callback);
    }

    SyteResult<Boolean> removeRecentlyViewed(String sku) {
        return execute(removeRecentlyViewedCall(sku), json -> true);
    }

    void removeRecentlyViewedAsync(String sku, SyteCallback<Boolean> callback) {
        enqueue(removeRecentlyViewedCall(sku), json -> true, callback);
    }

    SyteResult<Boolean> mergeRecentlyViewed(String fromUserId) {
        return execute(mergeRecentlyViewedCall(fromUserId), json -> true);
    }

    void mergeRecentlyViewedAsync(String fromUserId, SyteCallback<Boolean> callback) {
        enqueue(mergeRecentlyViewedCall(fromUserId), json -> true, callback);
    }

    SyteResult<CollectionResult> getCollection(MerchandisingCollection request) {
        return execute(collectionCall(request), CollectionResult::new);
    }

    void getCollectionAsync(MerchandisingCollection request, SyteCallback<CollectionResult> callback) {
        enqueue(collectionCall(request), CollectionResult::new, callback);
    }

    SyteResult<ShoppableGalleryResult> getShoppableGallery(ShoppableGallery request) {
        return execute(shoppableGalleryCall(request), ShoppableGalleryResult::new);
    }

    void getShoppableGalleryAsync(ShoppableGallery request, SyteCallback<ShoppableGalleryResult> callback) {
        enqueue(shoppableGalleryCall(request), ShoppableGalleryResult::new, callback);
    }

    private <T> SyteResult<T> execute(Call<ResponseBody> call, ResultBuilder<T> builder) {
        renewTimestamp();
        Response<ResponseBody> response = null;
        try {
            response = call.execute();
            return toResult(response, builder);
        } catch (IOException | RuntimeException e) {
            return handleException(response, e);
        }
    }

    private <T> void enqueue(Call<ResponseBody> call, ResultBuilder<T> builder, SyteCallback<T> callback) {
        renewTimestamp();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                try {
                    callback.onResult(toResult(response, builder));
                } catch (IOException | RuntimeException e) {
                    callback.onResult(handleException(response, e));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                callback.onResult(handleOnFailure(t));
            }
        });
    }

    private <T> SyteResult<T> toResult(Response<ResponseBody> response, ResultBuilder<T> builder) throws IOException {
        SyteResult<T> result = new SyteResult<>();
        result.resultCode = response.code();
        result.isSuccessful = response.isSuccessful();
        if (!response.isSuccessful() || response.body() == null) {
            result.isSuccessful = false;
            result.errorMessage = response.errorBody() != null ? response.errorBody().string() : "Result body is null";
            return result;
        }
        String body = response.body().string();
        JsonObject json = body.isEmpty() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
        result.data = builder.build(json);
        return result;
    }

}
