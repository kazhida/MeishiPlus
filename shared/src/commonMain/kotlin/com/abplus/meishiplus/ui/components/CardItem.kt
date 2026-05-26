package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abplus.meishiplus.data.entities.CardEntity
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.ic_edit
import meishiplus.shared.generated.resources.ic_layout
import org.jetbrains.compose.resources.painterResource

@Composable
fun CardItem(
    cardEntity: CardEntity,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
    isLayoutLocked: Boolean = true,
    onCardChange: (CardEntity) -> Unit = {},
    onLayoutChangeFinished: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(91f / 55f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cardEntity.bgFile.isNotBlank()) {
                AsyncImage(
                    model = cardEntity.bgFile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(Modifier.fillMaxSize().alpha(cardEntity.bgAlpha).background(Color.White))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                CardText(
                    element = cardEntity.organization,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveOrganization(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                CardText(
                    element = cardEntity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveTitle(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                CardText(
                    element = cardEntity.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveName(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                ContactLabel(
                    label = "TEL",
                    element = cardEntity.phone,
                    isVisible = cardEntity.phone.value.isNotBlank(),
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.movePhone(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                CardText(
                    element = cardEntity.phone,
                    style = MaterialTheme.typography.bodySmall,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.movePhone(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                ContactLabel(
                    label = "MAIL",
                    element = cardEntity.email,
                    isVisible = cardEntity.email.value.isNotBlank(),
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveEmail(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                CardText(
                    element = cardEntity.email,
                    style = MaterialTheme.typography.bodySmall,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveEmail(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                ContactLabel(
                    label = "ADDR",
                    element = cardEntity.address1,
                    isVisible = cardEntity.address1.value.isNotBlank() || cardEntity.address2.value.isNotBlank(),
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveAddress(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
                CardText(
                    element = cardEntity.address2,
                    style = MaterialTheme.typography.bodySmall,
                    text = listOf(
                        cardEntity.address1.value,
                        cardEntity.address2.value,
                    ).filter { it.isNotBlank() }.joinToString("\n"),
                    maxLines = 2,
                    isLayoutLocked = isLayoutLocked,
                    onDrag = { dx, dy -> onCardChange(cardEntity.moveAddress(dx, dy)) },
                    onDragFinished = onLayoutChangeFinished,
                )
            }

            if (isLayoutLocked) Column(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = "編集",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onLayoutClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_layout),
                        contentDescription = "レイアウト",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.CardText(
    element: CardEntity.CardElement,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    text: String = element.value,
    maxLines: Int = 1,
    isLayoutLocked: Boolean = true,
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragFinished: () -> Unit = {},
) {
    val xOffset = maxWidth * element.x
    val yOffset = maxHeight * element.y
    val textMaxWidth = (maxWidth - xOffset - 16.dp).coerceAtLeast(24.dp)

    Text(
        text = text,
        style = style.copy(fontSize = element.fontSize.sp),
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .widthIn(min = 24.dp, max = textMaxWidth)
            .draggableCardElement(
                isLayoutLocked = isLayoutLocked,
                maxWidthPx = constraints.maxWidth.toFloat(),
                maxHeightPx = constraints.maxHeight.toFloat(),
                onDrag = onDrag,
                onDragFinished = onDragFinished,
            )
            .graphicsLayer {
                rotationZ = element.rotation.toFloat()
            },
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.ContactLabel(
    label: String,
    element: CardEntity.CardElement,
    isVisible: Boolean,
    isLayoutLocked: Boolean = true,
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragFinished: () -> Unit = {},
) {
    if (!isVisible) return

    val xOffset = maxWidth * (element.x - ContactLabelXOffset).coerceAtLeast(0f)
    val yOffset = maxHeight * element.y

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .offset(
                x = xOffset,
                y = yOffset,
            )
            .draggableCardElement(
                isLayoutLocked = isLayoutLocked,
                maxWidthPx = constraints.maxWidth.toFloat(),
                maxHeightPx = constraints.maxHeight.toFloat(),
                onDrag = onDrag,
                onDragFinished = onDragFinished,
            ),
    )
}

@Composable
private fun Modifier.draggableCardElement(
    isLayoutLocked: Boolean,
    maxWidthPx: Float,
    maxHeightPx: Float,
    onDrag: (Float, Float) -> Unit,
    onDragFinished: () -> Unit,
): Modifier {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)

    if (isLayoutLocked || maxWidthPx <= 0f || maxHeightPx <= 0f) return this

    return pointerInput(maxWidthPx, maxHeightPx) {
        detectDragGestures(
            onDragEnd = { currentOnDragFinished() },
            onDragCancel = { currentOnDragFinished() },
            onDrag = { change, dragAmount ->
                change.consume()
                currentOnDrag(
                    dragAmount.x / maxWidthPx,
                    dragAmount.y / maxHeightPx,
                )
            },
        )
    }
}

private fun CardEntity.moveName(dx: Float, dy: Float): CardEntity =
    copy(name = name.movedBy(dx, dy))

private fun CardEntity.moveOrganization(dx: Float, dy: Float): CardEntity =
    copy(organization = organization.movedBy(dx, dy))

private fun CardEntity.moveTitle(dx: Float, dy: Float): CardEntity =
    copy(title = title.movedBy(dx, dy))

private fun CardEntity.movePhone(dx: Float, dy: Float): CardEntity =
    copy(phone = phone.movedBy(dx, dy))

private fun CardEntity.moveEmail(dx: Float, dy: Float): CardEntity =
    copy(email = email.movedBy(dx, dy))

private fun CardEntity.moveAddress(dx: Float, dy: Float): CardEntity =
    copy(
        address1 = address1.movedBy(dx, dy),
        address2 = address2.movedBy(dx, dy),
    )

private fun CardEntity.CardElement.movedBy(
    dx: Float,
    dy: Float,
): CardEntity.CardElement =
    copy(
        x = (x + dx).coerceIn(0f, 1f),
        y = (y + dy).coerceIn(0f, 1f),
    )

private const val ContactLabelXOffset = 0.13f
