package com.abplus.meishiplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfPreview(
    filePath: String,
    modifier: Modifier = Modifier,
    rotateClockwise: Boolean = false,
    fillBounds: Boolean = false,
)
