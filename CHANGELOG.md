# Changelog

## 2.0.0

Refresh of the 2021 SDK for current Android tooling and the current Syte API.

### Added

- Frequently bought together recommendations.
- Photo search uploads the photo straight to item detection in one call, as Syte's current docs describe, replacing the separate image upload service. Photo searches are reported as camera searches.

### Breaking

- Shopper and session IDs move from encrypted shared preferences to plain app-private preferences. Apps upgrading from 1.x get a new shopper ID once.
- Published as `com.github.elnini0:syte-android-sdk`.

### Fixed

- The project could not build on Apple Silicon Macs without Rosetta, because Android Gradle Plugin 4.2's resource tool is Intel-only. Now on Android Gradle Plugin 9.5, Gradle 9.8 and Java 17.
- Apps that shrink their release build with R8 failed to build, and once fixed the shrinker stripped the SDK's data classes so results came back empty. The library now ships consumer rules and is published unshrunk.
- Purchase, checkout and add-to-cart events now send their data inside `rawData`, as Syte's event API requires. Before, purchases were not counted.
- Autocomplete and popular searches returned an error when there were simply no results. They now return an empty result.
- Search filter values with quotes produced malformed JSON. Filters are now serialized with Gson.
- Search hits now include their `sku`.
- Personalization sent SKUs as unquoted JSON, so any SKU with letters (most retailers' SKUs) got a 400 from the API.
- Sample app builds again on current tooling and Android 12+ (explicit `exported` flag, no `switch` on resource IDs).

### Changed

- Compiles against Android 16 (API 36).
- Calls go to `cdn-api.syteapi.com`, the host in Syte's current API docs.
- Visual search, recommendation and event calls also send the API key in the `x-api-key` header, as the docs describe. Text search keeps the `sig` parameter, since those endpoints reject the header.
- Removed the manual AAR zip distribution; install through JitPack.
- Dropped the deprecated `androidx.security:security-crypto` dependency and the shut-down JCenter repository. Updated Retrofit, Gson, OkHttp and ExifInterface.
- Added offline unit tests, and live device tests that run against a real account when credentials are passed in.
