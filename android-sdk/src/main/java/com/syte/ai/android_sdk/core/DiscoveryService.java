package com.syte.ai.android_sdk.core;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Recently viewed, collections and galleries. These endpoints take the API key only as a
 * syte-api-key header, and the account ID in the path.
 */
interface DiscoveryService {

    String API_KEY_HEADER = "syte-api-key";

    @GET("{accountId}/v2/recently-viewed")
    Call<ResponseBody> getRecentlyViewed(@Path("accountId") String accountId,
                                         @Header(API_KEY_HEADER) String apiKey,
                                         @Query("user_uuid") String userId,
                                         @Query("locale") String locale,
                                         @Query("fields") String fields,
                                         @Query("feed") String feed,
                                         @Query("force_currency") String forceCurrency,
                                         @QueryMap Map<String, String> options);

    @DELETE("{accountId}/v2/recently-viewed")
    Call<ResponseBody> removeRecentlyViewed(@Path("accountId") String accountId,
                                            @Header(API_KEY_HEADER) String apiKey,
                                            @Query("user_uuid") String userId,
                                            @Query("sku") String sku);

    @PATCH("{accountId}/v2/recently-viewed/sync")
    Call<ResponseBody> mergeRecentlyViewed(@Path("accountId") String accountId,
                                           @Header(API_KEY_HEADER) String apiKey,
                                           @Body RequestBody body);

    // The docs' error for this call names the ID parameter collection_id.
    @GET("{accountId}/v2/collections")
    Call<ResponseBody> getCollection(@Path("accountId") String accountId,
                                     @Header(API_KEY_HEADER) String apiKey,
                                     @Query("collection_id") String collectionId,
                                     @Query("user_uuid") String userId,
                                     @Query("locale") String locale,
                                     @Query("from") int from,
                                     @Query("size") int size,
                                     @Query("fields") String fields,
                                     @Query("filters") String filters,
                                     @Query("sku") String sku,
                                     @Query("feed") String feed,
                                     @Query("session_skus") String sessionSkus,
                                     @QueryMap Map<String, String> options);

    @GET("{accountId}/v2/galleries/shoppable/{galleryId}")
    Call<ResponseBody> getShoppableGallery(@Path("accountId") String accountId,
                                           @Path("galleryId") String galleryId,
                                           @Header(API_KEY_HEADER) String apiKey,
                                           @Query("locale") String locale,
                                           @Query("fields") String fields,
                                           @Query("feed") String feed);

}
