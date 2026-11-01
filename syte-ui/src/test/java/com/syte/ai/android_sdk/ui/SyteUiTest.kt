package com.syte.ai.android_sdk.ui

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.syte.ai.android_sdk.data.result.offers.BoundsResult
import com.syte.ai.android_sdk.data.result.offers.Item
import com.syte.ai.android_sdk.data.result.text_search.HitsItem
import com.syte.ai.android_sdk.enums.TextSearchEventType
import com.syte.ai.android_sdk.events.BaseSyteEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Offline checks for the UI kit's data mapping and the events each view sends. No network. */
class SyteUiTest {

    private val service = SytePreviewService(delayMillis = 0)

    private fun body(event: BaseSyteEvent) = JsonParser.parseString(event.requestBodyString).asJsonObject

    private val eventNames get() = service.events.map { it.name }

    // Products

    @Test
    fun recommendationItemsMapToProducts() {
        val item = Gson().fromJson(
            """{"sku": "AB-1", "description": "Long description", "brand": "Monoprix", "imageUrl": "https://example.com/a.jpg",
               "offer": "https://example.com/p/ab-1", "price": "€29.99", "originalPrice": "€39.99",
               "floatPrice": 29.99, "floatOriginalPrice": 39.99}""",
            Item::class.java,
        )
        item.setOriginalData(JSONObject("""{"title": "Robe longue"}"""))
        val product = SyteUiProduct.from(item)
        assertEquals("AB-1", product.sku)
        assertEquals("the catalog title beats the description", "Robe longue", product.title)
        assertEquals("https://example.com/p/ab-1", product.productUrl)
        assertTrue(product.isOnSale)
    }

    @Test
    fun searchHitsMapToProducts() {
        val hit = Gson().fromJson(
            """{"sku": "6849751", "title": "Peignoir en polaire", "price": "€45.99", "originalPrice": "€45.99",
               "floatPrice": 45.99, "floatOriginalPrice": 45.99, "imageUrl": "https://example.com/b.jpg"}""",
            HitsItem::class.java,
        )
        val product = SyteUiProduct.from(hit)
        assertEquals("Peignoir en polaire", product.title)
        assertFalse("same price before and after isn't a sale", product.isOnSale)
    }

    @Test
    fun priceAmountsAreReadFromFormattedPrices() {
        assertEquals(45.99, SyteUiProduct.amount("€45.99")!!, 0.001)
        assertEquals(45.99, SyteUiProduct.amount("45,99 €")!!, 0.001)
        assertEquals(1299.0, SyteUiProduct.amount("$1,299.00")!!, 0.001)
        assertNull(SyteUiProduct.amount("free"))
        assertFalse("a missing price arrives as 0", SyteUiProduct.isDiscounted(0.0, 39.99))
    }

    @Test
    fun detectedItemsKeepTheirPositionAndImage() {
        val result = Gson().fromJson(
            """{"imageUrl": "https://uploads.example.com/photo", "bounds": [{"label": "dresses", "center": [0.4, 0.6],
               "gender": "female", "catalog": "fashion", "offers": "https://cdn-api.syteapi.com/v1.1/offers?x=1"}]}""",
            BoundsResult::class.java,
        )
        val detection = SyteLiveService.detection(result)
        assertEquals("https://uploads.example.com/photo", detection.imageUrl)
        val item = detection.items.single()
        assertEquals("dresses", item.label)
        assertEquals(0.4f, item.centerX)
        assertEquals(0.6f, item.centerY)
        assertNotNull(item.bound)
    }

    // Carousel

    @Test
    fun similarCarouselReportsItsResultsOnceAndEachClick() = runTest {
        val model = CarouselModel()
        val request = SyteRecommendation.Similar("AB-1", "https://example.com/a.jpg")
        model.runLoad(request, 4, "pdp", service)
        model.runLoad(request, 4, "pdp", service)

        val products = (model.state as CarouselModel.State.Loaded).products
        assertEquals(4, products.size)
        assertEquals("loading the same row twice reports it once", listOf("fe_si_show_layout"), eventNames)

        model.select(products[1], 1, request, "pdp", service)
        val click = service.events.last()
        assertEquals("fe_offer_click", click.name)
        assertEquals(listOf("similar_items"), click.tags)
        assertEquals("positions start at 1", 2, body(click)["position"].asInt)
    }

