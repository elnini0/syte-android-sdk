package com.syte.ai.android_sdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.syte.ai.android_sdk.SyteCallback;
import com.syte.ai.android_sdk.data.BoughtTogether;
import com.syte.ai.android_sdk.data.ImageSearch;
import com.syte.ai.android_sdk.data.Personalization;
import com.syte.ai.android_sdk.data.ShopTheLook;
import com.syte.ai.android_sdk.data.SimilarItems;
import com.syte.ai.android_sdk.data.TextSearch;
import com.syte.ai.android_sdk.data.UrlImageSearch;
import com.syte.ai.android_sdk.data.result.SyteResult;
import com.syte.ai.android_sdk.data.result.auto_complete.AutoCompleteResult;
import com.syte.ai.android_sdk.data.result.offers.BoundsResult;
import com.syte.ai.android_sdk.data.result.text_search.HitsItem;
import com.syte.ai.android_sdk.data.result.text_search.TextSearchResult;
import com.syte.ai.android_sdk.enums.SyteProductType;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Calls the live API. Skipped unless an account is passed as instrumentation arguments:
 * ./gradlew :android-sdk:connectedAndroidTest
 *     -Pandroid.testInstrumentationRunnerArguments.syteAccountId=...
 *     -Pandroid.testInstrumentationRunnerArguments.syteApiKey=...
 * Optional: syteTestQuery and syteTestLang (defaults "robe" and "fr_FR").
 */
public class LiveApiTest {

    private Syte mSyte;
    private String mQuery;
    private String mLang;

    @Before
    public void setUp() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        String account = args.getString("syteAccountId");
        String key = args.getString("syteApiKey");
        Assume.assumeTrue("Pass syteAccountId and syteApiKey to run live tests", account != null && key != null);
        mQuery = args.getString("syteTestQuery", "robe");
        mLang = args.getString("syteTestLang", "fr_FR");
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SyteConfiguration configuration = new SyteConfiguration(context, account, key);
        configuration.setLocale(mLang);
        mSyte = Syte.newInstance(configuration);
    }

    private static <T> T ok(SyteResult<T> result) {
        assertTrue("HTTP " + result.resultCode + ": " + result.errorMessage, result.isSuccessful);
        assertNotNull(result.data);
        return result.data;
    }

    private HitsItem firstHit() {
        List<HitsItem> hits = ok(mSyte.getTextSearch(new TextSearch(mQuery, mLang))).getResult().getHits();
        assertFalse("no search hits for " + mQuery, hits.isEmpty());
        return hits.get(0);
    }

    private SyteResult<AutoCompleteResult> autocomplete(String query) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<SyteResult<AutoCompleteResult>> result = new AtomicReference<>();
        SyteCallback<AutoCompleteResult> callback = r -> { result.set(r); done.countDown(); };
        mSyte.getAutoCompleteForTextSearch(query, mLang, callback);
        assertTrue("autocomplete timed out", done.await(60, TimeUnit.SECONDS));
        return result.get();
    }

    @Test
    public void platformSettings() {
        assertNotNull(ok(mSyte.getPlatformSettings()).getData());
    }

    @Test
    public void textSearchWithFilters() {
        TextSearchResult plain = ok(mSyte.getTextSearch(new TextSearch(mQuery, mLang)));
        assertNotNull(plain.getResult().getHits().get(0).getSku());
        TextSearch filtered = new TextSearch(mQuery, mLang);
        filtered.addFilters("size", Collections.singletonList("L"));
        TextSearchResult narrowed = ok(mSyte.getTextSearch(filtered));
        assertTrue(narrowed.getResult().getTotal() <= plain.getResult().getTotal());
    }

    @Test
    public void autocompleteAndPopularSearches() throws InterruptedException {
        ok(autocomplete(mQuery));
        ok(autocomplete("zqxjv"));
        ok(mSyte.getPopularSearches(mLang));
    }

    @Test
    public void visualSearchFromImageUrl() {
        HitsItem hit = firstHit();
        UrlImageSearch request = new UrlImageSearch(hit.getImageUrl(), SyteProductType.DISCOVERY_BUTTON);
        request.setSku(hit.getSku());
        BoundsResult bounds = ok(mSyte.getBoundsForImageUrl(request));
        assertFalse(bounds.getBounds().isEmpty());
        assertFalse(bounds.getItemsForFirstBound().getItems().isEmpty());
    }

    @Test
    public void visualSearchFromPhoto() throws Exception {
        HitsItem hit = firstHit();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File photo = new File(context.getCacheDir(), "live-test.jpg");
        try (InputStream in = new URL(hit.getImageUrl()).openStream(); OutputStream out = new FileOutputStream(photo)) {
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
        }
        BoundsResult bounds = ok(mSyte.getBoundsForImage(context, new ImageSearch(photo)));
        assertFalse(bounds.getBounds().isEmpty());
        assertFalse(bounds.getItemsForFirstBound().getItems().isEmpty());
    }

    @Test
    public void recommendations() throws Exception {
        HitsItem hit = firstHit();
        assertFalse(ok(mSyte.getSimilarProducts(new SimilarItems(hit.getSku(), hit.getImageUrl()))).getItems().isEmpty());
        assertNotNull(ok(mSyte.getShopTheLook(new ShopTheLook(hit.getSku(), hit.getImageUrl()))).getItems());
        assertFalse(ok(mSyte.getBoughtTogether(new BoughtTogether(Collections.singletonList(hit.getSku())))).getItems().isEmpty());
        mSyte.addViewedProduct(hit.getSku());
        assertFalse(ok(mSyte.getPersonalizedProducts(new Personalization())).getItems().isEmpty());
    }

    @Test
    public void shopperIdSurvivesNewInstance() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String first = new SyteConfiguration(context, "1", "key").getUserId();
        String second = new SyteConfiguration(context, "1", "key").getUserId();
        assertNotNull(first);
        assertNotEquals("", first);
        assertEquals(first, second);
    }

}
