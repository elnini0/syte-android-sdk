## Syte Android SDK

Data-only client for Syte's product discovery API: visual search, similar items, Shop the Look, personalized recommendations, text search and event tracking. It returns data; your app draws the screens.

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

## Running the tests

Offline tests:

```
./gradlew :android-sdk:testDebugUnitTest
```

Live tests run on a device or emulator against a real account, and are skipped without one:

```
./gradlew :android-sdk:connectedDebugAndroidTest \
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

    syte.fireEvent(new EventCheckoutStart());

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