    @Test
    fun rowsWithoutDocumentedEventsSendNone() = runTest {
        val model = CarouselModel()
        val request = SyteRecommendation.BoughtTogether(listOf("AB-1"))
        model.runLoad(request, 4, "cart", service)
        val products = (model.state as CarouselModel.State.Loaded).products
        model.select(products[0], 0, request, "cart", service)
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun failedCarouselTriesAgainNextTime() = runTest {
        val model = CarouselModel()
        service.failure = "offline"
        model.runLoad(SyteRecommendation.Personalized(), 4, "home", service)
        assertEquals(CarouselModel.State.Failed("offline"), model.state)

        service.failure = null
        model.runLoad(SyteRecommendation.Personalized(), 4, "home", service)
        assertTrue(model.state is CarouselModel.State.Loaded)
    }

    // Visual search

    private fun visualSearch() = VisualSearchModel().also { it.service = service }

    @Test
    fun imageSearchPreselectsFirstItemAndReportsIt() = runTest {
        val model = visualSearch()
        model.runUrlSearch("https://example.com/pdp.jpg", "AB-1")

        assertEquals(VisualSearchModel.Phase.Detected, model.phase)
        assertEquals(0, model.selected)
        assertEquals(SytePreviewService.sampleProducts, model.results)
        assertEquals(listOf("fe_bb_show_layout", "fe_bb_show_results"), eventNames)

        val shown = body(service.events[1])
        assertEquals("https://example.com/pdp.jpg", shown["image_url"].asString)
        assertEquals(0, shown["bound_index"].asInt)
        assertEquals("AB-1", shown["sku"].asString)
        assertEquals(SytePreviewService.sampleProducts.mapNotNull { it.sku }.joinToString(","), shown["skus"].asString)
    }

    @Test
    fun tappingAnotherItemLoadsItsResults() = runTest {
        val model = visualSearch()
        model.runUrlSearch("https://example.com/pdp.jpg", null)
        model.runSelect(model.detection!!.items.last())

        assertEquals(1, model.selected)
        assertEquals(SytePreviewService.sampleProducts.size - 1, model.results.size)
        assertEquals(listOf("fe_bb_bb_click", "fe_bb_show_results"), eventNames.takeLast(2))
        assertEquals(1, body(service.events.last())["bound_index"].asInt)
    }

    @Test
    fun clickFromProductImageCarriesTheSourceProduct() = runTest {
        val model = visualSearch()
        model.runUrlSearch("https://example.com/pdp.jpg", "AB-1")
        model.resultTapped(model.results[0], 0)

        val click = body(service.events.last())
        assertEquals("AB-1", click["source_sku"].asString)
        assertEquals("https://example.com/p/1", click["target_url"].asString)
        assertEquals(1, click["position"].asInt)
    }

    @Test
    fun newPhotoDropsAnswersForTheOldOne() = runTest {
        val model = visualSearch()
        service.delayMillis = 200
        val first = async { model.runUrlSearch("https://example.com/a.jpg", null) }
        delay(50)
        model.reset()
        first.await()
        assertEquals(VisualSearchModel.Phase.Idle, model.phase)
        assertNull(model.detection)
    }

    // Text search

    private fun search() = SearchModel().also {
        it.service = service
        it.typingPauseMillis = 0
    }

    @Test
    fun searchReportsResultsWithHowTheQueryWasChosen() = runTest {
        val model = search()
        model.runSubmit("coat", TextSearchEventType.POPULAR_SEARCH)

        assertEquals(SearchModel.Phase.Results, model.phase)
        assertEquals("Wool coat", model.results.first().title)
        val event = service.events.single()
        assertEquals("fe_text_show_results", event.name)
        assertEquals("popular_search", body(event)["type"].asString)
    }

    @Test
    fun scrollingLoadsTheNextPage() = runTest {
        val model = SearchModel().also { it.service = PagedService() }
        model.runSubmit("dress", TextSearchEventType.SUBMIT)
        assertEquals(SearchModel.PAGE_SIZE, model.results.size)
        model.runLoadMore()
        assertEquals(30, model.results.size)
        model.runLoadMore()
        assertEquals("nothing left to load", 30, model.results.size)
    }

    @Test
    fun editingTheQueryLeavesResultsForSuggestions() = runTest {
        val model = search()
        model.runSubmit("coat", TextSearchEventType.SUBMIT)

        model.textChanged()
        assertEquals("the box still holds the searched query", SearchModel.Phase.Results, model.phase)

        model.text = "shi"
        model.textChanged()
        assertEquals(SearchModel.Phase.Browsing, model.phase)
        assertEquals(listOf("linen shirt"), model.suggestions)
    }
}

/** 30 results, served a page at a time. */
private class PagedService : SyteUiService {
    private val all = (0 until 30).map { SyteUiProduct(sku = "P$it") }
    override suspend fun search(query: String, from: Int, size: Int) = SyteSearchPage(all.drop(from).take(size), all.size)
    override suspend fun recommendations(request: SyteRecommendation, limit: Int) = emptyList<SyteUiProduct>()
    override suspend fun detectItems(photo: android.net.Uri) = SyteDetection(null, emptyList(), null)
    override suspend fun detectItems(imageUrl: String, sku: String?) = SyteDetection(null, emptyList(), null)
    override suspend fun products(item: SyteDetectedItem) = emptyList<SyteUiProduct>()
    override suspend fun suggestions(query: String) = emptyList<String>()
    override suspend fun popularSearches() = emptyList<String>()
    override fun recentSearches() = emptyList<String>()
    override fun track(event: BaseSyteEvent) {}
}
