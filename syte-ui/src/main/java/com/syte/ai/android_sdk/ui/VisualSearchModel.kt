package com.syte.ai.android_sdk.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Drives [SyteVisualSearch]: detect items in an image, preselect the first one, load results for
 * whichever item the shopper taps. Kept in a ViewModel so a rotation doesn't lose the search.
 */
internal class VisualSearchModel : ViewModel() {

    sealed interface Source {
        data class Photo(val uri: Uri) : Source
        data class Url(val url: String) : Source
    }

    sealed interface Phase {
        data object Idle : Phase
        data object Detecting : Phase
        data object Detected : Phase
        data class Failed(val message: String) : Phase
    }

    var source by mutableStateOf<Source?>(null)
        private set
    var phase by mutableStateOf<Phase>(Phase.Idle)
        private set
    var detection by mutableStateOf<SyteDetection?>(null)
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var results by mutableStateOf<List<SyteUiProduct>>(emptyList())
        private set
    var loadingResults by mutableStateOf(false)
        private set
    var resultsError by mutableStateOf<String?>(null)
        private set

    var service: SyteUiService? = null
    var pageName = "mobile_sdk"

    /** The product whose image is being searched, when started from a product page. */
    var sourceSku: String? = null
        private set

    /** Bumped on every new image, so answers for an earlier image are dropped. */
    private var searchId = 0

    fun searchPhoto(uri: Uri) {
        viewModelScope.launch { runPhotoSearch(uri) }
    }

    fun searchUrl(url: String, sku: String?) {
        viewModelScope.launch { runUrlSearch(url, sku) }
    }

    fun select(item: SyteDetectedItem) {
        viewModelScope.launch { runSelect(item) }
    }

    fun retry() {
        viewModelScope.launch { runRetry() }
    }

    suspend fun runPhotoSearch(uri: Uri) {
        source = Source.Photo(uri)
        sourceSku = null
        detect { it.detectItems(uri) }
    }

    suspend fun runUrlSearch(url: String, sku: String?) {
        source = Source.Url(url)
        sourceSku = sku
        detect { it.detectItems(url, sku) }
    }

    suspend fun runSelect(item: SyteDetectedItem) {
        val service = service ?: return
        val detection = detection ?: return
        if (selected == item.index) return
        selected = item.index
        service.track(SyteUiEvents.itemTapped(item, detection, pageName))
        loadResults(item, searchId)
    }

    suspend fun runRetry() {
        val source = source
        when {
            phase is Phase.Failed && source is Source.Photo -> runPhotoSearch(source.uri)
            phase is Phase.Failed && source is Source.Url -> runUrlSearch(source.url, sourceSku)
            else -> detection?.items?.firstOrNull { it.index == selected }?.let { loadResults(it, searchId) }
        }
    }

    fun resultTapped(product: SyteUiProduct, index: Int) {
        SyteUiEvents.resultClicked(product, index, sourceSku, pageName)?.let { service?.track(it) }
    }

    fun reset() {
        searchId++
        source = null
        phase = Phase.Idle
        detection = null
        selected = null
        results = emptyList()
        loadingResults = false
        resultsError = null
    }

    private suspend fun detect(find: suspend (SyteUiService) -> SyteDetection) {
        val service = service ?: run {
            phase = Phase.Failed(NO_SERVICE)
            return
        }
        val id = ++searchId
        phase = Phase.Detecting
        detection = null
        selected = null
        results = emptyList()
        resultsError = null
        try {
            val found = find(service)
            if (id != searchId) return
            detection = found
            phase = Phase.Detected
            val first = found.items.firstOrNull() ?: return
            service.track(SyteUiEvents.itemsShown(found, pageName))
            selected = first.index
            val products = found.firstItemProducts
            if (products != null) show(products, first) else loadResults(first, id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (id != searchId) return
            phase = Phase.Failed(e.message ?: e.toString())
        }
    }

    private suspend fun loadResults(item: SyteDetectedItem, id: Int) {
        val service = service ?: return
        loadingResults = true
        resultsError = null
        results = emptyList()
        try {
            val products = service.products(item)
            if (id != searchId || selected != item.index) return
            show(products, item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (id != searchId || selected != item.index) return
            loadingResults = false
            resultsError = e.message ?: e.toString()
        }
    }

    private fun show(products: List<SyteUiProduct>, item: SyteDetectedItem) {
        results = products
        loadingResults = false
        val detection = detection ?: return
        if (products.isEmpty()) return
        service?.track(SyteUiEvents.itemResultsShown(products, item, detection, sourceSku, pageName))
    }
}
