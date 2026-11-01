package com.syte.ai.android_sdk.demo

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks through the demo and saves a screenshot of every step to the app's files, under
 * walkthrough/. Runs on sample products, or on a real account when syteAccountId and syteApiKey
 * are passed as instrumentation arguments.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DemoWalkthroughTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val args = InstrumentationRegistry.getArguments()
    private val query = args.getString("syteTestQuery", "shirt")

    @Before
    fun launch() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DemoActivity::class.java)
        args.getString("syteAccountId")?.let { intent.putExtra("syte_account_id", it) }
        args.getString("syteApiKey")?.let { intent.putExtra("syte_api_key", it) }
        args.getString("syteTestLang")?.let { intent.putExtra("syte_locale", it) }
        ActivityScenario.launch<DemoActivity>(intent)
    }

    @Test
    fun searchProductPageAndSimilarLooks() {
        val field = compose.onNodeWithTag("syte.search.field")
        field.performTextInput(query.take(3))
        settle(2000)
        snapshot("search-suggestions")

        field.performTextInput(query.drop(3))
        field.performImeAction()
        compose.waitUntilAtLeastOneExists(hasTestTag("syte.product"), 20_000)
        settle(2500)
        snapshot("search-results")

        compose.onAllNodesWithTag("syte.product")[0].performClick()
        compose.waitUntilAtLeastOneExists(hasText("Find similar looks"), 10_000)
        settle(2500)
        snapshot("product-page")
        // The rows start below the fold, and a lazy list only builds what's on screen.
        compose.onNodeWithTag("demo.product").performTouchInput { swipeUp() }
        compose.waitUntilAtLeastOneExists(hasTestTag("syte.carousel.product"), 20_000)
        settle(2500)
        snapshot("product-page-rows")

        compose.onNodeWithTag("demo.product").performTouchInput { swipeDown() }
        compose.waitUntilAtLeastOneExists(hasText("Find similar looks"), 10_000)
        compose.onNodeWithText("Find similar looks").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("syte.marker"), 30_000)
        settle(3000)
        snapshot("similar-looks")
    }

    @Test
    fun photoSearchFromLibrary() {
        compose.onNodeWithText("Photo").performClick()
        compose.onNodeWithText("Choose photo").performClick()
        // The photo picker runs outside the app, so its photos can't be queried. Tap where the
        // newest one sits, top left. Add one first: adb push photo.jpg /sdcard/Pictures/ and scan it.
        Thread.sleep(4000)
        snapshot("photo-picker")
        val device = UiDevice.getInstance(instrumentation)
        device.click((device.displayWidth * 0.17).toInt(), (device.displayHeight * 0.58).toInt())
        compose.waitUntilAtLeastOneExists(hasTestTag("syte.marker"), 30_000)
        settle(3000)
        snapshot("photo-results")
    }

    @Test
    fun forYou() {
        compose.onNodeWithText("For you").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("syte.carousel.product"), 20_000)
        settle(2500)
        snapshot("for-you")
    }

    /**
     * Lets real time pass while still drawing frames. The test controls the app's frame clock,
     * so a plain sleep would leave the screen as it was, images and all.
     */
    private fun settle(millis: Long) {
        val end = SystemClock.uptimeMillis() + millis
        while (SystemClock.uptimeMillis() < end) {
            compose.waitForIdle()
            Thread.sleep(100)
        }
    }

    private fun snapshot(name: String) {
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "walkthrough").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
