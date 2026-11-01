package com.syte.ai.android_sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A titled, horizontally scrolling row of products. Shows placeholder cards while loading and
 * hides itself when there's nothing to show, so a product page never has an empty or broken row.
 *
 *     SyteProductCarousel(SyteRecommendation.Similar(product.sku, product.image), title = "You may also like") {
 *         openProduct(it)
 *     }
 *
 * @param pageName Sent with events as the page the row was on.
 * @param card Your own card, used instead of [SyteProductCard].
 */
@Composable
fun SyteProductCarousel(
    request: SyteRecommendation,
    modifier: Modifier = Modifier,
    title: String? = null,
    limit: Int = 12,
    pageName: String = "mobile_sdk",
    card: @Composable (SyteUiProduct) -> Unit = { SyteProductCard(it) },
    onSelect: (SyteUiProduct) -> Unit = {},
) {
    val service = LocalSyteService.current
    val theme = currentSyteTheme()
    val model: CarouselModel = viewModel(key = "syte.carousel.$request.$limit") { CarouselModel() }
    LaunchedEffect(request, service) { model.load(request, limit, pageName, service) }

    val products = when (val state = model.state) {
        CarouselModel.State.Loading -> null
        is CarouselModel.State.Loaded -> state.products.ifEmpty { return }
        is CarouselModel.State.Failed -> return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(theme.spacing)) {
        title?.let {
            Text(
                it,
                style = theme.headerStyle,
                color = theme.primaryText,
                modifier = Modifier.padding(horizontal = theme.edgeInset).semantics { heading() },
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = theme.edgeInset),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing),
            userScrollEnabled = products != null,
        ) {
            if (products == null) {
                items(4) { Box(Modifier.width(theme.carouselCardWidth)) { SytePlaceholderCard() } }
            } else {
                itemsIndexed(products) { index, product ->
                    Box(
                        Modifier
                            .width(theme.carouselCardWidth)
                            .testTag("syte.carousel.product")
                            .clickable(role = Role.Button) {
                                model.select(product, index, request, pageName, service)
                                onSelect(product)
                            },
                    ) {
                        card(product)
                    }
                }
            }
        }
    }
}

internal class CarouselModel : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val products: List<SyteUiProduct>) : State
        data class Failed(val message: String) : State
    }

    var state by mutableStateOf<State>(State.Loading)
        private set

    private var current: SyteRecommendation? = null

    /**
     * Loads in the ViewModel's scope rather than the row's, so a row that scrolls off screen
     * mid-load still has its products when it scrolls back.
     */
    fun load(request: SyteRecommendation, limit: Int, pageName: String, service: SyteUiService?) {
        if (current == request) return
        viewModelScope.launch { runLoad(request, limit, pageName, service) }
    }

    suspend fun runLoad(request: SyteRecommendation, limit: Int, pageName: String, service: SyteUiService?) {
        if (current == request) return
        if (service == null) {
            state = State.Failed(NO_SERVICE)
            return
        }
        current = request
        state = State.Loading
        try {
            val products = service.recommendations(request, limit)
            if (current != request) return
            state = State.Loaded(products)
            if (products.isNotEmpty()) {
                SyteUiEvents.carouselShown(request, products.size, pageName)?.let(service::track)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != request) return
            state = State.Failed(e.message ?: e.toString())
            // Try again the next time the row appears.
            current = null
        }
    }

    fun select(product: SyteUiProduct, index: Int, request: SyteRecommendation, pageName: String, service: SyteUiService?) {
        SyteUiEvents.carouselClicked(request, product, index, pageName)?.let { service?.track(it) }
    }
}
