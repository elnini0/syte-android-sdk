## Syte Android SDK

Client for Syte's product discovery API: visual search, similar items, Shop the Look, personalized recommendations, text search and event tracking. The SDK returns data for your own screens. The optional UI kit adds ready-made Jetpack Compose screens on top of it, see [UI kit](#ui-kit).

Requires Android 5.0 (API 21) or higher. Built with Android Gradle Plugin 9.5 against Android 16 (API 36). See [CHANGELOG.md](CHANGELOG.md) for what changed in 2.0.

## Set up

1. Add the JitPack repository in `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. Add the SDK dependency:

```groovy
dependencies {
    implementation 'com.github.elnini0:syte-android-sdk:2.0.0'
}
```

Shrinking with R8 (`minifyEnabled true`) needs no extra rules: the library ships its own.

## UI kit

`syte-ui` is a second library in this repo: Jetpack Compose views that call Syte, show the results, and send Syte's events on their own. It includes the SDK, so add it instead of the SDK line above:

```groovy
dependencies {
    implementation 'com.github.elnini0.syte-android-sdk:syte-android-ui:2.0.0'
}
```

Needs Android 6.0 (API 23) or higher, and your app must compile against API 37, which current Compose libraries require.

- `SyteProductCarousel` is a titled row of similar items, Shop the Look, personalized picks, frequently bought together, recently viewed or a merchandising collection. It shows placeholder cards while loading and hides itself when there's nothing to show.
- `SyteVisualSearch` asks for a photo (camera or gallery), marks the items Syte finds, and shows results for the one the shopper taps. Given an image URL, it searches that image right away, for a "find similar" button on a product page.
- `SyteSearch` has a search box with recent and popular searches, suggestions while typing, and a results grid that loads more as the shopper scrolls.
- The pieces they're built from are public too: `SyteProductCard`, `SyteProductImage`, `SytePrice`, and `syteProducts` for adding products to your own `LazyVerticalGrid`.

Create the service once and wrap your content in `SyteUi`:

```kotlin
class ShopActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val syte = Syte.newInstance(SyteConfiguration(this, "<account id>", "<api key>"))
        val service = SyteLiveService(this, syte)
        setContent {
            MaterialTheme {
                SyteUi(service) { ShopScreen() }
            }
        }
    }
}
```

Then drop the views where they belong. Each takes a lambda for when the shopper picks a product:

```kotlin
SyteProductCarousel(SyteRecommendation.Similar(product.sku, product.imageUrl), title = "You may also like") { picked ->
    openProduct(picked.sku)
}

SyteVisualSearch { picked -> openProduct(picked.sku) }

SyteSearch { picked -> openProduct(picked.sku) }
```

Pass your own card to match your product tiles:

```kotlin
SyteProductCarousel(
    SyteRecommendation.BoughtTogether(cart.skus),
    title = "Complete your order",
    card = { product -> MyProductTile(product) },
) { picked -> cart.add(picked.sku) }
```

Colors and type follow your `MaterialTheme`, so dark mode works out of the box. Sizes and every piece of text can be changed:

```kotlin
val theme = SyteTheme.fromMaterialTheme().copy(
    cornerRadius = 4.dp,
    strings = SyteUiStrings(searchPlaceholder = "Rechercher"),
)
SyteUi(service, theme) { ShopScreen() }
```

Photos taken with the camera go through a file provider the kit registers, so the app needs no setup for it. If your app declares the `CAMERA` permission, Android requires it to be granted before the camera opens, so ask for it before showing photo search. Choosing from the gallery uses the system photo picker and needs no permission.

### Events the views send

- Similar items and Shop the Look rows: when the row is shown, and each product click.
- Photo search: camera button shown and tapped, items found, item tapped, results shown, result clicked. Clicks carry the product the search started from and the link clicked, when known.
- Search: when results are shown, with whether the query was typed, a recent search or a popular one.

Personalized, bought together, recently viewed and collection rows send nothing, and neither do clicks on search results: Syte documents no events for them. Page views, add to cart and purchases stay your app's job, as in the [events section](#data-collection).

### Previews and tests

`SytePreviewService` returns made-up products with no network calls and keeps every event it's sent, for previews and unit tests. Products, search box and item markers carry the test tags `syte.product`, `syte.carousel.product`, `syte.search.field` and `syte.marker` for UI tests.

### Demo app

The `demo` module is a small shop built from the kit: search, product pages with all three recommendation rows, photo search and a "For you" tab. It runs on made-up products. To use your catalog, add `syte.accountId`, `syte.apiKey` and optionally `syte.locale` to `local.properties`, which stays out of git.

The demo's walkthrough test visits every screen and saves a screenshot of each step on the device, under the app's files in `walkthrough/`:

```
./gradlew :demo:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.syteAccountId=<account id> \
    -Pandroid.testInstrumentationRunnerArguments.syteApiKey=<api key> \
    -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
adb pull /sdcard/Android/data/com.github.elnini0.sytedemo/files/walkthrough
```

The photo step expects a photo in the device's gallery.

## Running the tests

Offline tests:

```
./gradlew :android-sdk:testDebugUnitTest :syte-ui:testDebugUnitTest
```

Live tests run on a device or emulator against a real account, and are skipped without one:

```
./gradlew :android-sdk:connectedDebugAndroidTest :syte-ui:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.syteAccountId=<account id> \
    -Pandroid.testInstrumentationRunnerArguments.syteApiKey=<api key>
```

Recently viewed, collection and gallery live tests also need `syteTestDiscovery=true`, and `syteTestCollectionId` / `syteTestGalleryId` for the last two.

Optional `syteTestQuery` and `syteTestLang` arguments (defaults `robe` and `fr_FR`) pick a search term that returns products for your account.

## Account Credentials

In order to use this SDK, please contact Syte for your account ID and API key. The `signature` parameter below is your API key.     

## Use the SDK

# Initialization

To start using the Syte SDK, instantiate the SyteConfiguration object and pass your API credentials to its constructor. 
Your credentials can be found in Syte’s Platform - Settings - API Keys.
Then use the created instance to set the locale.

    SyteConfiguration syteConfiguration = new SyteConfiguration(
        context,
        <account_id>,
        <api_signature>
    );
    syteConfiguration.setLocale("en_US");

Then you'll need to initialize Syte class passing the configuration instance:

    Syte syte = Syte.newInstance(syteConfiguration);

API used: https://cdn.syteapi.com/accounts/[account_id]

To retrieve the instance of SytePlatformSettings use the syte.getPlatformSettings() method.

## Image Search

Object detection ("bounds") and a similarity search ("Items") per object detected in the image. 
Search can be performed with an image or image URL.

To use the image Search functionality do the following:

1. Create dedicated class instance and pass the required data.

For Url image search:

    `UrlImageSearch urlImageSearch = new UrlImageSearch(
        <image URL>,
        <Syte product type>
    );`
    
For image search:
    
    `ImageSearch ImageSearch = new ImageSearch(
        <image Uri>
    );`

2. Retrieve bounds:

For Url image search:

    `SyteResult<BoundsResult> result = syte.getBoundsForImageUrl(urlImageSearch);`
    
For image search:

    `SyteResult<BoundsResult> result = syte.getBoundsForImage(context, imageSearch);`

3. Retrieve Items for a bound:

    `SyteResult<ItemsResult> syte.getItemsForBound(result.data.getBounds().get(index), null);`

You can pass CropCoordinates instance instead of *null* here to enable the crop functionality. Example:

    CropCoordinates coordinates = new CropCoordinates(0.2, 0.2, 0.8, 0.8);
    // The coordinates should be relative ranging from 0.0 to 1.0
        // CropCoordinates class reflects two points of the image: top left and bottom right.
    SyteResult<ItemsResult> syte.getItems(result.data.getBounds().get(index), coordinates);

**NOTE**
Items for the first bound will be retrieved by default.
Use **urlImageSearch.setRetrieveItemsForTheFirstBound(false)**  or **imageSearch.setRetrieveItemsForTheFirstBound(false)** to disable this behaviour.
To get the items for the first bound use the BoundsResult.getItemsForFirstBound() method.

# Product Recommendations
To use the "Recommendations" functionality, do the following:

1. Use Syte class instance to get results for different features:

*   `SyteResult<SimilarProductsResult> getSimilarProducts(
        SimilarItems similarItems
    );`
*   `SyteResult<ShopTheLookResult> getShopTheLook(
        ShopTheLook shopTheLook
    );`
*   `SyteResult<PersonalizationResult> getPersonalizedProducts(
        Personalization personalization
    );`
*   `SyteResult<PersonalizationResult> getBoughtTogether(
        BoughtTogether boughtTogether
    );`

Each has an `...Async` version that takes a `SyteCallback`.

"Frequently bought together" takes the SKUs in the shopper's cart, or the product they're looking at:

```java
SyteResult<PersonalizationResult> result =
        syte.getBoughtTogether(new BoughtTogether(Collections.singletonList("ABC123")));
```
    
**NOTE:** You must add at least one product ID to use the "Personalization" functionality. To do this use the **Syte.addViewedProduct(String)** method.

# Recently viewed, collections and galleries

These calls read data Syte keeps on its side, and need the features enabled on your account. They have not been tested against a live account yet: the test account available while building them answers 403.

Recently viewed products for this SDK's shopper ID, most recent first:
```java
SyteResult<RecentlyViewedResult> viewed = syte.getRecentlyViewed(new RecentlyViewed());
syte.removeRecentlyViewed("ABC123");
syte.mergeRecentlyViewed("<another shopper ID>");
```
Syte builds this list from the shopper's activity; check with Syte which events feed it in an app.

A merchandising collection's products, a page at a time:
```java
MerchandisingCollection collection = new MerchandisingCollection("<collection id>");
collection.setSize(12);
SyteResult<CollectionResult> page = syte.getCollection(collection);
```

A shoppable gallery, with the products tagged on each image:
```java
SyteResult<ShoppableGalleryResult> gallery = syte.getShoppableGallery(new ShoppableGallery("<gallery id>"));
```

Each has an `...Async` version that takes a `SyteCallback`. Products from these calls come back as `SyteProduct`, with getters for common fields and everything else in `getFields()`. Ask for other product fields with the request's `setFields`.

# Personalized ranking

Enabling the personalized ranking will attach the list of viewed products to the requests. 
To add a product to the list of viewed ones use the **Syte.addViewedProduct(String)** method.
To enable this functionality use the **setPersonalizedRanking(true)** method. 
It is supported in the following classes: **UrlImageSearch, ImageSearch, ShopTheLook, SimilarProducts**.
Personalized ranking is disabled by default.

# Data Collection

The SDK can be used to fire various events to Syte. Example:

    syte.fireEvent(new EventCheckoutStart(62.94, "EUR", products, "checkout"));

# Text Search

The SDK can be used for the Text Search functionality.

There are 3 main features:

1. Popular Searches. Will retrieve the list of the most popular searches.

    `syte.getPopularSearches("en_US");`

2. Text search. Will retrieve the results for the specified query.

    `syte.getTextSearch(TextSearch textSearch);`

    To retrieve a list of recent text searches use syte.getRecentTextSearches() method.

3. Auto-complete. Text auto-completion functionality.

    `syte.getAutoCompleteForTextSearch("query", "en_US", syteResult -> {`
        `// Process the results here.`
    `});`
