package com.abplus.meishiplus.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.ui.components.CardEntry

@Composable
fun CardEntryScreen(
    cardEntity: CardEntity,
    onCardChange: (CardEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold() {
        CardEntry(cardEntity, onCardChange, modifier)
    }
}