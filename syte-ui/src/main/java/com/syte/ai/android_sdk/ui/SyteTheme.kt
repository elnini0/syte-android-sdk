package com.syte.ai.android_sdk.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors, fonts, sizes and text for every Syte view. Start from the app's Material theme and
 * change what you need:
 *
 *     SyteUi(service, SyteTheme.fromMaterialTheme().copy(cornerRadius = 4.dp)) { … }
 */
@Immutable
data class SyteTheme(
    val accent: Color,
    /** Text and icons on top of [accent]. */
    val onAccent: Color,
    val background: Color,
    val imageBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val salePrice: Color,
    val titleStyle: TextStyle,
    val brandStyle: TextStyle,
    val priceStyle: TextStyle,
    val headerStyle: TextStyle,
    val cornerRadius: Dp = 10.dp,
    val spacing: Dp = 12.dp,
    /** Space between the screen edge and the content. */
    val edgeInset: Dp = 16.dp,
    /** Product image width divided by height. Fashion catalogs are usually 3:4. */
    val imageAspectRatio: Float = 3f / 4f,
    /** Card width in carousels. */
    val carouselCardWidth: Dp = 148.dp,
    /** Smallest card width in grids. Grids fit as many columns as this allows. */
    val gridMinCardWidth: Dp = 150.dp,
    val strings: SyteUiStrings = SyteUiStrings(),
) {

    companion object {

        /** Colors and type from the surrounding `MaterialTheme`, so dark mode follows the app. */
        @Composable
        fun fromMaterialTheme(): SyteTheme {
            val colors = MaterialTheme.colorScheme
            val type = MaterialTheme.typography
            return SyteTheme(
                accent = colors.primary,
                onAccent = colors.onPrimary,
                background = colors.background,
                imageBackground = colors.surfaceVariant,
                primaryText = colors.onBackground,
                secondaryText = colors.onSurfaceVariant,
                salePrice = colors.error,
                titleStyle = type.bodyMedium,
                brandStyle = type.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                priceStyle = type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                headerStyle = type.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Every piece of text the kit shows, in English by default. */
@Immutable
data class SyteUiStrings(
    val searchPlaceholder: String = "Search",
    val popularSearches: String = "Popular searches",
    val recentSearches: String = "Recent searches",
    /** `%s` is the query. */
    val noResults: String = "No results for “%s”",
    /** `%d` is the number of results. */
    val resultCount: String = "%d results",
    val clearSearch: String = "Clear",
    val tryAgain: String = "Try again",
    val visualSearchTitle: String = "Search with a photo",
    val visualSearchHint: String = "Take or choose a photo and we'll find products like the ones in it.",
    val takePhoto: String = "Take photo",
    val choosePhoto: String = "Choose photo",
    val newPhoto: String = "New photo",
    val findingItems: String = "Finding items…",
    val nothingFound: String = "We couldn't find any products in this photo.",
)

/** The service the Syte views use. Set it with [SyteUi]. */
val LocalSyteService = staticCompositionLocalOf<SyteUiService?> { null }

/** The theme the Syte views use. Set it with [SyteUi]. */
val LocalSyteTheme = staticCompositionLocalOf<SyteTheme?> { null }

/**
 * Connects and styles every Syte view inside [content]. Create the service once, for example in
 * your `Application` or `Activity`, rather than inside a composable.
 *
 * @param theme Leave out to follow the app's `MaterialTheme`.
 */
@Composable
fun SyteUi(service: SyteUiService, theme: SyteTheme? = null, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSyteService provides service,
        LocalSyteTheme provides (theme ?: LocalSyteTheme.current),
        content = content,
    )
}

@Composable
internal fun currentSyteTheme(): SyteTheme = LocalSyteTheme.current ?: SyteTheme.fromMaterialTheme()

internal const val NO_SERVICE = "No Syte service. Wrap your content in SyteUi(service) { … }."
