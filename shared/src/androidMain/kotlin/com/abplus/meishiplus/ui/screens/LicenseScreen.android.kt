package com.abplus.meishiplus.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val LicenseAssetUrl = "file:///android_asset/license.html"

@Composable
actual fun LicenseContent(
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                loadUrl(LicenseAssetUrl)
            }
        },
    )
}
