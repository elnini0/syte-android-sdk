package com.syte.ai.android_sdk.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.syte.ai.android_sdk.data.MerchandisingCollection;
import com.syte.ai.android_sdk.data.RecentlyViewed;
import com.syte.ai.android_sdk.data.ShoppableGallery;
import com.syte.ai.android_sdk.data.result.SyteResult;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Live calls for recently viewed, collections and galleries. These endpoints are enabled per account,
 * so the tests only run with syteTestDiscovery=true alongside the account arguments (see LiveApiTest).
 * Collection and gallery checks also need syteTestCollectionId and syteTestGalleryId.
 */
public class DiscoveryLiveTest {

    private Syte mSyte;
    private Bundle mArgs;

    @Before
    public void setUp() throws Exception {
        mArgs = InstrumentationRegistry.getArguments();
        String account = mArgs.getString("syteAccountId");
        String key = mArgs.getString("syteApiKey");
        Assume.assumeTrue("Pass syteTestDiscovery=true with an account that has these features enabled",
                "true".equals(mArgs.getString("syteTestDiscovery")) && account != null && key != null);
        mSyte = Syte.newInstance(new SyteConfiguration(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), account, key));
    }

    private static <T> void ok(SyteResult<T> result) {
        assertTrue("HTTP " + result.resultCode + ": " + result.errorMessage, result.isSuccessful);
        assertNotNull(result.data);
    }

    @Test
    public void recentlyViewed() {
        ok(mSyte.getRecentlyViewed(new RecentlyViewed()));
    }

    @Test
    public void collection() {
        String id = mArgs.getString("syteTestCollectionId");
        Assume.assumeTrue("Pass syteTestCollectionId", id != null);
        ok(mSyte.getCollection(new MerchandisingCollection(id)));
    }

    @Test
    public void shoppableGallery() {
        String id = mArgs.getString("syteTestGalleryId");
        Assume.assumeTrue("Pass syteTestGalleryId", id != null);
        ok(mSyte.getShoppableGallery(new ShoppableGallery(id)));
    }

}
