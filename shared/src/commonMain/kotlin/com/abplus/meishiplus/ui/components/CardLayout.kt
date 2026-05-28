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
import meishiplus.shared.generated.resources.Res

private val BackgroundImageFileNames = listOf(
    "00.jpg",
    "01.jpg",
    "02.jpg",
    "03.jpg",
    "04.jpg",
    "05.jpg",
    "06.jpg",
    "07.jpg",
    "08.jpg",
    "09.jpg",
    "10.jpg",
    "11.jpg",
    "12.jpg",
    "13.jpg",
    "14.jpg",
    "15.jpg",
    "16.jpg",
    "17.jpg",
    "18.jpg",
    "19.jpg",
    "20.jpg",
    "21.jpg",
    "22.jpg",
    "23.jpg",
    "24.jpg",
    "25.jpg",
    "26.jpg",
    "27.jpg",
    "28.jpg",
    "29.jpg",
    "30.jpg",
    "31.jpg",
    "32.jpg",
    "33.jpg",
    "34.jpg",
    "35.jpg",
    "36.jpg",
    "37.jpg",
    "38.jpg",
    "39.jpg",
    "40.jpg",
    "41.jpg",
    "42.jpg",
    "43.jpg",
    "44.jpg",
)

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
            value = editingCard.bgAlpha,
            onValueChange = { bgAlpha ->
                val updatedCard = editingCard.copy(bgAlpha = bgAlpha)
                editingCard = updatedCard
                onCardChange(updatedCard)
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        BackgroundImageGrid(
            selectedImageUri = editingCard.bgFile,
            onImageClick = { imageUri ->
                val updatedCard = editingCard.copy(bgFile = imageUri)
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
    selectedImageUri: String,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = remember {
        BackgroundImageFileNames.map { fileName -> Res.getUri("files/bgs/$fileName") }
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
                rowImages.forEach { imageUri ->
                    val shape = RoundedCornerShape(4.dp)
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(91f / 55f)
                            .clip(shape)
                            .clickable { onImageClick(imageUri) }
                            .border(
                                width = if (imageUri == selectedImageUri) 3.dp else 0.dp,
                                color = if (imageUri == selectedImageUri) Color(0xFF00AFAF) else Color.Transparent,
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
