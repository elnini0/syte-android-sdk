package com.syte.ai.android_sdk.ui

import android.net.Uri
import com.syte.ai.android_sdk.events.BaseSyteEvent
import kotlinx.coroutines.delay

/**
 * Made-up products with no network calls, for previews, tests and offline demos.
 * Keeps every event it's sent in [events].
 *
 * @param delayMillis Wait before each answer, to show loading states.
 */
class SytePreviewService(var delayMillis: Long = 400) : SyteUiService {

    /** When set, every call fails with this message. */
    var failure: String? = null

    private val sent = mutableListOf<BaseSyteEvent>()
    val events: List<BaseSyteEvent> get() = sent

    override suspend fun recommendations(request: SyteRecommendation, limit: Int): List<SyteUiProduct> {
        answer()
        val shift = when (request) {
            is SyteRecommendation.Similar -> 0
            is SyteRecommendation.ShopTheLook -> 2
            is SyteRecommendation.Personalized -> 4
            is SyteRecommendation.BoughtTogether -> 6
            SyteRecommendation.RecentlyViewed -> 1
            is SyteRecommendation.Collection -> 3
        }
        return (sampleProducts.drop(shift) + sampleProducts.take(shift)).take(limit)
    }

    override suspend fun detectItems(photo: Uri): SyteDetection {
        answer()
        return sampleDetection("https://example.com/uploads/photo.jpg")
    }

    override suspend fun detectItems(imageUrl: String, sku: String?): SyteDetection {
        answer()
        return sampleDetection(imageUrl)
    }

    override suspend fun products(item: SyteDetectedItem): List<SyteUiProduct> {
        answer()
        return sampleProducts.reversed().drop(item.index)
    }

    override suspend fun search(query: String, from: Int, size: Int): SyteSearchPage {
        answer()
        val matches = sampleProducts.filter {
            it.title.orEmpty().contains(query, ignoreCase = true) || it.brand.orEmpty().contains(query, ignoreCase = true)
        }
        val found = if (matches.isEmpty() && query.length < 3) sampleProducts else matches
        return SyteSearchPage(found.drop(from).take(size), found.size)
    }

    override suspend fun suggestions(query: String): List<String> {
        answer()
        return sampleProducts.mapNotNull { it.title?.lowercase() }.filter { query.lowercase() in it }
    }

    override suspend fun popularSearches(): List<String> {
        answer()
        return listOf("coat", "shirt", "loafers")
    }

    override fun recentSearches(): List<String> = emptyList()

    override fun track(event: BaseSyteEvent) {
        sent += event
    }

    private suspend fun answer() {
        if (delayMillis > 0) delay(delayMillis)
        failure?.let { throw SyteUiException(it) }
    }

    companion object {

        val sampleProducts: List<SyteUiProduct> = listOf(
            listOf("Linen shirt", "Northline", "€49.00", null),
            listOf("Wide-leg trousers", "Northline", "€39.00", "€59.00"),
            listOf("Wool coat", "Atelier 9", "€189.00", null),
            listOf("Leather loafers", "Brunn", "€120.00", null),
            listOf("Knit cardigan", "Atelier 9", "€45.00", "€65.00"),
            listOf("Denim jacket", "Brunn", "€79.00", null),
            listOf("Silk scarf", "Maison Ore", "€35.00", null),
            listOf("Canvas tote", "Northline", "€25.00", null),
        ).mapIndexed { index, (title, brand, price, original) ->
            SyteUiProduct(
                sku = "SAMPLE-${index + 1}",
                title = title,
                brand = brand,
                productUrl = "https://example.com/p/${index + 1}",
                price = price,
                originalPrice = original,
                isOnSale = original != null,
            )
        }

        private fun sampleDetection(imageUrl: String) = SyteDetection(
            imageUrl = imageUrl,
            items = listOf(
                SyteDetectedItem(0, "tops", 0.5f, 0.32f, "female", "fashion"),
                SyteDetectedItem(1, "pants", 0.48f, 0.68f, "female", "fashion"),
            ),
            firstItemProducts = sampleProducts,
        )
    }
}
