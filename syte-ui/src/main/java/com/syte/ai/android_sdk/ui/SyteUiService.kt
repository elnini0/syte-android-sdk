package com.syte.ai.android_sdk.ui

import android.net.Uri
import com.syte.ai.android_sdk.data.result.offers.Bound
import com.syte.ai.android_sdk.events.BaseSyteEvent

/** What a carousel shows. */
sealed interface SyteRecommendation {

    /** Products that look like this one. Fits a product page. */
    data class Similar(val sku: String, val imageUrl: String) : SyteRecommendation

    /** Products that complete the outfit in this product's image. Fits a product page. */
    data class ShopTheLook(val sku: String, val imageUrl: String) : SyteRecommendation

    /** Picks for this shopper from what they viewed, optionally anchored on a product. */
    data class Personalized(val sku: String? = null) : SyteRecommendation

    /** Products often bought with these. Fits the cart. */
    data class BoughtTogether(val skus: List<String>) : SyteRecommendation

    /** The shopper's recently viewed products. */
    data object RecentlyViewed : SyteRecommendation

    /** A merchandising collection set up in Syte. */
    data class Collection(val id: String) : SyteRecommendation
}

/** An item Syte found in a photo. */
data class SyteDetectedItem(
    /** Position in the detection result, starting at 0. */
    val index: Int,
    /** Category, for example "dresses". */
    val label: String,
    /** Center of the item, from 0 to 1 across the image. */
    val centerX: Float,
    /** Center of the item, from 0 to 1 down the image. */
    val centerY: Float,
    val gender: String? = null,
    val catalog: String? = null,
) {
    internal var bound: Bound? = null
}

/** The items found in a photo, with the results for the first one. */
data class SyteDetection(
    /** Link to the searched image. For photos this is Syte's stored copy. Sent with visual search events. */
    val imageUrl: String?,
    val items: List<SyteDetectedItem>,
    /** Results for the first item, fetched with the detection to save a round trip. */
    val firstItemProducts: List<SyteUiProduct>?,
)

/** One page of text search results. */
data class SyteSearchPage(
    val products: List<SyteUiProduct>,
    /** Results across all pages. */
    val total: Int,
)

class SyteUiException(message: String) : Exception(message)

/**
 * Where the views get their data and send their events. Use [SyteLiveService] in the app and
 * [SytePreviewService] in previews and tests, or write your own to wrap Syte with your backend.
 * Called from the main thread.
 */
interface SyteUiService {

    suspend fun recommendations(request: SyteRecommendation, limit: Int): List<SyteUiProduct>

    suspend fun detectItems(photo: Uri): SyteDetection

    /** @param sku The product the image belongs to, when searching from a product page. */
    suspend fun detectItems(imageUrl: String, sku: String?): SyteDetection

    suspend fun products(item: SyteDetectedItem): List<SyteUiProduct>

    suspend fun search(query: String, from: Int, size: Int): SyteSearchPage

    suspend fun suggestions(query: String): List<String>

    suspend fun popularSearches(): List<String>

    fun recentSearches(): List<String>

    fun track(event: BaseSyteEvent)
}
