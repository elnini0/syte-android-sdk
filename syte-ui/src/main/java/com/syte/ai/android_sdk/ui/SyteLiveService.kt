package com.syte.ai.android_sdk.ui

import android.content.Context
import android.net.Uri
import com.syte.ai.android_sdk.core.Syte
import com.syte.ai.android_sdk.data.BoughtTogether
import com.syte.ai.android_sdk.data.ImageSearch
import com.syte.ai.android_sdk.data.MerchandisingCollection
import com.syte.ai.android_sdk.data.Personalization
import com.syte.ai.android_sdk.data.RecentlyViewed
import com.syte.ai.android_sdk.data.ShopTheLook
import com.syte.ai.android_sdk.data.SimilarItems
import com.syte.ai.android_sdk.data.TextSearch
import com.syte.ai.android_sdk.data.UrlImageSearch
import com.syte.ai.android_sdk.data.result.SyteResult
import com.syte.ai.android_sdk.data.result.offers.BoundsResult
import com.syte.ai.android_sdk.enums.SyteProductType
import com.syte.ai.android_sdk.events.BaseSyteEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calls Syte through the SDK.
 *
 *     val syte = Syte.newInstance(SyteConfiguration(context, "<account id>", "<api key>"))
 *     val service = SyteLiveService(context, syte)
 */
class SyteLiveService(context: Context, val syte: Syte) : SyteUiService {

    private val appContext = context.applicationContext

    /** Sent as the referer on recommendation calls. */
    var syteUrlReferer = "mobile_sdk"

    private var pendingSuggestions: CompletableDeferred<List<String>>? = null

    override suspend fun recommendations(request: SyteRecommendation, limit: Int): List<SyteUiProduct> =
        when (request) {
            is SyteRecommendation.Similar -> {
                val similar = SimilarItems(request.sku, request.imageUrl).apply {
                    setLimit(limit)
                    setSyteUrlReferer(syteUrlReferer)
                }
                call { syte.getSimilarProducts(similar) }.items.orEmpty().map(SyteUiProduct::from)
            }
            is SyteRecommendation.ShopTheLook -> {
                val look = ShopTheLook(request.sku, request.imageUrl).apply {
                    setLimit(limit)
                    setSyteUrlReferer(syteUrlReferer)
                }
                // Interleave the outfit's categories so the first cards aren't all from one of them.
                call { syte.getShopTheLook(look) }.getItemsForAllLabels(true).take(limit).map(SyteUiProduct::from)
            }
            is SyteRecommendation.Personalized -> {
                val personalization = Personalization().apply {
                    request.sku?.let { setSku(it) }
                    setLimit(limit)
                    setSyteUrlReferer(syteUrlReferer)
                }
                call { syte.getPersonalizedProducts(personalization) }.items.orEmpty().map(SyteUiProduct::from)
            }
            is SyteRecommendation.BoughtTogether -> {
                val together = BoughtTogether(request.skus).apply {
                    setLimit(limit)
                    setSyteUrlReferer(syteUrlReferer)
                }
                call { syte.getBoughtTogether(together) }.items.orEmpty().map(SyteUiProduct::from)
            }
            SyteRecommendation.RecentlyViewed ->
                call { syte.getRecentlyViewed(RecentlyViewed()) }.products.take(limit).map(SyteUiProduct::from)
            is SyteRecommendation.Collection -> {
                val collection = MerchandisingCollection(request.id).apply { setSize(limit) }
                call { syte.getCollection(collection) }.products.map(SyteUiProduct::from)
            }
        }

    override suspend fun detectItems(photo: Uri): SyteDetection {
        val search = ImageSearch(photo).apply { setRetrieveItemsForTheFirstBound(true) }
        return detection(call { syte.getBoundsForImage(appContext, search) })
    }

    override suspend fun detectItems(imageUrl: String, sku: String?): SyteDetection {
        val search = UrlImageSearch(imageUrl, SyteProductType.DISCOVERY_BUTTON).apply {
            sku?.let { setSku(it) }
            setRetrieveItemsForTheFirstBound(true)
        }
        return detection(call { syte.getBoundsForImageUrl(search) })
    }

    override suspend fun products(item: SyteDetectedItem): List<SyteUiProduct> {
        val bound = item.bound ?: throw SyteUiException("This item has no results link.")
        return call { syte.getItemsForBound(bound, null) }.items.orEmpty().map(SyteUiProduct::from)
    }

    override suspend fun search(query: String, from: Int, size: Int): SyteSearchPage {
        val search = TextSearch(query, syte.configuration.locale).apply {
            setFrom(from)
            setSize(size)
        }
        val result = call { syte.getTextSearch(search) }.result
        val hits = result?.hits.orEmpty()
        return SyteSearchPage(hits.map(SyteUiProduct::from), result?.total ?: hits.size)
    }

    override suspend fun suggestions(query: String): List<String> {
        // The SDK spaces autocomplete calls 500ms apart and drops or replaces calls made sooner,
        // without ever answering them. Resolve the call being replaced so nothing waits forever.
        pendingSuggestions?.complete(emptyList())
        val request = CompletableDeferred<List<String>>()
        pendingSuggestions = request
        syte.getAutoCompleteForTextSearch(query) { result ->
            if (result.isSuccessful) {
                request.complete(result.data?.results?.suggestedSearchTerms.orEmpty().mapNotNull { it.searchTerm })
            } else {
                request.completeExceptionally(SyteUiException(result.errorMessage ?: "Autocomplete failed."))
            }
        }
        return request.await()
    }

    override suspend fun popularSearches(): List<String> =
        call { syte.getPopularSearches(syte.configuration.locale) }

    override fun recentSearches(): List<String> {
        val seen = HashSet<String>()
        return syte.recentTextSearches.orEmpty().filter { it.isNotEmpty() && seen.add(it.lowercase()) }
    }

    override fun track(event: BaseSyteEvent) {
        syte.fireEvent(event)
    }

    private suspend fun <T> call(block: () -> SyteResult<T>): T = withContext(Dispatchers.IO) {
        val result = block()
        val data = result.data
        if (!result.isSuccessful || data == null) {
            throw SyteUiException(result.errorMessage ?: "Syte answered ${result.resultCode}.")
        }
        data
    }

    internal companion object {

        fun detection(result: BoundsResult) = SyteDetection(
            imageUrl = result.imageUrl,
            items = result.bounds.orEmpty().mapIndexed { index, bound ->
                val center = bound.center.orEmpty()
                SyteDetectedItem(
                    index = index,
                    label = bound.label.orEmpty(),
                    centerX = center.getOrNull(0)?.toFloat() ?: 0.5f,
                    centerY = center.getOrNull(1)?.toFloat() ?: 0.5f,
                    gender = bound.gender,
                    catalog = bound.catalog,
                ).also { it.bound = bound }
            },
            firstItemProducts = result.itemsForFirstBound?.items?.map(SyteUiProduct::from),
        )
    }
}
