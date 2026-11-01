package com.syte.ai.android_sdk.ui

import androidx.compose.runtime.Immutable
import com.syte.ai.android_sdk.data.result.discovery.SyteProduct
import com.syte.ai.android_sdk.data.result.offers.Item
import com.syte.ai.android_sdk.data.result.text_search.HitsItem

/**
 * A product as the UI kit shows it. Each Syte call returns its own product type, so every
 * result is mapped to this before it reaches a view.
 */
@Immutable
data class SyteUiProduct(
    val sku: String?,
    val title: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    /** The product page on the retailer's site. */
    val productUrl: String? = null,
    /** Price as the retailer formats it, for example "€45.99". */
    val price: String? = null,
    /** Price before discount, as the retailer formats it. */
    val originalPrice: String? = null,
    /** True when the original price is above the current price. */
    val isOnSale: Boolean = false,
) {

    /** The SKU, or the image URL for the rare result without one. */
    val id: String get() = sku ?: imageUrl.orEmpty()

    internal companion object {

        /** Visual search and recommendation results. */
        fun from(item: Item) = SyteUiProduct(
            sku = (item.sku ?: item.parentSku).nonEmpty(),
            title = item.originalData?.optString("title").nonEmpty() ?: item.description.nonEmpty(),
            brand = item.brand.nonEmpty(),
            imageUrl = item.imageUrl.nonEmpty(),
            productUrl = item.itemUrl.nonEmpty(),
            price = item.price.nonEmpty(),
            originalPrice = item.originalPrice.nonEmpty(),
            isOnSale = isDiscounted(item.floatPrice, item.floatOriginalPrice),
        )

        /** Text search results. */
        fun from(hit: HitsItem) = SyteUiProduct(
            sku = (hit.sku ?: hit.id).nonEmpty(),
            title = hit.title.nonEmpty() ?: hit.description.nonEmpty(),
            brand = hit.brand.nonEmpty(),
            imageUrl = hit.imageUrl.nonEmpty(),
            productUrl = hit.offer.nonEmpty(),
            price = hit.price.nonEmpty(),
            originalPrice = hit.originalPrice.nonEmpty(),
            isOnSale = isDiscounted(hit.floatPrice, hit.floatOriginalPrice),
        )

        /** Recently viewed, collections and galleries. */
        fun from(product: SyteProduct) = SyteUiProduct(
            sku = product.sku.nonEmpty(),
            title = product.title.nonEmpty(),
            brand = product.brand.nonEmpty(),
            imageUrl = product.imageUrl.nonEmpty(),
            productUrl = product.url.nonEmpty(),
            price = product.price.nonEmpty(),
            originalPrice = product.originalPrice.nonEmpty(),
            isOnSale = isDiscounted(amount(product.price), amount(product.originalPrice)),
        )

        /** Missing prices arrive as 0, so only positive prices count. */
        fun isDiscounted(price: Double?, original: Double?): Boolean {
            if (price == null || original == null || price <= 0 || original <= 0) return false
            return original > price
        }

        /** Reads the number out of a price string such as "45.99", "€45.99" or "45,99 €". */
        fun amount(text: String?): Double? {
            val kept = text?.filter { it.isDigit() || it == '.' || it == ',' }.orEmpty()
            if (kept.isEmpty()) return null
            // A comma followed by exactly two digits at the end is a decimal comma.
            val comma = kept.lastIndexOf(',')
            if (comma >= 0 && kept.length - comma == 3 && '.' !in kept) {
                return kept.replace(',', '.').toDoubleOrNull()
            }
            return kept.replace(",", "").toDoubleOrNull()
        }
    }
}

internal fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }
