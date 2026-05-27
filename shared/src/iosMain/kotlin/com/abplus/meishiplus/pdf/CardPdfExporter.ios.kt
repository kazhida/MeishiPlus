package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetAlpha
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSAttributedStringKey
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.drawAtPoint
import kotlin.math.PI
import kotlin.math.max

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    val outputPath = "${cacheDirectoryPath()}/meishi-${cardEntity.id.ifBlank { "card" }}-${currentTimeMillis()}.pdf"
    val bounds = CGRectMake(0.0, 0.0, CardPdfWidth, CardPdfHeight)
    val renderer = UIGraphicsPDFRenderer(bounds = bounds)
    val data = renderer.PDFDataWithActions { context ->
        context?.beginPage()
        drawCardPdf(cardEntity)
    }
    data.writeToFile(outputPath, atomically = true)
    return CardPdfExportResult(filePath = outputPath)
}

@OptIn(ExperimentalForeignApi::class)
private fun drawCardPdf(cardEntity: CardEntity) {
    val rect = CGRectMake(0.0, 0.0, CardPdfWidth, CardPdfHeight)

    UIColor.whiteColor.setFill()
    platform.UIKit.UIRectFill(rect)

    if (cardEntity.bgFile.isNotBlank()) {
        loadImage(cardEntity.bgFile)?.let { image ->
            image.CGImage?.let { cgImage ->
                val context = platform.UIKit.UIGraphicsGetCurrentContext() ?: return@let
                CGContextSaveGState(context)
                CGContextTranslateCTM(context, 0.0, CardPdfHeight)
                CGContextScaleCTM(context, 1.0, -1.0)
                CGContextDrawImage(context, rect, cgImage)
                CGContextRestoreGState(context)
            }
        }
    }

    if (cardEntity.bgAlpha > 0f) {
        val context = platform.UIKit.UIGraphicsGetCurrentContext()
        if (context != null) {
            CGContextSaveGState(context)
            CGContextSetAlpha(context, cardEntity.bgAlpha.coerceIn(0f, 1f).toDouble())
            CGContextSetFillColorWithColor(context, UIColor.whiteColor.CGColor)
            platform.UIKit.UIRectFill(rect)
            CGContextRestoreGState(context)
        }
    }

    drawCardText(cardEntity.organization, UIColor(red = 0.0, green = 0.686, blue = 0.686, alpha = 1.0), false)
    drawCardText(cardEntity.title, UIColor.darkGrayColor, false)
    drawCardText(cardEntity.name, UIColor.blackColor, true)
    if (cardEntity.phone.value.isNotBlank()) {
        drawCardText(cardEntity.phone.labelElement("TEL"), UIColor.darkGrayColor, false)
        drawCardText(cardEntity.phone, UIColor.blackColor, false)
    }
    if (cardEntity.email.value.isNotBlank()) {
        drawCardText(cardEntity.email.labelElement("MAIL"), UIColor.darkGrayColor, false)
        drawCardText(cardEntity.email, UIColor.blackColor, false)
    }
    if (cardEntity.address1.value.isNotBlank() || cardEntity.address2.value.isNotBlank()) {
        drawCardText(cardEntity.address1.labelElement("ADDR"), UIColor.darkGrayColor, false)
        drawCardText(
            cardEntity.address2.copy(
                value = listOf(cardEntity.address1.value, cardEntity.address2.value)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            ),
            UIColor.blackColor,
            false,
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun drawCardText(
    element: CardEntity.CardElement,
    color: UIColor,
    bold: Boolean,
) {
    if (element.value.isBlank()) return

    val context = platform.UIKit.UIGraphicsGetCurrentContext()
    val x = CardPdfWidth * element.x
    val y = CardPdfHeight * element.y
    val fontSize = element.fontSize * CardPdfFontScale
    val font = if (bold) UIFont.boldSystemFontOfSize(fontSize) else UIFont.systemFontOfSize(fontSize)
    val attributes = mapOf<Any?, Any>(
        NSFontAttributeName to font,
        NSForegroundColorAttributeName to color,
    )

    if (context != null) {
        CGContextSaveGState(context)
        CGContextTranslateCTM(context, x, y)
        CGContextRotateCTM(context, element.rotation * PI / 180.0)
        NSString.create(string = element.value).drawAtPoint(
            point = platform.CoreGraphics.CGPointMake(0.0, 0.0),
            withAttributes = attributes,
        )
        CGContextRestoreGState(context)
    }
}

private fun CardEntity.CardElement.labelElement(label: String): CardEntity.CardElement = copy(
    value = label,
    x = max(0f, x - ContactLabelXOffsetRatio),
)

private fun loadImage(uri: String): UIImage? {
    val data = if (uri.startsWith("file://")) {
        NSURL.URLWithString(uri)?.let { NSData.dataWithContentsOfURL(it) }
    } else {
        NSData.dataWithContentsOfFile(uri)
    }
    return data?.let { UIImage.imageWithData(it) }
}

private fun cacheDirectoryPath(): String {
    return NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
}

private fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970() * 1000).toLong()

private const val CardPdfWidth = 910.0
private const val CardPdfHeight = 550.0
private const val CardPdfFontScale = 2.5
private const val ContactLabelXOffsetRatio = 0.08f
