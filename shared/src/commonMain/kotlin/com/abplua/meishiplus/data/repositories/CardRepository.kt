package com.abplua.meishiplus.data.repositories

interface CardRepository {
    suspend fun getCards(cardIds: List<String>): List<String>
}
