package com.syte.ai.android_sdk.core;

import androidx.annotation.Nullable;

import com.syte.ai.android_sdk.util.SyteLogger;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

class RetrofitBuilder {

    static final String API_KEY_HEADER = "x-api-key";

    /**
     * Visual search, recommendation and event calls take the API key as a header, per Syte's docs.
     * Text search endpoints reject that header with a 403, so they keep only the sig parameter.
     */
    @Nullable
    static String apiKeyHeaderFor(HttpUrl url) {
        String path = url.encodedPath();
        if (path.startsWith("/v1.1/") || path.equals("/et") || path.equals("/eet")) {
            return url.queryParameter("sig");
        }
        return null;
    }

    static Retrofit build(String url) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (SyteLogger.getLogLevel().ordinal() < SyteLogger.LogLevel.WARN.ordinal()) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @NotNull
                    @Override
                    public Response intercept(@NotNull Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request.Builder builder = originalRequest.newBuilder()
                                .header("User-Agent", System.getProperty("http.agent") + " syte_android_sdk");
                        String apiKey = apiKeyHeaderFor(originalRequest.url());
                        if (apiKey != null) {
                            builder.header(API_KEY_HEADER, apiKey);
                        }
                        return chain.proceed(builder.build());
                    }
                })
                .addInterceptor(interceptor)
                .build();
        return new Retrofit
                .Builder()
                .baseUrl(url)
                .client(client)
                .build();
    }

}
