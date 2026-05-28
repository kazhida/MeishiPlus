package com.abplus.meishiplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfPreview(
    filePath: String,
    modifier: Modifier,
    rotateClockwise: Boolean,
    fillBounds: Boolean,
) {
    val previewModifier = if (rotateClockwise) {
        modifier.graphicsLayer {
            rotationZ = 90f
            scaleX = if (fillBounds) 1.4f else 1f
            scaleY = if (fillBounds) 1.4f else 1f
        }
    } else {
        modifier
    }

    UIKitView(
        factory = {
            PDFView().apply {
                autoScales = true
                backgroundColor = UIColor.whiteColor
                document = PDFDocument(NSURL.fileURLWithPath(filePath))
            }
        },
        modifier = previewModifier,
        update = { view ->
            view.document = PDFDocument(NSURL.fileURLWithPath(filePath))
        },
    )
}
