package com.syte.ai.android_sdk.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.syte.ai.android_sdk.SyteCallback;
import com.syte.ai.android_sdk.data.CropCoordinates;
import com.syte.ai.android_sdk.data.ImageSearch;
import com.syte.ai.android_sdk.data.BoughtTogether;
import com.syte.ai.android_sdk.data.Personalization;
import com.syte.ai.android_sdk.data.ShopTheLook;
import com.syte.ai.android_sdk.data.SimilarItems;
import com.syte.ai.android_sdk.data.TextSearch;
import com.syte.ai.android_sdk.data.UrlImageSearch;
import com.syte.ai.android_sdk.data.result.SyteResult;
import com.syte.ai.android_sdk.data.result.account.SytePlatformSettings;
import com.syte.ai.android_sdk.data.result.auto_complete.AutoCompleteResult;
import com.syte.ai.android_sdk.data.result.offers.Bound;
import com.syte.ai.android_sdk.data.result.offers.BoundsResult;
import com.syte.ai.android_sdk.data.result.offers.ItemsResult;
import com.syte.ai.android_sdk.data.result.recommendation.PersonalizationResult;
import com.syte.ai.android_sdk.data.result.recommendation.ShopTheLookResult;
import com.syte.ai.android_sdk.data.result.recommendation.SimilarProductsResult;
import com.syte.ai.android_sdk.data.result.text_search.TextSearchResult;
import com.syte.ai.android_sdk.enums.Catalog;
import com.syte.ai.android_sdk.enums.SyteProductType;
import com.syte.ai.android_sdk.exceptions.SyteGeneralException;
import com.syte.ai.android_sdk.util.SyteLogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

class SyteRemoteDataSource extends BaseRemoteDataSource {

    private static final String TAG = SyteRemoteDataSource.class.getSimpleName();

    private static final String SYTE_URL = "https://cdn-api.syteapi.com";

    private interface BoundsResultCallback {
        void onResult(SyteResult<BoundsResult> result);
    }

    private final SyteService mSyteService;
    private final RecommendationRemoteDataSource mRecommendationRemoteDataSource;
    private final TextSearchRemoteDataSource mTextSearchRemoteDataSource;

    SyteRemoteDataSource(SyteConfiguration configuration) {
        Retrofit retrofit = RetrofitBuilder.build(SYTE_URL);

        mSyteService = retrofit.create(SyteService.class);
        mRecommendationRemoteDataSource = new RecommendationRemoteDataSource(mSyteService, configuration);
        mTextSearchRemoteDataSource = new TextSearchRemoteDataSource(mSyteService, configuration);

        mConfiguration = configuration;
    }

    public void getAutoCompleteAsync(String query, String lang, SyteCallback<AutoCompleteResult> callback) {
        renewTimestamp();
        mTextSearchRemoteDataSource.getAutoCompleteAsync(query, lang, callback);
    }

    SyteResult<TextSearchResult> getTextSearch(TextSearch textSearch) {
        renewTimestamp();
        return mTextSearchRemoteDataSource.getTextSearch(textSearch);
    }

    public void getTextSearchAsync(TextSearch textSearch, SyteCallback<TextSearchResult> callback) {
        renewTimestamp();
        mTextSearchRemoteDataSource.getTextSearchAsync(textSearch, callback);
    }

    SyteResult<List<String>> getPopularSearch(String lang) {
        renewTimestamp();
        return mTextSearchRemoteDataSource.getPopularSearch(lang);
    }

    void getPopularSearchAsync(
            String lang,
            SyteCallback<List<String>> callback
    ) {
        renewTimestamp();
        mTextSearchRemoteDataSource.getPopularSearchAsync(lang, callback);
    }

    SyteResult<SimilarProductsResult> getSimilarProducts(
            SimilarItems similarProducts
    ) {
        renewTimestamp();
        return mRecommendationRemoteDataSource.getSimilarProducts(similarProducts);
    }

    void getSimilarProductsAsync(
            SimilarItems similarProducts,
            SyteCallback<SimilarProductsResult> callback
    ) {
        renewTimestamp();
        mRecommendationRemoteDataSource.getSimilarProductsAsync(
                similarProducts,
                callback
        );
    }

    SyteResult<ShopTheLookResult> getShopTheLook(
            ShopTheLook shopTheLook
    ) {
        renewTimestamp();
        return mRecommendationRemoteDataSource.getShopTheLook(shopTheLook);
    }

    void getShopTheLookAsync(
            ShopTheLook shopTheLook,
            SyteCallback<ShopTheLookResult> callback
    ) {
        renewTimestamp();
        mRecommendationRemoteDataSource.getShopTheLookAsync(
                shopTheLook,
                callback
        );
    }

    SyteResult<PersonalizationResult> getPersonalization(
            Personalization personalization
    ) {
        renewTimestamp();
        return mRecommendationRemoteDataSource.getPersonalization(personalization);
    }

    void getPersonalizationAsync(
            Personalization personalization,
            SyteCallback<PersonalizationResult> callback
    ) {
        renewTimestamp();
        mRecommendationRemoteDataSource.getPersonalizationAsync(
                personalization,
                callback
        );
    }

    SyteResult<PersonalizationResult> getBoughtTogether(BoughtTogether boughtTogether) {
        renewTimestamp();
        return mRecommendationRemoteDataSource.getBoughtTogether(boughtTogether);
    }

    void getBoughtTogetherAsync(
            BoughtTogether boughtTogether,
            SyteCallback<PersonalizationResult> callback
    ) {
        renewTimestamp();
        mRecommendationRemoteDataSource.getBoughtTogetherAsync(boughtTogether, callback);
    }

    SyteResult<SytePlatformSettings> initialize() {
        renewTimestamp();
        Response<ResponseBody> response = null;
        SyteResult<SytePlatformSettings> syteResult = new SyteResult<>();
        try {
            response = mSyteService.initialize(mConfiguration.getAccountId()).execute();
        } catch (IOException e) {
            return handleException(response, e);
        }
        try {
            if (response.body() == null) {
                return handleEmptyBody(response);
            }
            syteResult.data =
                    new Gson().fromJson(response.body().string(), SytePlatformSettings.class);
        } catch (IOException e) {
            return handleException(response, e);
        }
        syteResult.resultCode = response.code();
        syteResult.isSuccessful = response.isSuccessful();
        return syteResult;
    }

