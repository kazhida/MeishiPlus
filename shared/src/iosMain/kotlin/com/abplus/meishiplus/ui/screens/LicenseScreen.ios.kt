package com.abplus.meishiplus.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import meishiplus.shared.generated.resources.Res
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@Composable
actual fun LicenseContent(
    modifier: Modifier,
) {
    UIKitView(
        modifier = modifier,
        factory = {
            WKWebView().apply {
                val url = NSURL.URLWithString(Res.getUri("files/license.html"))
                if (url != null) {
                    loadRequest(NSURLRequest.requestWithURL(url))
                } else {
                    loadHTMLString(
                        string = "<html><body><p>license.html could not be loaded.</p></body></html>",
                        baseURL = null,
                    )
                }
            }
        },
    )
}
