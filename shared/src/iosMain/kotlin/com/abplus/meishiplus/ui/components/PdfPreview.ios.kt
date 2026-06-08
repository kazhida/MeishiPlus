package com.abplus.meishiplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    UIKitView(
        factory = {
            PDFView().apply {
                autoScales = true
                backgroundColor = UIColor.whiteColor
                document = loadPreviewDocument(filePath, rotateClockwise)
            }
        },
        modifier = modifier,
        update = { view ->
            view.document = loadPreviewDocument(filePath, rotateClockwise)
            view.autoScales = true
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun loadPreviewDocument(
    filePath: String,
    rotateClockwise: Boolean,
): PDFDocument {
    return PDFDocument(NSURL.fileURLWithPath(filePath)).also { document ->
        if (rotateClockwise) {
            document.pageAtIndex(0u)?.rotation = 90
        }
    }
}
