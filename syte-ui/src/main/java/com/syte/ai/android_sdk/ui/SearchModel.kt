package com.syte.ai.android_sdk.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syte.ai.android_sdk.enums.TextSearchEventType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Drives [SyteSearch]: suggestions while typing, then pages of results. */
internal class SearchModel : ViewModel() {

    sealed interface Phase {
        /** Recent and popular searches, or suggestions for what's typed. */
        data object Browsing : Phase
        data object Searching : Phase
        data object Results : Phase
        data object Empty : Phase
        data class Failed(val message: String) : Phase
    }

    var text by mutableStateOf("")
    var phase by mutableStateOf<Phase>(Phase.Browsing)
        private set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var popular by mutableStateOf<List<String>>(emptyList())
        private set
    var recent by mutableStateOf<List<String>>(emptyList())
        private set
    var results by mutableStateOf<List<SyteUiProduct>>(emptyList())
        private set
    var total by mutableStateOf(0)
        private set
    var loadingMore by mutableStateOf(false)
        private set

    /** The query the results are for. */
    var query = ""
        private set
    private var queryType = TextSearchEventType.SUBMIT

    var service: SyteUiService? = null
    var pageName = "mobile_sdk"

    /** Wait after the last keystroke before asking for suggestions. */
    var typingPauseMillis = 300L

    /** Bumped on every new search, so answers for an earlier one are dropped. */
    private var searchId = 0
    private var typing: Job? = null
    private var started = false

    private val typed get() = text.trim()

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch { runStart() }
    }

    fun onTextChange(value: String) {
        text = value
        typing?.cancel()
        typing = viewModelScope.launch { textChanged() }
    }

    fun submit(term: String, type: TextSearchEventType) {
        typing?.cancel()
        viewModelScope.launch { runSubmit(term, type) }
    }

    fun loadMore() {
        viewModelScope.launch { runLoadMore() }
    }

    fun retry() = submit(query, queryType)

    suspend fun runStart() {
        val service = service ?: return
        recent = service.recentSearches()
        if (popular.isEmpty()) popular = runCatching { service.popularSearches() }.getOrDefault(emptyList())
    }

    /** Runs after each keystroke, cancelled by the next one. */
    suspend fun textChanged() {
        val typed = typed
        if (phase != Phase.Browsing) {
            // Still showing the results for what's in the box.
            if (typed == query) return
            searchId++
            phase = Phase.Browsing
        }
        val service = service
        if (typed.isEmpty() || service == null) {
            suggestions = emptyList()
            return
        }
        delay(typingPauseMillis)
        val found = try {
            service.suggestions(typed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        if (this.typed == typed && phase == Phase.Browsing) suggestions = found
    }

    suspend fun runSubmit(term: String, type: TextSearchEventType) {
        val query = term.trim()
        val service = service ?: return
        if (query.isEmpty()) return
        val id = ++searchId
        this.query = query
        queryType = type
        phase = Phase.Searching
        results = emptyList()
        total = 0
        suggestions = emptyList()
        text = query
        try {
            val page = service.search(query, 0, PAGE_SIZE)
            if (id != searchId) return
            results = page.products
            total = page.total
            phase = if (page.products.isEmpty()) Phase.Empty else Phase.Results
            if (page.products.isNotEmpty()) {
                service.track(SyteUiEvents.searchResultsShown(query, type, page.total, pageName))
            }
            recent = service.recentSearches()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (id != searchId) return
            phase = Phase.Failed(e.message ?: e.toString())
        }
    }

    suspend fun runLoadMore() {
        val service = service ?: return
        if (phase != Phase.Results || loadingMore || results.size >= total) return
        val id = searchId
        loadingMore = true
        val page = try {
            service.search(query, results.size, PAGE_SIZE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (id != searchId) return
        loadingMore = false
        if (page == null) return
        results = results + page.products
        // An empty page before the total is reached means the total was an estimate.
        if (page.products.isEmpty()) total = results.size
    }

    fun clear() {
        typing?.cancel()
        searchId++
        text = ""
        query = ""
        phase = Phase.Browsing
        suggestions = emptyList()
        results = emptyList()
        total = 0
    }

    companion object {
        const val PAGE_SIZE = 24
    }
}
