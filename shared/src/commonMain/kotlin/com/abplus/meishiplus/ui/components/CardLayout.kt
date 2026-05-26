package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity

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

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        CardItem(
            cardEntity = editingCard,
            modifier = Modifier.fillMaxWidth(),
            onEditClick = {},
            onLayoutClick = {},
            isLayoutLocked = false,
            onCardChange = { updatedCard ->
                editingCard = updatedCard
            },
            onLayoutChangeFinished = {
                onCardChange(latestEditingCard)
            },
        )
    }
}
