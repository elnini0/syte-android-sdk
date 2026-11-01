package com.syte.ai.android_sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The default product card: image, brand, title and price. Pass your own card to any carousel or
 * grid to replace it.
 */
@Composable
fun SyteProductCard(product: SyteUiProduct, modifier: Modifier = Modifier) {
    val theme = currentSyteTheme()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SyteProductImage(product.imageUrl, Modifier.padding(bottom = 4.dp))
        product.brand?.let {
            Text(it, style = theme.brandStyle, color = theme.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        product.title?.let {
            Text(it, style = theme.titleStyle, color = theme.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        SytePrice(product)
    }
}

/** Current price, with the original price struck through when the product is on sale. */
@Composable
fun SytePrice(product: SyteUiProduct, modifier: Modifier = Modifier) {
    val theme = currentSyteTheme()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        product.price?.let {
            Text(it, style = theme.priceStyle, color = if (product.isOnSale) theme.salePrice else theme.primaryText, maxLines = 1)
        }
        if (product.isOnSale) {
            product.originalPrice?.let {
                Text(it, style = theme.titleStyle, color = theme.secondaryText, textDecoration = TextDecoration.LineThrough, maxLines = 1)
            }
        }
    }
}

/** A product image at the theme's aspect ratio, cropped to fill. */
@Composable
fun SyteProductImage(url: String?, modifier: Modifier = Modifier) {
    val theme = currentSyteTheme()
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(theme.imageAspectRatio)
            .clip(RoundedCornerShape(theme.cornerRadius))
            .background(theme.imageBackground),
    )
}

/**
 * Adds products to a `LazyVerticalGrid`, one per cell. Use `GridCells.Adaptive(theme.gridMinCardWidth)`
 * for the theme's column width.
 *
 * @param onSelect Called with the product's position, starting at 0.
 * @param onReachEnd Called when the last product scrolls into view, to load the next page.
 */
fun LazyGridScope.syteProducts(
    products: List<SyteUiProduct>,
    onSelect: (Int, SyteUiProduct) -> Unit,
    onReachEnd: (() -> Unit)? = null,
    card: @Composable (SyteUiProduct) -> Unit = { SyteProductCard(it) },
) {
    itemsIndexed(products) { index, product ->
        Box(
            Modifier
                .testTag("syte.product")
                .clickable(role = Role.Button) { onSelect(index, product) },
        ) {
            card(product)
        }
        if (onReachEnd != null && index == products.lastIndex) {
            LaunchedEffect(products.size) { onReachEnd() }
        }
    }
}

/** Grey card shapes shown while a row loads. */
@Composable
internal fun SytePlaceholderCard() {
    val theme = currentSyteTheme()
    val shape = RoundedCornerShape(theme.cornerRadius / 2)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(theme.imageAspectRatio).clip(RoundedCornerShape(theme.cornerRadius)).background(theme.imageBackground))
        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(shape).background(theme.imageBackground))
        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(shape).background(theme.imageBackground))
        Box(Modifier.fillMaxWidth(0.3f).height(12.dp).clip(shape).background(theme.imageBackground))
    }
}

/** Shown where a list failed to load. */
@Composable
internal fun SyteErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val theme = currentSyteTheme()
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = theme.titleStyle, color = theme.secondaryText, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text(theme.strings.tryAgain, color = theme.accent)
        }
    }
}
