package com.abplus.meishiplus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplus.meishiplus.data.entities.CardEntity

@Composable
fun CardLayout(
    modifier: Modifier = Modifier,
    cardEntity: CardEntity
) {
    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        CardItem(
            cardEntity = cardEntity,
            modifier = Modifier.fillMaxWidth(),
            onEditClick = {},
            onLayoutClick = {},
        )

    }
}
