package com.syte.ai.android_sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syte.ai.android_sdk.enums.TextSearchEventType

/**
 * Text search with recent and popular searches, suggestions while typing, and a results grid
 * that loads more as the shopper scrolls. Fills the space it's given.
 */
@Composable
fun SyteSearch(
    modifier: Modifier = Modifier,
    pageName: String = "mobile_sdk",
    card: @Composable (SyteUiProduct) -> Unit = { SyteProductCard(it) },
    onSelect: (SyteUiProduct) -> Unit = {},
) {
    val service = LocalSyteService.current
    val theme = currentSyteTheme()
    val focus = LocalFocusManager.current
    val model: SearchModel = viewModel(key = "syte.search.$pageName") { SearchModel() }
    SideEffect {
        model.service = service
        model.pageName = pageName
    }
    LaunchedEffect(service) {
        model.service = service
        model.start()
    }

    fun search(term: String, type: TextSearchEventType) {
        focus.clearFocus()
        model.submit(term, type)
    }

    Column(modifier.fillMaxSize().background(theme.background)) {
        SearchField(
            text = model.text,
            onTextChange = model::onTextChange,
            onSubmit = { search(model.text, TextSearchEventType.SUBMIT) },
            onClear = model::clear,
        )
        when (val phase = model.phase) {
            SearchModel.Phase.Browsing -> LazyColumn(Modifier.fillMaxSize()) {
                if (model.text.isBlank()) {
                    terms(theme.strings.recentSearches, model.recent.take(5), SyteIcons.History) { search(it, TextSearchEventType.RECENT_SEARCH) }
                    terms(theme.strings.popularSearches, model.popular, SyteIcons.Trending) { search(it, TextSearchEventType.POPULAR_SEARCH) }
                } else {
                    terms(null, model.suggestions, Icons.Filled.Search) { search(it, TextSearchEventType.SUBMIT) }
                }
            }
            SearchModel.Phase.Searching -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.accent)
            }
            SearchModel.Phase.Results -> LazyVerticalGrid(
                columns = GridCells.Adaptive(theme.gridMinCardWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(theme.edgeInset),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing),
                verticalArrangement = Arrangement.spacedBy(theme.spacing * 2),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(theme.strings.resultCount.format(model.total), style = theme.titleStyle, color = theme.secondaryText)
                }
                syteProducts(model.results, onSelect = { _, product -> onSelect(product) }, onReachEnd = model::loadMore, card = card)
                if (model.loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = theme.accent)
                        }
                    }
                }
            }
            SearchModel.Phase.Empty -> Text(
                theme.strings.noResults.format(model.query),
                style = theme.titleStyle,
                color = theme.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(48.dp),
            )
            is SearchModel.Phase.Failed -> SyteErrorView(phase.message, model::retry)
        }
    }
}

@Composable
private fun SearchField(text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit, onClear: () -> Unit) {
    val theme = currentSyteTheme()
    Row(
        Modifier
            .padding(horizontal = theme.edgeInset, vertical = 8.dp)
            .fillMaxWidth()
            .background(theme.imageBackground, RoundedCornerShape(theme.cornerRadius))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = theme.secondaryText, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 12.dp)) {
            if (text.isEmpty()) Text(theme.strings.searchPlaceholder, style = theme.titleStyle, color = theme.secondaryText)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = theme.titleStyle.copy(color = theme.primaryText),
                cursorBrush = SolidColor(theme.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().testTag("syte.search.field"),
            )
        }
        if (text.isNotEmpty()) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = theme.strings.clearSearch, tint = theme.secondaryText)
            }
        }
    }
}

private fun LazyListScope.terms(title: String?, terms: List<String>, icon: ImageVector, onTap: (String) -> Unit) {
    if (terms.isEmpty()) return
    if (title != null) {
        item {
            val theme = currentSyteTheme()
            Text(
                title,
                style = theme.headerStyle,
                color = theme.primaryText,
                modifier = Modifier.padding(start = theme.edgeInset, end = theme.edgeInset, top = 16.dp, bottom = 4.dp).semantics { heading() },
            )
        }
    }
    items(terms) { term ->
        val theme = currentSyteTheme()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onTap(term) }
                .padding(horizontal = theme.edgeInset, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = theme.secondaryText, modifier = Modifier.size(20.dp))
            Text(term, style = theme.titleStyle, color = theme.primaryText)
        }
    }
}
