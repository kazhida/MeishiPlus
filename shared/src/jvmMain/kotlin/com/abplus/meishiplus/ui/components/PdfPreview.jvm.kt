package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun PdfPreview(
    filePath: String,
    modifier: Modifier,
    rotateClockwise: Boolean,
    fillBounds: Boolean,
) {
    Box(modifier = modifier) {
        Text(
            text = filePath,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