    void initializeAsync(SyteCallback<SytePlatformSettings> callback) {
        renewTimestamp();
        mSyteService.initialize(mConfiguration.getAccountId()).enqueue(
                new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        SyteResult<SytePlatformSettings> syteResult = new SyteResult<>();
                        try {
                            if (response.body() == null) {
                                callback.onResult(handleEmptyBody(response));
                            }
                            syteResult.data =
                                    new Gson().fromJson(response.body().string(), SytePlatformSettings.class);
                        } catch (IOException e) {
                            callback.onResult(handleException(response, e));
                        }
                        syteResult.resultCode = response.code();
                        syteResult.isSuccessful = response.isSuccessful();
                        callback.onResult(syteResult);
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        callback.onResult(handleOnFailure(t));
                    }
                }
        );
    }

    private Call<ResponseBody> generateBoundsCall(
            UrlImageSearch requestData
    ) {
        Catalog catalog = requestData.getCatalog();
        String stringCatalog = null;

        if (catalog != null) {
            stringCatalog = catalog.getName().toLowerCase();
        }

        Long sessionId = mConfiguration.getSessionId();
        String stringSessionId = "";
        if (sessionId == -1L) {
            stringSessionId = "OptedOut";
        } else {
            stringSessionId = Long.toString(sessionId);
        }
        return mSyteService.getBounds(
                mConfiguration.getAccountId(),
                mConfiguration.getApiSignature(),
                requestData.getPersonalizedRanking() ? mConfiguration.getUserId() : null,
                requestData.getPersonalizedRanking() ? stringSessionId: null,
                requestData.getProductType().name,
                requestData.getProductType().name,
                mConfiguration.getLocale(),
                stringCatalog,
                requestData.getSku(),
                requestData.getImageUrl(),
                requestData.getPersonalizedRanking() && mConfiguration.isLocalStorageEnabled() ?
                        Utils.viewedProductsString(mConfiguration.getViewedProducts()) : null,
                requestData.getOptions()
        );
    }

    SyteResult<BoundsResult> getBounds(UrlImageSearch requestData) {
        renewTimestamp();
        Response<ResponseBody> response = null;
        try {
            response = generateBoundsCall(requestData).execute();
            return onBoundsResult(requestData,
                    response,
                    requestData.getFirstBoundItemsCoordinates(),
                    true,
                    null
            );
        } catch (IOException e) {
            return handleException(response, e);
        }
    }

    void getBoundsAsync(UrlImageSearch requestData,
                        SyteCallback<BoundsResult> callback) {
        renewTimestamp();
        generateBoundsCall(requestData).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(@NotNull Call<ResponseBody> call,
                                   @NotNull Response<ResponseBody> response) {
                try {
                    onBoundsResult(
                            requestData,
                            response,
                            requestData.getFirstBoundItemsCoordinates(),
                            false,
                            new BoundsResultCallback() {
                                @Override
                                public void onResult(SyteResult<BoundsResult> result) {
                                    callback.onResult(result);
                                }
                            });
                } catch (IOException e) {
                    callback.onResult(handleException(response, e));
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                callback.onResult(handleOnFailure(t));
            }

        });
    }

    SyteResult<ItemsResult> getOffers(
            Bound bound,
            @Nullable CropCoordinates cropCoordinates
    ) {
        renewTimestamp();
        SyteResult<ItemsResult> syteResult = new SyteResult<>();
        Response<ResponseBody> response = null;
        try {
            response = generateOffersCall(
                    bound.getItemUrl(),
                    cropCoordinates
            ).execute();
            syteResult.isSuccessful = response.isSuccessful();
            syteResult.resultCode = response.code();
            syteResult.data = onOffersResult(response);
            if (response.errorBody() != null) {
                syteResult.errorMessage = response.errorBody().string();
            }
            return syteResult;
        } catch (IOException e) {
            return handleException(response, e);
        }
    }

    void getOffersAsync(
            Bound bound,
            CropCoordinates cropCoordinates,
            SyteCallback<ItemsResult> callback) {
        renewTimestamp();
        generateOffersCall(
                bound.getItemUrl(),
                cropCoordinates
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                SyteResult<ItemsResult> syteResult = new SyteResult<>();
                syteResult.isSuccessful = response.isSuccessful();
                syteResult.resultCode = response.code();
                try {
                    syteResult.data = onOffersResult(response);
                    if (response.errorBody() != null) {
                        syteResult.errorMessage = response.errorBody().string();
                    }
                    callback.onResult(syteResult);
                } catch (IOException e) {
                    callback.onResult(handleException(response, e));
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                callback.onResult(handleOnFailure(t));
            }
        });
    }

    private SyteResult<BoundsResult> onBoundsResult(
            UrlImageSearch requestData,
            Response<ResponseBody> response,
            @Nullable CropCoordinates cropCoordinates,
            boolean sync,
            @Nullable BoundsResultCallback resultCallback
    ) throws IOException {
        renewTimestamp();
        SyteResult<BoundsResult> syteResult = new SyteResult<>();
        syteResult.isSuccessful = response.isSuccessful();
        syteResult.resultCode = response.code();

        if (response.body() == null) {
            if (resultCallback != null) {
                resultCallback.onResult(handleEmptyBody(response));
            }
            return handleEmptyBody(response);
        }

        String modifiedResponseString = null;
        try {
            modifiedResponseString = normalizeBoundsJson(response.body().string());
        } catch (IOException | RuntimeException e) {
            if (resultCallback != null) {
                resultCallback.onResult(handleException(response, e));
            }
            return handleException(response, e);
        }

        try {
            syteResult.data = new Gson().fromJson(modifiedResponseString, BoundsResult.class);
        } catch (Exception e) {
            if (resultCallback != null) {
                resultCallback.onResult(handleException(response, e));
            }
            return handleException(response, e);
        }

        if (syteResult.data == null) {
            if (resultCallback != null) {
                resultCallback.onResult(syteResult);
            }
            return syteResult;
        }

        if (syteResult.data.getBounds() == null || syteResult.data.getBounds().size() == 0) {
            if (resultCallback != null) {
                resultCallback.onResult(syteResult);
            }
            return syteResult;
        }

        if (requestData.isRetrieveItemsForTheFirstBound()) {
            if (sync) {
                Response<ResponseBody> offersResponse = null;
                try {
                    offersResponse = generateOffersCall(
                            syteResult.data.getBounds().get(0).getItemUrl(),
                            cropCoordinates
                    ).execute();
                } catch (IOException e) {
                    return handleException(response, e);
                }
                syteResult.data.setFirstBoundItemsResult(onOffersResult(offersResponse));
                return syteResult;
            } else {
                generateOffersCall(
                        syteResult.data.getBounds().get(0).getItemUrl(),
                        cropCoordinates
                ).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        try {
                            syteResult.data.setFirstBoundItemsResult(onOffersResult(response));
                            if (resultCallback != null) {
                                resultCallback.onResult(syteResult);
                            }
                        } catch (IOException e) {
                            if (resultCallback != null) {
                                resultCallback.onResult(handleException(response, e));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        if (resultCallback != null) {
                            resultCallback.onResult(handleOnFailure(t));
                        }
                    }
                });
            }
        } else if (resultCallback != null) {
            resultCallback.onResult(syteResult);
        }
        return syteResult;
    }

    private Call<ResponseBody> generateOffersCall(
            String offersUrl,
            CropCoordinates cropCoordinates
    ) {
        boolean cropEnabled = cropCoordinates != null;
        String actualUrl = null;
        String coordinatesBase64 = null;
        String catalog = null;

        if (cropEnabled) {
            byte[] coordinateStringBytes = cropCoordinates.toString().getBytes();
            Uri uri = Uri.parse(offersUrl);
            final Set<String> params = uri.getQueryParameterNames();
            final Uri.Builder newUri = uri.buildUpon().clearQuery();

            coordinatesBase64 = Base64.encodeToString(coordinateStringBytes, Base64.DEFAULT);

            SyteLogger.i(TAG, "Encoded coordinates: " + coordinatesBase64);

            for (String param : params) {
                if (param.equals("cats")
                        || param.equals("crop")
                        || param.equals("feed")
                ) continue;
                newUri.appendQueryParameter(param, uri.getQueryParameter(param));
            }

            actualUrl = newUri.toString();
        } else {
            actualUrl = offersUrl;
        }

        return mSyteService.getOffers(
                actualUrl,
                coordinatesBase64,
                cropEnabled ? Catalog.GENERAL.getName() : null,
                cropEnabled ? catalog : null
        );
    }

    private ItemsResult onOffersResult(Response<ResponseBody> offersResponse) throws IOException {
        if (offersResponse.isSuccessful()
                && offersResponse.body() != null) {

            String offersResponseString = offersResponse.body().string();
            ItemsResult itemsResult = new Gson().fromJson(
                    offersResponseString,
                    ItemsResult.class
            );

            try {
                JSONObject jsonObject = new JSONObject(offersResponseString);
                JSONArray offersArray = jsonObject.getJSONArray("ads");
                for (int i = 0; i < itemsResult.getItems().size(); i++) {
                    itemsResult.getItems().get(i).setOriginalData(offersArray.getJSONObject(i)
                            .getJSONObject("original_data"));
                }
            } catch (Exception e) {
                handleException(offersResponse, e);
            }

            return itemsResult;
        } else return null;
    }

    public SyteResult<BoundsResult> getBoundsWild(
            Context context,
            ImageSearch requestData
    ) {
        renewTimestamp();
        UrlImageSearch uploadRequest = uploadRequestFor(requestData);
        Response<ResponseBody> response = null;
        try {
            response = generateUploadCall(uploadRequest, prepareImage(context, requestData)).execute();
            return onBoundsResult(uploadRequest,
                    response,
                    uploadRequest.getFirstBoundItemsCoordinates(),
                    true,
                    null
            );
        } catch (Exception e) {
            return handleException(response, e);
        }
    }

    void getBoundsWildAsync(
            Context context,
            ImageSearch requestData,
            SyteCallback<BoundsResult> callback
    ) {
        renewTimestamp();
        UrlImageSearch uploadRequest = uploadRequestFor(requestData);
        byte[] bytes;
        try {
            bytes = prepareImage(context, requestData);
        } catch (Exception e) {
            callback.onResult(handleException(null, e));
            return;
        }
        generateUploadCall(uploadRequest, bytes).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call,
                                   @NotNull Response<ResponseBody> response) {
                try {
                    onBoundsResult(
                            uploadRequest,
                            response,
                            uploadRequest.getFirstBoundItemsCoordinates(),
                            false,
                            callback::onResult);
                } catch (IOException e) {
                    callback.onResult(handleException(response, e));
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                callback.onResult(handleOnFailure(t));
            }
        });
    }

    /**
     * Photo searches go straight to item detection; this carries the photo search's options
     * through the same result handling as image URL searches.
     */
    private UrlImageSearch uploadRequestFor(ImageSearch requestData) {
        UrlImageSearch uploadRequest = new UrlImageSearch("", SyteProductType.CAMERA);
        uploadRequest.setRetrieveItemsForTheFirstBound(requestData.isRetrieveItemsForTheFirstBound());
        uploadRequest.setFirstBoundItemsCoordinates(requestData.getFirstBoundItemsCoordinates());
        uploadRequest.setPersonalizedRanking(requestData.getPersonalizedRanking());
        return uploadRequest;
    }

    private Call<ResponseBody> generateUploadCall(UrlImageSearch requestData, byte[] image) {
        Long sessionId = mConfiguration.getSessionId();
        String stringSessionId = sessionId == -1L ? "OptedOut" : Long.toString(sessionId);
        return mSyteService.uploadImageForBounds(
                mConfiguration.getAccountId(),
                mConfiguration.getApiSignature(),
                requestData.getPersonalizedRanking() ? mConfiguration.getUserId() : null,
                requestData.getPersonalizedRanking() ? stringSessionId : null,
                requestData.getProductType().name,
                requestData.getProductType().name,
                mConfiguration.getLocale(),
                null,
                requestData.getPersonalizedRanking() && mConfiguration.isLocalStorageEnabled() ?
                        Utils.viewedProductsString(mConfiguration.getViewedProducts()) : null,
                "image_bin",
                RequestBody.create(MediaType.parse("application/octet-stream"), image)
        );
    }

    /**
     * Detection responses are keyed by the image's URL, which for uploaded photos isn't known
     * in advance. Re-key the single entry as "bounds" so it maps onto {@link BoundsResult}, and
     * keep the key as "imageUrl": for photos it's where Syte stored the upload.
     */
    static String normalizeBoundsJson(String responseString) {
        JsonObject json = JsonParser.parseString(responseString).getAsJsonObject();
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            normalized.add("bounds", entry.getValue());
            normalized.addProperty("imageUrl", entry.getKey());
            break;
        }
        return normalized.toString();
    }

    @Override
    public void setConfiguration(SyteConfiguration configuration) {
        super.setConfiguration(configuration);
        mRecommendationRemoteDataSource.setConfiguration(configuration);
        mTextSearchRemoteDataSource.setConfiguration(configuration);
    }

    private byte[] prepareImage(
            Context context,
            ImageSearch requestData) throws IOException, SyteGeneralException {

        ImageProcessor imageProcessor = new ImageProcessor();
        Bitmap bitmap = imageProcessor.rotateImageIfNeeded(
                context,
                requestData.getImageUri()
        );
        if (bitmap == null) {
            throw new SyteGeneralException("Could not read the photo.");
        }
        return imageProcessor.compress(bitmap, requestData.getImageScale());
    }

}
