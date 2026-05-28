package com.abplus.meishiplus.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.abplus.meishiplus.data.entities.CardEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.max
import androidx.core.net.toUri
import androidx.core.graphics.withRotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AndroidCardPdfContext {
    lateinit var applicationContext: Context
}

actual fun deletePdfFileQuietly(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (file.exists() && !file.delete()) {
            Log.w("CardPdfExporter", "Failed to delete PDF file: $filePath")
        }
    }.onFailure { throwable ->
        Log.w("CardPdfExporter", "Failed to delete PDF file: $filePath", throwable)
    }
}

actual suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    val context = AndroidCardPdfContext.applicationContext
    val outputFile = File(
        context.cacheDir,
        "meishi-${cardEntity.id.ifBlank { "card" }}-${System.currentTimeMillis()}.pdf",
    )

    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(CardPdfWidth, CardPdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        drawCardPdf(
            canvas = page.canvas,
            cardEntity = cardEntity,
            rect = RectF(0f, 0f, CardPdfWidth.toFloat(), CardPdfHeight.toFloat()),
            drawBorder = false,
            loadBitmap = { uri -> context.loadBitmapFromUri(uri) },
        )
        document.finishPage(page)
        withContext(Dispatchers.IO) {
            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
        }
    } finally {
        document.close()
    }

    return CardPdfExportResult(filePath = outputFile.absolutePath)
}

actual suspend fun createPostcardCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    val context = AndroidCardPdfContext.applicationContext
    val outputFile = File(
        context.cacheDir,
        "meishi-postcard-${cardEntity.id.ifBlank { "card" }}-${System.currentTimeMillis()}.pdf",
    )

    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(PostcardPdfWidth, PostcardPdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)

        val cardWidth = mmToPdfPoints(91f)
        val cardHeight = mmToPdfPoints(55f)
        val left = (PostcardPdfWidth - cardWidth) / 2f
        val gap = mmToPdfPoints(12f)
        val top = (PostcardPdfHeight - cardHeight * 2f - gap) / 2f

        repeat(2) { index ->
            val cardTop = top + index * (cardHeight + gap)
            drawCardPdf(
                canvas = page.canvas,
                cardEntity = cardEntity,
                rect = RectF(left, cardTop, left + cardWidth, cardTop + cardHeight),
                drawBorder = true,
                loadBitmap = { uri -> context.loadBitmapFromUri(uri) },
            )
        }

        document.finishPage(page)
        withContext(Dispatchers.IO) {
            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
        }
    } finally {
        document.close()
    }

    context.openPdfFile(outputFile)
    return CardPdfExportResult(filePath = outputFile.absolutePath)
}

actual suspend fun createA4CardPdf(
    cardEntity: CardEntity,
    topMarginMm: Float,
    bottomMarginMm: Float,
    leftMarginMm: Float,
    rightMarginMm: Float,
): CardPdfExportResult {
    val context = AndroidCardPdfContext.applicationContext
    val outputFile = File(
        context.cacheDir,
        "meishi-a4-${cardEntity.id.ifBlank { "card" }}-${System.currentTimeMillis()}.pdf",
    )

    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(A4PdfWidth, A4PdfHeight, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)

        val leftMargin = mmToPdfPoints(leftMarginMm.coerceAtLeast(0f))
        val rightMargin = mmToPdfPoints(rightMarginMm.coerceAtLeast(0f))
        val topMargin = mmToPdfPoints(topMarginMm.coerceAtLeast(0f))
        val bottomMargin = mmToPdfPoints(bottomMarginMm.coerceAtLeast(0f))
        val printableLeft = leftMargin
        val printableTop = topMargin
        val printableWidth = (A4PdfWidth - leftMargin - rightMargin).coerceAtLeast(0f)
        val printableHeight = (A4PdfHeight - topMargin - bottomMargin).coerceAtLeast(0f)
        val cellWidth = printableWidth / A4Columns
        val cellHeight = printableHeight / A4Rows
        val cardAspectWidth = 91f
        val cardAspectHeight = 55f
        val scale = min(cellWidth / cardAspectWidth, cellHeight / cardAspectHeight).coerceAtLeast(0f)
        val cardWidth = cardAspectWidth * scale
        val cardHeight = cardAspectHeight * scale

        repeat(A4Rows) { row ->
            repeat(A4Columns) { column ->
                val cellLeft = printableLeft + column * cellWidth
                val cellTop = printableTop + row * cellHeight
                val cardLeft = cellLeft + (cellWidth - cardWidth) / 2f
                val cardTop = cellTop + (cellHeight - cardHeight) / 2f
                drawCardPdf(
                    canvas = page.canvas,
                    cardEntity = cardEntity,
                    rect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight),
                    drawBorder = true,
                    loadBitmap = { uri -> context.loadBitmapFromUri(uri) },
                )
            }
        }

        document.finishPage(page)
        withContext(Dispatchers.IO) {
            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
        }
    } finally {
        document.close()
    }

    context.openPdfFile(outputFile)
    return CardPdfExportResult(filePath = outputFile.absolutePath)
}

