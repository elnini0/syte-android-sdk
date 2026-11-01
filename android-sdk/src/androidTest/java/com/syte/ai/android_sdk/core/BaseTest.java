package com.syte.ai.android_sdk.core;

import androidx.test.platform.app.InstrumentationRegistry;

import com.syte.ai.android_sdk.exceptions.SyteInitializationException;

import android.os.Bundle;

import org.junit.Assume;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseTest {

    protected Syte mSyte;
    protected SyteConfiguration mConfiguration;

    @Before
    public void setUp() {
        try {
            // The demo account these tests were written for no longer exists; pass a real one
            // as instrumentation arguments (see LiveApiTest).
            Bundle args = InstrumentationRegistry.getArguments();
            String account = args.getString("syteAccountId");
            String key = args.getString("syteApiKey");
            Assume.assumeTrue("Pass syteAccountId and syteApiKey to run live tests", account != null && key != null);
            mConfiguration = new SyteConfiguration(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    account,
                    key
            );
            mSyte = Syte.newInstance(mConfiguration);
            mConfiguration.getStorage().clearViewedProducts();
            mConfiguration.getStorage().clearPopularSearch();
            mConfiguration.getStorage().clearSessionId();
        } catch (SyteInitializationException syteInitializationException) {
            fail("Unable to create configuration: " + syteInitializationException.getMessage());
        }
    }

    protected void startSessionInternal() {}

}
