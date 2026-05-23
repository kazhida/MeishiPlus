package com.abplus.meishiplus.data.repositories

import com.abplus.meishiplus.data.entities.CardEntity

interface CardRepository {
    suspend fun getCard(id: String): CardEntity
    suspend fun getCards(cardIds: List<String>): List<CardEntity>
    suspend fun saveCard(card: CardEntity)
    suspend fun deleteCard(id: String)
    suspend fun updateCard(card: CardEntity)
}
