package com.syte.ai.android_sdk.ui

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.syte.ai.android_sdk.enums.Placement
import com.syte.ai.android_sdk.events.EventCameraButtonClick
import com.syte.ai.android_sdk.events.EventCameraButtonImpression
import java.io.File

/**
 * Photo search, start to finish: take or choose a photo, tap one of the items Syte finds in it,
 * browse similar products. Started with [imageUrl], it searches that image right away, which fits
 * a "find similar" button on a product image.
 *
 * Fills the space it's given and scrolls on its own. Without a camera, only choosing a photo is
 * offered. If your app declares the CAMERA permission, ask for it before showing this view.
 *
 * @param sku The product [imageUrl] belongs to, so Syte can tie clicks back to it.
 */
@Composable
fun SyteVisualSearch(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    sku: String? = null,
    pageName: String = "mobile_sdk",
    card: @Composable (SyteUiProduct) -> Unit = { SyteProductCard(it) },
    onSelect: (SyteUiProduct) -> Unit = {},
) {
    val service = LocalSyteService.current
    val theme = currentSyteTheme()
    val context = LocalContext.current
    val model: VisualSearchModel = viewModel(key = "syte.visual.$pageName.${imageUrl.orEmpty()}") { VisualSearchModel() }
    SideEffect {
        model.service = service
        model.pageName = pageName
    }

    var cameraFile by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        cameraFile?.takeIf { saved }?.let(model::searchPhoto)
    }
    val library = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(model::searchPhoto)
    }

    LaunchedEffect(imageUrl) {
        model.service = service
        model.pageName = pageName
        if (imageUrl != null && model.source == null) model.searchUrl(imageUrl, sku)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(theme.gridMinCardWidth),
        modifier = modifier.fillMaxSize().background(theme.background),
        contentPadding = PaddingValues(theme.edgeInset),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing),
        verticalArrangement = Arrangement.spacedBy(theme.spacing * 2),
    ) {
        val source = model.source
        if (source == null) {
            fullWidth {
                PhotoPrompt(
                    canTakePhoto = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
                    onTakePhoto = {
                        service?.track(EventCameraButtonClick(Placement.DEFAULT, pageName))
                        val file = newPhotoFile(context)
                        cameraFile = file
                        camera.launch(file)
                    },
                    onChoosePhoto = {
                        service?.track(EventCameraButtonClick(Placement.DEFAULT, pageName))
                        library.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onShown = { service?.track(EventCameraButtonImpression(pageName)) },
                )
            }
            return@LazyVerticalGrid
        }

        val items = model.detection?.items.orEmpty()
        fullWidth {
            DetectionImage(source, items, model.selected, model.phase == VisualSearchModel.Phase.Detecting, model::select)
        }
        if (items.size > 1) {
            fullWidth { ItemChips(items, model.selected, model::select) }
        }
        fullWidth {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val status = when {
                    model.phase == VisualSearchModel.Phase.Detecting -> theme.strings.findingItems
                    model.phase == VisualSearchModel.Phase.Detected && items.isEmpty() -> theme.strings.nothingFound
                    else -> null
                }
                Text(status.orEmpty(), style = theme.titleStyle, color = theme.secondaryText, modifier = Modifier.weight(1f))
                TextButton(onClick = model::reset) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                    Text(theme.strings.newPhoto, color = theme.accent, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        val phase = model.phase
        val resultsError = model.resultsError
        when {
            phase is VisualSearchModel.Phase.Failed -> fullWidth { SyteErrorView(phase.message, model::retry) }
            resultsError != null -> fullWidth { SyteErrorView(resultsError, model::retry) }
            model.loadingResults -> fullWidth {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accent)
                }
            }
            else -> syteProducts(model.results, onSelect = { index, product ->
                model.resultTapped(product, index)
                onSelect(product)
            }, card = card)
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** A file in the app's cache for the camera app to save into, shared through the kit's file provider. */
private fun newPhotoFile(context: Context): Uri {
    val dir = File(context.cacheDir, "syte_photos").apply { mkdirs() }
    val file = File(dir, "photo-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.syte.photos", file)
}

@Composable
private fun PhotoPrompt(canTakePhoto: Boolean, onTakePhoto: () -> Unit, onChoosePhoto: () -> Unit, onShown: () -> Unit) {
    val theme = currentSyteTheme()
    LaunchedEffect(Unit) { onShown() }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(theme.spacing),
    ) {
        Icon(SyteIcons.Camera, contentDescription = null, tint = theme.accent, modifier = Modifier.size(48.dp))
        Text(theme.strings.visualSearchTitle, style = theme.headerStyle, color = theme.primaryText)
        Text(theme.strings.visualSearchHint, style = theme.titleStyle, color = theme.secondaryText, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing), modifier = Modifier.padding(top = 8.dp)) {
            if (canTakePhoto) PhotoButton(theme.strings.takePhoto, SyteIcons.Camera, onTakePhoto)
            PhotoButton(theme.strings.choosePhoto, SyteIcons.Photo, onChoosePhoto)
        }
    }
}

@Composable
private fun PhotoButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val theme = currentSyteTheme()
    Row(
        Modifier
            .clip(CircleShape)
            .background(theme.accent.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
        Text(label, style = theme.titleStyle.copy(fontWeight = FontWeight.SemiBold), color = theme.accent)
    }
}

/** The searched image with a tappable marker on each item found. */
@Composable
private fun DetectionImage(
    source: VisualSearchModel.Source,
    items: List<SyteDetectedItem>,
    selected: Int?,
    detecting: Boolean,
    onTap: (SyteDetectedItem) -> Unit,
) {
    val theme = currentSyteTheme()
    var ratio by remember(source) { mutableStateOf<Float?>(null) }
    val model: Any = when (source) {
        is VisualSearchModel.Source.Photo -> source.uri
        is VisualSearchModel.Source.Url -> source.url
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Sized to the image's own proportions, so marker positions line up with what's drawn.
        Box(
            Modifier
                .heightIn(max = 440.dp)
                .aspectRatio(ratio ?: theme.imageAspectRatio)
                .clip(RoundedCornerShape(theme.cornerRadius))
                .background(theme.imageBackground),
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    val image = state.result.image
                    if (image.width > 0 && image.height > 0) ratio = image.width.toFloat() / image.height
                },
            )
            if (ratio != null) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    items.forEach { item ->
                        Marker(
                            selected = item.index == selected,
                            modifier = Modifier
                                .offset(x = maxWidth * item.centerX - 22.dp, y = maxHeight * item.centerY - 22.dp)
                                .testTag("syte.marker")
                                .semantics {
                                    contentDescription = displayName(item)
                                    this.selected = item.index == selected
                                }
                                .clickable(role = Role.Button) { onTap(item) },
                        )
                    }
                }
            }
            if (detecting) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Marker(selected: Boolean, modifier: Modifier) {
    val theme = currentSyteTheme()
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(if (selected) 28.dp else 24.dp)
                .shadow(4.dp, CircleShape)
                .background(if (selected) theme.accent else Color.White, CircleShape)
                .border(3.dp, Color.White, CircleShape),
        )
    }
}

@Composable
private fun ItemChips(items: List<SyteDetectedItem>, selected: Int?, onTap: (SyteDetectedItem) -> Unit) {
    val theme = currentSyteTheme()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val isSelected = item.index == selected
            Text(
                displayName(item),
                style = theme.titleStyle,
                color = if (isSelected) theme.onAccent else theme.primaryText,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) theme.accent else theme.imageBackground)
                    .clickable(role = Role.Button) { onTap(item) }
                    .semantics { this.selected = isSelected }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

private fun displayName(item: SyteDetectedItem): String =
    if (item.label.isEmpty()) "Item ${item.index + 1}" else item.label.replaceFirstChar { it.uppercase() }