private fun drawCardPdf(
    canvas: Canvas,
    cardEntity: CardEntity,
    rect: RectF,
    drawBorder: Boolean,
    loadBitmap: (String) -> Bitmap?,
) {
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        canvas.drawRect(rect, this)
    }

    if (cardEntity.bgFile.isNotBlank()) {
        loadBitmap(cardEntity.bgFile)?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, bitmap.centerCropRect(rect), Paint(Paint.ANTI_ALIAS_FLAG))
        }
    }

    if (cardEntity.bgAlpha > 0f) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (cardEntity.bgAlpha.coerceIn(0f, 1f) * 255).toInt()
            style = Paint.Style.FILL
            canvas.drawRect(rect, this)
        }
    }

    drawCardText(canvas, rect, cardEntity.organization, CardPdfColors.Primary, Typeface.DEFAULT)
    drawCardText(canvas, rect, cardEntity.title, CardPdfColors.Secondary, Typeface.DEFAULT)
    drawCardText(canvas, rect, cardEntity.name, Color.BLACK, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
    if (cardEntity.phone.value.isNotBlank()) {
        drawCardText(canvas, rect, cardEntity.phone.labelElement("TEL"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(canvas, rect, cardEntity.phone, Color.BLACK, Typeface.DEFAULT)
    }
    if (cardEntity.email.value.isNotBlank()) {
        drawCardText(canvas, rect, cardEntity.email.labelElement("MAIL"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(canvas, rect, cardEntity.email, Color.BLACK, Typeface.DEFAULT)
    }
    if (cardEntity.address1.value.isNotBlank() || cardEntity.address2.value.isNotBlank()) {
        drawCardText(canvas, rect, cardEntity.address1.labelElement("ADDR"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(
            canvas = canvas,
            rect = rect,
            element = cardEntity.address2.copy(
                value = listOf(cardEntity.address1.value, cardEntity.address2.value)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            ),
            color = Color.BLACK,
            typeface = Typeface.DEFAULT,
        )
    }

    if (drawBorder) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CardPdfColors.Border
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            canvas.drawRect(rect, this)
        }
    }
}

private fun drawCardText(
    canvas: Canvas,
    rect: RectF,
    element: CardEntity.CardElement,
    color: Int,
    typeface: Typeface,
) {
    if (element.value.isBlank()) return

    val x = rect.left + rect.width() * element.x
    val y = rect.top + rect.height() * element.y
    val scale = rect.width() / CardPdfWidth
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.typeface = typeface
        textSize = element.fontSize * CardPdfFontScale * scale
    }

    canvas.withRotation(element.rotation.toFloat(), x, y) {
        element.value.lines().forEachIndexed { index, line ->
            drawText(line, x, y + paint.textSize + index * paint.textSize * 1.25f, paint)
        }
    }
}

private fun CardEntity.CardElement.labelElement(label: String): CardEntity.CardElement = copy(
    value = label,
    x = max(0f, x - ContactLabelXOffsetRatio),
)

private fun Context.loadBitmapFromUri(uri: String): Bitmap? {
    return runCatching {
        val assetPrefix = "file:///android_asset/"
        if (uri.startsWith(assetPrefix)) {
            assets.open(uri.removePrefix(assetPrefix)).use(BitmapFactory::decodeStream)
        } else {
            contentResolver.openInputStream(uri.toUri())?.use(BitmapFactory::decodeStream)
        }
    }.getOrNull()
}

private fun Bitmap.centerCropRect(bounds: RectF): RectF {
    val scale = max(bounds.width() / width, bounds.height() / height)
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val left = bounds.left + (bounds.width() - scaledWidth) / 2f
    val top = bounds.top + (bounds.height() - scaledHeight) / 2f
    return RectF(left, top, left + scaledWidth, top + scaledHeight)
}

private fun Context.openPdfFile(file: File) {
    val uri = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(Intent.createChooser(intent, "PDFを開く").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private const val CardPdfWidth = 910
private const val CardPdfHeight = 550
private const val CardPdfFontScale = 2.5f
private const val ContactLabelXOffsetRatio = 0.08f
private val PostcardPdfWidth = mmToPdfPoints(100f).toInt()
private val PostcardPdfHeight = mmToPdfPoints(148f).toInt()
private val A4PdfWidth = mmToPdfPoints(210f).toInt()
private val A4PdfHeight = mmToPdfPoints(297f).toInt()
private const val A4Columns = 2
private const val A4Rows = 5

private fun mmToPdfPoints(mm: Float): Float = mm / 25.4f * 72f

private object CardPdfColors {
    const val Primary: Int = 0xFF00AFAF.toInt()
    const val Secondary: Int = 0xFF666666.toInt()
    const val Border: Int = 0xFFD9D9D9.toInt()
}
