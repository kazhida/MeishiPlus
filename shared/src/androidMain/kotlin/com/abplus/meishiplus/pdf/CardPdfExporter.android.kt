package com.abplus.meishiplus.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.abplus.meishiplus.data.entities.CardEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object AndroidCardPdfContext {
    lateinit var applicationContext: Context
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
            loadBitmap = { uri -> context.loadBitmapFromUri(uri) },
        )
        document.finishPage(page)
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return CardPdfExportResult(filePath = outputFile.absolutePath)
}

private fun drawCardPdf(
    canvas: Canvas,
    cardEntity: CardEntity,
    loadBitmap: (String) -> Bitmap?,
) {
    val pageRect = RectF(0f, 0f, CardPdfWidth.toFloat(), CardPdfHeight.toFloat())
    canvas.drawColor(Color.WHITE)

    if (cardEntity.bgFile.isNotBlank()) {
        loadBitmap(cardEntity.bgFile)?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, pageRect, Paint(Paint.ANTI_ALIAS_FLAG))
        }
    }

    if (cardEntity.bgAlpha > 0f) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (cardEntity.bgAlpha.coerceIn(0f, 1f) * 255).toInt()
            style = Paint.Style.FILL
            canvas.drawRect(pageRect, this)
        }
    }

    drawCardText(canvas, cardEntity.organization, CardPdfColors.Primary, Typeface.DEFAULT)
    drawCardText(canvas, cardEntity.title, CardPdfColors.Secondary, Typeface.DEFAULT)
    drawCardText(canvas, cardEntity.name, Color.BLACK, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
    if (cardEntity.phone.value.isNotBlank()) {
        drawCardText(canvas, cardEntity.phone.labelElement("TEL"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(canvas, cardEntity.phone, Color.BLACK, Typeface.DEFAULT)
    }
    if (cardEntity.email.value.isNotBlank()) {
        drawCardText(canvas, cardEntity.email.labelElement("MAIL"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(canvas, cardEntity.email, Color.BLACK, Typeface.DEFAULT)
    }
    if (cardEntity.address1.value.isNotBlank() || cardEntity.address2.value.isNotBlank()) {
        drawCardText(canvas, cardEntity.address1.labelElement("ADDR"), CardPdfColors.Secondary, Typeface.DEFAULT)
        drawCardText(
            canvas = canvas,
            element = cardEntity.address2.copy(
                value = listOf(cardEntity.address1.value, cardEntity.address2.value)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            ),
            color = Color.BLACK,
            typeface = Typeface.DEFAULT,
        )
    }
}

private fun drawCardText(
    canvas: Canvas,
    element: CardEntity.CardElement,
    color: Int,
    typeface: Typeface,
) {
    if (element.value.isBlank()) return

    val x = CardPdfWidth * element.x
    val y = CardPdfHeight * element.y
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.typeface = typeface
        textSize = element.fontSize * CardPdfFontScale
    }

    canvas.save()
    canvas.rotate(element.rotation.toFloat(), x, y)
    element.value.lines().forEachIndexed { index, line ->
        canvas.drawText(line, x, y + paint.textSize + index * paint.textSize * 1.25f, paint)
    }
    canvas.restore()
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
            contentResolver.openInputStream(android.net.Uri.parse(uri))?.use(BitmapFactory::decodeStream)
        }
    }.getOrNull()
}

private const val CardPdfWidth = 910
private const val CardPdfHeight = 550
private const val CardPdfFontScale = 2.5f
private const val ContactLabelXOffsetRatio = 0.08f

private object CardPdfColors {
    const val Primary: Int = 0xFF00AFAF.toInt()
    const val Secondary: Int = 0xFF666666.toInt()
}
