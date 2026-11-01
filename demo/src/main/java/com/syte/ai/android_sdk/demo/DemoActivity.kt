package com.syte.ai.android_sdk.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.syte.ai.android_sdk.core.Syte
import com.syte.ai.android_sdk.core.SyteConfiguration
import com.syte.ai.android_sdk.ui.SyteLiveService
import com.syte.ai.android_sdk.ui.SytePreviewService
import com.syte.ai.android_sdk.ui.SyteTheme
import com.syte.ai.android_sdk.ui.SyteUi
import com.syte.ai.android_sdk.ui.SyteUiService

/**
 * A small shop built from the UI kit. Runs on made-up products unless an account is passed in,
 * either in local.properties (syte.accountId, syte.apiKey, syte.locale) or as launch extras
 * (syte_account_id, syte_api_key, syte_locale).
 */
class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val (service, isLive) = DemoServices.get(this, intent)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Teal.copy(alpha = 0.9f)) else lightColorScheme(primary = Teal)) {
                // Shows a retailer's own colors and corners in place of the defaults.
                val theme = SyteTheme.fromMaterialTheme().copy(salePrice = Color(0xFFBF382B), cornerRadius = 6.dp)
                SyteUi(service, theme) {
                    DemoApp(isLive)
                }
            }
        }
    }

    private companion object {
        val Teal = Color(0xFF1C5C54)
    }
}

/** One service for the app's lifetime, so rotating the screen doesn't start over. */
private object DemoServices {

    private var cached: Pair<SyteUiService, Boolean>? = null

    fun get(context: Context, intent: Intent): Pair<SyteUiService, Boolean> = cached ?: create(context, intent).also { cached = it }

    private fun create(context: Context, intent: Intent): Pair<SyteUiService, Boolean> {
        val account = intent.getStringExtra("syte_account_id") ?: BuildConfig.SYTE_ACCOUNT_ID
        val key = intent.getStringExtra("syte_api_key") ?: BuildConfig.SYTE_API_KEY
        val locale = intent.getStringExtra("syte_locale") ?: BuildConfig.SYTE_LOCALE
        if (account.isEmpty() || key.isEmpty()) return SytePreviewService() to false
        val configuration = SyteConfiguration(context.applicationContext, account, key).apply { setLocale(locale) }
        return SyteLiveService(context, Syte.newInstance(configuration)) to true
    }
}
