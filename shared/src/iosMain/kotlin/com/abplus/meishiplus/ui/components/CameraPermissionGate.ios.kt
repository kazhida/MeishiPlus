package com.abplus.meishiplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CameraPermissionGate(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    content()
}
