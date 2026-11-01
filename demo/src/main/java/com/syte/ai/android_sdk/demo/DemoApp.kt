package com.syte.ai.android_sdk.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syte.ai.android_sdk.events.EventPageView
import com.syte.ai.android_sdk.ui.LocalSyteService
import com.syte.ai.android_sdk.ui.SyteIcons
import com.syte.ai.android_sdk.ui.SytePrice
import com.syte.ai.android_sdk.ui.SyteProductCarousel
import com.syte.ai.android_sdk.ui.SyteProductImage
import com.syte.ai.android_sdk.ui.SyteRecommendation
import com.syte.ai.android_sdk.ui.SyteSearch
import com.syte.ai.android_sdk.ui.SyteUiProduct
import com.syte.ai.android_sdk.ui.SyteVisualSearch

/** Where a tab's navigation can go. */
sealed interface Route {
    data class Product(val product: SyteUiProduct) : Route
    data class SimilarLooks(val product: SyteUiProduct) : Route
}

private data class Tab(val title: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Search", "Search", Icons.Filled.Search),
    Tab("Photo search", "Photo", SyteIcons.Camera),
    Tab("For you", "For you", Icons.Filled.Star),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp(isLive: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // One back stack per tab, kept while switching tabs.
    val stacks = remember { List(tabs.size) { mutableStateListOf<Route>() } }
    val stack = stacks[tab]
    val open: (Route) -> Unit = { stack.add(it) }
    BackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }

    val top = stack.lastOrNull()
    val title = when (top) {
        null -> tabs[tab].title
        is Route.Product -> top.product.brand.orEmpty()
        is Route.SimilarLooks -> "Similar looks"
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (top != null) {
                        IconButton(onClick = { stack.removeAt(stack.lastIndex) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (top) {
                null -> when (tab) {
                    0 -> SyteSearch(pageName = "search") { open(Route.Product(it)) }
                    1 -> SyteVisualSearch(pageName = "camera") { open(Route.Product(it)) }
                    else -> ForYouPage(isLive, open)
                }
                is Route.Product -> ProductPage(top.product, open)
                is Route.SimilarLooks -> SyteVisualSearch(imageUrl = top.product.imageUrl, sku = top.product.sku, pageName = "pdp") {
                    open(Route.Product(it))
                }
            }
        }
    }
}

/** A product page with the three recommendation rows Syte offers for it. */
@Composable
fun ProductPage(product: SyteUiProduct, open: (Route) -> Unit) {
    val service = LocalSyteService.current
    LaunchedEffect(product.sku) {
        // The app's own job, not the kit's: tell Syte the product was viewed, which also feeds
        // "Picked for you".
        product.sku?.let { service?.track(EventPageView(it, "pdp")) }
    }
    val sku = product.sku
    val image = product.imageUrl
    LazyColumn(
        modifier = Modifier.testTag("demo.product"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyteProductImage(image, Modifier.widthIn(max = 420.dp))
                product.brand?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(product.title.orEmpty(), style = MaterialTheme.typography.titleLarge)
                SytePrice(product)
                if (image != null) {
                    Button(onClick = { open(Route.SimilarLooks(product)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Icon(SyteIcons.Camera, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Find similar looks")
                    }
                }
            }
        }
        if (sku != null && image != null) {
            item {
                SyteProductCarousel(SyteRecommendation.Similar(sku, image), title = "Similar items", pageName = "pdp") { open(Route.Product(it)) }
            }
            item {
                SyteProductCarousel(SyteRecommendation.ShopTheLook(sku, image), title = "Complete the look", pageName = "pdp") { open(Route.Product(it)) }
            }
            item {
                SyteProductCarousel(SyteRecommendation.BoughtTogether(listOf(sku)), title = "Often bought together", pageName = "pdp") {
                    open(Route.Product(it))
                }
            }
        }
    }
}

@Composable
fun ForYouPage(isLive: Boolean, open: (Route) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item {
            Text(
                if (isLive) "Live account" else "Sample products. Put an account in local.properties to use your catalog.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            SyteProductCarousel(SyteRecommendation.Personalized(), title = "Picked for you", pageName = "home") { open(Route.Product(it)) }
        }
        item {
            SyteProductCarousel(SyteRecommendation.RecentlyViewed, title = "Recently viewed", pageName = "home") { open(Route.Product(it)) }
        }
    }
}
