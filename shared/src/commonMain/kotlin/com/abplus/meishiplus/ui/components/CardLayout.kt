package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.resources.BusinessCardBackgroundImagePaths
import com.abplus.meishiplus.resources.BusinessCardBackgroundOverlayMaxAlpha
import com.abplus.meishiplus.resources.normalizeBusinessCardBackgroundPath
import com.abplus.meishiplus.resources.resolveBusinessCardBackgroundUri

@Composable
fun CardLayout(
    modifier: Modifier = Modifier,
    cardEntity: CardEntity,
    onCardChange: (CardEntity) -> Unit = {},
) {
    var editingCard by remember(cardEntity.id) {
        mutableStateOf(cardEntity)
    }
    val latestEditingCard by rememberUpdatedState(editingCard)

    LaunchedEffect(cardEntity) {
        if (cardEntity != editingCard) {
            editingCard = cardEntity
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onCardChange(latestEditingCard)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        CardItem(
            cardEntity = editingCard,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .aspectRatio(91f / 55f),
            isLayoutLocked = false,
            onCardChange = { updatedCard ->
                editingCard = updatedCard
            },
            onLayoutChangeFinished = {
                onCardChange(latestEditingCard)
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = editingCard.bgAlpha.coerceIn(0f, BusinessCardBackgroundOverlayMaxAlpha),
            onValueChange = { bgAlpha ->
                val updatedCard = editingCard.copy(bgAlpha = bgAlpha)
                editingCard = updatedCard
                onCardChange(updatedCard)
            },
            valueRange = 0f..BusinessCardBackgroundOverlayMaxAlpha,
            modifier = Modifier.fillMaxWidth(),
        )
        BackgroundImageGrid(
            selectedImagePath = normalizeBusinessCardBackgroundPath(editingCard.bgFile),
            onImageClick = { imagePath ->
                val updatedCard = editingCard.copy(bgFile = imagePath)
                editingCard = updatedCard
                onCardChange(updatedCard)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun BackgroundImageGrid(
    selectedImagePath: String,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = remember {
        BusinessCardBackgroundImagePaths
    }

    Spacer(modifier = Modifier.height(16.dp))
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        images.chunked(3).forEach { rowImages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowImages.forEach { imagePath ->
                    val shape = RoundedCornerShape(4.dp)
                    AsyncImage(
                        model = resolveBusinessCardBackgroundUri(imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(91f / 55f)
                            .clip(shape)
                            .clickable { onImageClick(imagePath) }
                            .border(
                                width = if (imagePath == selectedImagePath) 3.dp else 0.dp,
                                color = if (imagePath == selectedImagePath) Color(0xFF00AFAF) else Color.Transparent,
                                shape = shape,
                            ),
                    )
                }
                repeat(3 - rowImages.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .width(0.dp)
                            .height(0.dp),
                    )
                }
            }
        }
    }
}
