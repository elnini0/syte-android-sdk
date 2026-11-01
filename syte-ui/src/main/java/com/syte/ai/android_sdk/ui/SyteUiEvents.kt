package com.syte.ai.android_sdk.ui

import com.syte.ai.android_sdk.enums.TextSearchEventType
import com.syte.ai.android_sdk.events.BaseSyteEvent
import com.syte.ai.android_sdk.events.EventBBClick
import com.syte.ai.android_sdk.events.EventBBShowLayout
import com.syte.ai.android_sdk.events.EventBBShowResults
import com.syte.ai.android_sdk.events.EventOfferClick
import com.syte.ai.android_sdk.events.EventShopTheLookOfferClick
import com.syte.ai.android_sdk.events.EventShopTheLookShowLayout
import com.syte.ai.android_sdk.events.EventSimilarItemsOfferClick
import com.syte.ai.android_sdk.events.EventSimilarItemsShowLayout
import com.syte.ai.android_sdk.events.EventTextShowResults

/**
 * The events the views send on their own. Positions start at 1, as in Syte's event docs.
 *
 * Personalized, bought together, recently viewed and collection rows send nothing, and neither do
 * clicks on text search results: Syte documents no events for them and the SDK has none, so
 * guessing names would only add noise to reports.
 */
internal object SyteUiEvents {

    fun carouselShown(request: SyteRecommendation, count: Int, pageName: String): BaseSyteEvent? = when (request) {
        is SyteRecommendation.Similar -> EventSimilarItemsShowLayout(count, pageName)
        is SyteRecommendation.ShopTheLook -> EventShopTheLookShowLayout(count, pageName)
        else -> null
    }

    fun carouselClicked(request: SyteRecommendation, product: SyteUiProduct, index: Int, pageName: String): BaseSyteEvent? {
        val sku = product.sku ?: return null
        return when (request) {
            is SyteRecommendation.Similar -> EventSimilarItemsOfferClick(sku, index + 1, pageName)
            is SyteRecommendation.ShopTheLook -> EventShopTheLookOfferClick(sku, index + 1, pageName)
            else -> null
        }
    }

    fun itemsShown(detection: SyteDetection, pageName: String): BaseSyteEvent =
        EventBBShowLayout(detection.imageUrl.orEmpty(), detection.items.size, pageName)

    fun itemTapped(item: SyteDetectedItem, detection: SyteDetection, pageName: String): BaseSyteEvent =
        EventBBClick(detection.imageUrl.orEmpty(), item.label, item.gender.orEmpty(), item.catalog.orEmpty(), pageName)

    fun itemResultsShown(
        products: List<SyteUiProduct>,
        item: SyteDetectedItem,
        detection: SyteDetection,
        sourceSku: String?,
        pageName: String,
    ): BaseSyteEvent = EventBBShowResults(
        detection.imageUrl.orEmpty(),
        item.label,
        products.size,
        pageName,
        products.mapNotNull { it.sku },
        sourceSku,
        item.index,
    )

    fun resultClicked(product: SyteUiProduct, index: Int, sourceSku: String?, pageName: String): BaseSyteEvent? {
        val sku = product.sku ?: return null
        return EventOfferClick(sku, index + 1, pageName, sourceSku, product.productUrl)
    }

    fun searchResultsShown(query: String, type: TextSearchEventType, total: Int, pageName: String): BaseSyteEvent =
        EventTextShowResults(query, type, total, pageName)
}
