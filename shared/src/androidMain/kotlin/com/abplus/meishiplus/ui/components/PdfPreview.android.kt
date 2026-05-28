package com.abplus.meishiplus.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun PdfPreview(
    filePath: String,
    modifier: Modifier,
    rotateClockwise: Boolean,
    fillBounds: Boolean,
) {
    var bitmap by remember(filePath, rotateClockwise) { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember(filePath, rotateClockwise) { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath, rotateClockwise) {
        bitmap = null
        errorMessage = null
        runCatching {
            withContext(Dispatchers.IO) {
                renderFirstPdfPage(filePath, rotateClockwise)
            }
        }.onSuccess { rendered ->
            bitmap = rendered
        }.onFailure { throwable ->
            errorMessage = throwable.message ?: "PDFの表示に失敗しました"
        }
    }

    Box(modifier = modifier) {
        when {
            errorMessage != null -> Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
            bitmap == null -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = if (fillBounds) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun renderFirstPdfPage(filePath: String, rotateClockwise: Boolean): Bitmap {
    val descriptor = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
    descriptor.use {
        PdfRenderer(it).use { renderer ->
            renderer.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return if (rotateClockwise) bitmap.rotatedClockwise() else bitmap
            }
        }
    }
}

private fun Bitmap.rotatedClockwise(): Bitmap {
    val matrix = Matrix().apply {
        postRotate(90f)
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also {
        if (it != this) recycle()
    }
}
