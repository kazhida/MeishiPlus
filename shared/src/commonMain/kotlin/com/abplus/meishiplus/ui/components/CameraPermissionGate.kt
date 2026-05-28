package com.abplus.meishiplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
