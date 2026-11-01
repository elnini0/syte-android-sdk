package com.syte.ai.android_sdk.ui

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.syte.ai.android_sdk.core.Syte
import com.syte.ai.android_sdk.core.SyteConfiguration
import java.io.File
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the live service against a real account. Skipped unless the account is passed in:
 * -Pandroid.testInstrumentationRunnerArguments.syteAccountId=… and …syteApiKey=…
 */
@RunWith(AndroidJUnit4::class)
class SyteLiveServiceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var service: SyteLiveService
    private lateinit var query: String

    @Before
    fun setUp() {
        val args = InstrumentationRegistry.getArguments()
        val account = args.getString("syteAccountId")
        val key = args.getString("syteApiKey")
        assumeTrue("Pass syteAccountId and syteApiKey to run live tests", !account.isNullOrEmpty() && !key.isNullOrEmpty())
        query = args.getString("syteTestQuery", "robe")
        val configuration = SyteConfiguration(context, account, key).apply { setLocale(args.getString("syteTestLang", "fr_FR")) }
        service = SyteLiveService(context, Syte.newInstance(configuration))
    }

    @Test
    fun searchThenRecommendationsForTheFirstResult() = runBlocking {
        val product = service.search(query, 0, 4).products.first()
        assertNotNull(product.title)
        assertNotNull(product.price)
        val sku = product.sku!!
        val image = product.imageUrl!!

        val similar = service.recommendations(SyteRecommendation.Similar(sku, image), 6)
        assertFalse(similar.isEmpty())
        assertTrue(similar.all { it.imageUrl != null && it.sku != null })
        val look = service.recommendations(SyteRecommendation.ShopTheLook(sku, image), 6)
        assertTrue(look.size <= 6)
        val together = service.recommendations(SyteRecommendation.BoughtTogether(listOf(sku)), 6)
        assertFalse(together.isEmpty())
    }

    @Test
    fun photoDetectionReturnsStoredImageAndResults() = runBlocking {
        val url = service.search(query, 0, 1).products.first().imageUrl!!
        val photo = File(context.cacheDir, "live-photo.jpg")
        URL(url).openStream().use { input -> photo.outputStream().use { input.copyTo(it) } }

        val detection = service.detectItems(Uri.fromFile(photo))
        assertFalse(detection.items.isEmpty())
        assertTrue("photo searches report where Syte stored the photo", detection.imageUrl?.startsWith("https://") == true)
        assertFalse(detection.firstItemProducts!!.isEmpty())
        assertFalse(service.products(detection.items[0]).isEmpty())
    }

    @Test
    fun suggestionsAndPopularSearches() = runBlocking {
        assertNotNull(service.suggestions(query.take(3)))
        assertNotNull(service.popularSearches())
    }
}
