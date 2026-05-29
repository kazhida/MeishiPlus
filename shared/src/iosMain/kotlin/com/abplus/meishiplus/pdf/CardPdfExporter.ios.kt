package com.abplus.meishiplus.pdf

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.resources.BusinessCardBackgroundOverlayMaxAlpha
import com.abplus.meishiplus.resources.resolveBusinessCardBackgroundUri
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGContextClipToRect
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetAlpha
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetStrokeColorWithColor
import platform.CoreGraphics.CGContextStrokeRect
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
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
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIViewController
import platform.UIKit.drawAtPoint
import platform.UIKit.popoverPresentationController
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
actual fun deletePdfFileQuietly(filePath: String) {
    runCatching {
        val deleted = NSFileManager.defaultManager.removeItemAtPath(filePath, null)
        if (!deleted) {
            NSLog("Failed to delete PDF file: %@", filePath)
        }
    }.onFailure { throwable ->
        NSLog("Failed to delete PDF file: %@ (%@)", filePath, throwable.message ?: "unknown error")
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    val outputPath = "${cacheDirectoryPath()}/meishi-${cardEntity.id.ifBlank { "card" }}-${currentTimeMillis()}.pdf"
    val bounds = CGRectMake(0.0, 0.0, CardPdfWidth, CardPdfHeight)
    val renderer = UIGraphicsPDFRenderer(bounds = bounds)
    val data = renderer.PDFDataWithActions { context ->
        context?.beginPage()
        drawCardPdf(
            cardEntity = cardEntity,
            left = 0.0,
            top = 0.0,
            width = CardPdfWidth,
            height = CardPdfHeight,
            pageHeight = CardPdfHeight,
            drawBorder = false,
        )
    }
    data.writeToFile(outputPath, atomically = true)
    return CardPdfExportResult(filePath = outputPath)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createPostcardCardPdf(cardEntity: CardEntity): CardPdfExportResult {
    val outputPath = "${cacheDirectoryPath()}/meishi-postcard-${cardEntity.id.ifBlank { "card" }}-${currentTimeMillis()}.pdf"
    val bounds = CGRectMake(0.0, 0.0, PostcardPdfWidth, PostcardPdfHeight)
    val renderer = UIGraphicsPDFRenderer(bounds = bounds)
    val data = renderer.PDFDataWithActions { context ->
        context?.beginPage()

        UIColor.whiteColor.setFill()
        platform.UIKit.UIRectFill(bounds)

        val cardWidth = mmToPdfPoints(91.0)
        val cardHeight = mmToPdfPoints(55.0)
        val left = (PostcardPdfWidth - cardWidth) / 2.0
        val gap = mmToPdfPoints(12.0)
        val top = (PostcardPdfHeight - cardHeight * 2.0 - gap) / 2.0

        repeat(2) { index ->
            drawCardPdf(
                cardEntity = cardEntity,
                left = left,
                top = top + index * (cardHeight + gap),
                width = cardWidth,
                height = cardHeight,
                pageHeight = PostcardPdfHeight,
                drawBorder = true,
            )
        }
    }
    data.writeToFile(outputPath, atomically = true)
    sharePdfFile(outputPath)
    return CardPdfExportResult(filePath = outputPath)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun createA4CardPdf(
    cardEntity: CardEntity,
    topMarginMm: Float,
    bottomMarginMm: Float,
    leftMarginMm: Float,
    rightMarginMm: Float,
): CardPdfExportResult {
    val outputPath = "${cacheDirectoryPath()}/meishi-a4-${cardEntity.id.ifBlank { "card" }}-${currentTimeMillis()}.pdf"
    val bounds = CGRectMake(0.0, 0.0, A4PdfWidth, A4PdfHeight)
    val renderer = UIGraphicsPDFRenderer(bounds = bounds)
    val data = renderer.PDFDataWithActions { context ->
        context?.beginPage()

        UIColor.whiteColor.setFill()
        platform.UIKit.UIRectFill(bounds)

        val leftMargin = mmToPdfPoints(leftMarginMm.coerceAtLeast(0f).toDouble())
        val rightMargin = mmToPdfPoints(rightMarginMm.coerceAtLeast(0f).toDouble())
        val topMargin = mmToPdfPoints(topMarginMm.coerceAtLeast(0f).toDouble())
        val bottomMargin = mmToPdfPoints(bottomMarginMm.coerceAtLeast(0f).toDouble())
        val printableLeft = leftMargin
        val printableTop = topMargin
        val printableWidth = (A4PdfWidth - leftMargin - rightMargin).coerceAtLeast(0.0)
        val printableHeight = (A4PdfHeight - topMargin - bottomMargin).coerceAtLeast(0.0)
        val cellWidth = printableWidth / A4Columns
        val cellHeight = printableHeight / A4Rows
        val cardAspectWidth = 91.0
        val cardAspectHeight = 55.0
        val scale = min(cellWidth / cardAspectWidth, cellHeight / cardAspectHeight).coerceAtLeast(0.0)
        val cardWidth = cardAspectWidth * scale
        val cardHeight = cardAspectHeight * scale

        repeat(A4Rows) { row ->
            repeat(A4Columns) { column ->
                val cellLeft = printableLeft + column * cellWidth
                val cellTop = printableTop + row * cellHeight
                val cardLeft = cellLeft + (cellWidth - cardWidth) / 2.0
                val cardTop = cellTop + (cellHeight - cardHeight) / 2.0
                drawCardPdf(
                    cardEntity = cardEntity,
                    left = cardLeft,
                    top = cardTop,
                    width = cardWidth,
                    height = cardHeight,
                    pageHeight = A4PdfHeight,
                    drawBorder = true,
                )
            }
        }
    }
    data.writeToFile(outputPath, atomically = true)
    sharePdfFile(outputPath)
    return CardPdfExportResult(filePath = outputPath)
}

@OptIn(ExperimentalForeignApi::class)
private fun drawCardPdf(
    cardEntity: CardEntity,
    left: Double,
    top: Double,
    width: Double,
    height: Double,
    pageHeight: Double,
    drawBorder: Boolean,
) {
    val rect = CGRectMake(left, top, width, height)

    UIColor.whiteColor.setFill()
    platform.UIKit.UIRectFill(rect)

    if (cardEntity.bgFile.isNotBlank()) {
        loadImage(cardEntity.bgFile)?.let { image ->
            image.CGImage?.let { cgImage ->
                val context = platform.UIKit.UIGraphicsGetCurrentContext() ?: return@let
                val imageWidth = platform.CoreGraphics.CGImageGetWidth(cgImage).toDouble()
                val imageHeight = platform.CoreGraphics.CGImageGetHeight(cgImage).toDouble()
                val scale = max(width / imageWidth, height / imageHeight)
                val scaledWidth = imageWidth * scale
                val scaledHeight = imageHeight * scale
                val drawLeft = left + (width - scaledWidth) / 2.0
                val drawTop = top + (height - scaledHeight) / 2.0
                CGContextSaveGState(context)
                CGContextTranslateCTM(context, 0.0, pageHeight)
                CGContextScaleCTM(context, 1.0, -1.0)
                CGContextClipToRect(context, CGRectMake(left, pageHeight - top - height, width, height))
                CGContextDrawImage(
                    context,
                    CGRectMake(drawLeft, pageHeight - drawTop - scaledHeight, scaledWidth, scaledHeight),
                    cgImage,
                )
                CGContextRestoreGState(context)
            }
        }
    }

    if (cardEntity.bgAlpha > 0f) {
        val context = platform.UIKit.UIGraphicsGetCurrentContext()
        if (context != null) {
            CGContextSaveGState(context)
            CGContextSetAlpha(context, cardEntity.bgAlpha.coerceIn(0f, BusinessCardBackgroundOverlayMaxAlpha).toDouble())
            CGContextSetFillColorWithColor(context, UIColor.whiteColor.CGColor)
            platform.UIKit.UIRectFill(rect)
            CGContextRestoreGState(context)
        }
    }

    drawCardText(left, top, width, height, cardEntity.organization, UIColor(red = 0.0, green = 0.686, blue = 0.686, alpha = 1.0), false)
    drawCardText(left, top, width, height, cardEntity.title, UIColor.darkGrayColor, false)
    drawCardText(left, top, width, height, cardEntity.name, UIColor.blackColor, true)
    if (cardEntity.phone.value.isNotBlank()) {
        drawCardText(left, top, width, height, cardEntity.phone.labelElement("TEL"), UIColor.darkGrayColor, false)
        drawCardText(left, top, width, height, cardEntity.phone, UIColor.blackColor, false)
    }
    if (cardEntity.email.value.isNotBlank()) {
        drawCardText(left, top, width, height, cardEntity.email.labelElement("MAIL"), UIColor.darkGrayColor, false)
        drawCardText(left, top, width, height, cardEntity.email, UIColor.blackColor, false)
    }
    if (cardEntity.address1.value.isNotBlank() || cardEntity.address2.value.isNotBlank()) {
        drawCardText(left, top, width, height, cardEntity.address1.labelElement("ADDR"), UIColor.darkGrayColor, false)
        drawCardText(
            left,
            top,
            width,
            height,
            cardEntity.address2.copy(
                value = listOf(cardEntity.address1.value, cardEntity.address2.value)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            ),
            UIColor.blackColor,
            false,
        )
    }

    if (drawBorder) {
        val context = platform.UIKit.UIGraphicsGetCurrentContext()
        if (context != null) {
            CGContextSaveGState(context)
            CGContextSetStrokeColorWithColor(context, UIColor(red = 0.85, green = 0.85, blue = 0.85, alpha = 1.0).CGColor)
            CGContextSetLineWidth(context, 0.6)
            CGContextStrokeRect(context, rect)
            CGContextRestoreGState(context)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun drawCardText(
    left: Double,
    top: Double,
    width: Double,
    height: Double,
    element: CardEntity.CardElement,
    color: UIColor,
    bold: Boolean,
) {
    if (element.value.isBlank()) return

    val context = platform.UIKit.UIGraphicsGetCurrentContext()
    val x = left + width * element.x
    val y = top + height * element.y
    val scale = width / CardPdfWidth
    val fontSize = element.fontSize * CardPdfFontScale * scale
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
    val resolvedUri = resolveBusinessCardBackgroundUri(uri)
    val data = if (resolvedUri.startsWith("file://")) {
        NSURL.URLWithString(resolvedUri)?.let { NSData.dataWithContentsOfURL(it) }
    } else {
        NSData.dataWithContentsOfFile(resolvedUri)
    }
    return data?.let { UIImage.imageWithData(it) }
}

private fun cacheDirectoryPath(): String {
    return NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
}

@OptIn(ExperimentalForeignApi::class)
private fun sharePdfFile(path: String) {
    val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController?.topPresentedViewController() ?: return
    val url = NSURL.fileURLWithPath(path)
    val controller = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )
    controller.popoverPresentationController?.let { popover ->
        popover.sourceView = presenter.view
        popover.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
        popover.permittedArrowDirections = 0u
    }
    presenter.presentViewController(controller, animated = true, completion = null)
}

private fun UIViewController.topPresentedViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

private fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970() * 1000).toLong()

private const val CardPdfWidth = 910.0
private const val CardPdfHeight = 550.0
private const val CardPdfFontScale = 2.5
private const val ContactLabelXOffsetRatio = 0.08f
private val PostcardPdfWidth = mmToPdfPoints(100.0)
private val PostcardPdfHeight = mmToPdfPoints(148.0)
private val A4PdfWidth = mmToPdfPoints(210.0)
private val A4PdfHeight = mmToPdfPoints(297.0)
private const val A4Columns = 2
private const val A4Rows = 5

private fun mmToPdfPoints(mm: Double): Double = mm / 25.4 * 72.0
