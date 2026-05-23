package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.repositories.CardRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreCardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CardRepository {
    private val cards = firestore.collection(CARDS_COLLECTION)

    override suspend fun getCard(id: String): CardEntity =
        cards.document(id)
            .get()
            .await()
            .toObject(CardEntity::class.java)
            ?: error("Card not found: $id")

    override suspend fun getCards(cardIds: List<String>): List<CardEntity> {
        if (cardIds.isEmpty()) return emptyList()

        return cardIds.map { id ->
            getCard(id)
        }
    }

    override suspend fun saveCard(card: CardEntity) {
        cards.document(card.id)
            .set(card)
            .await()
    }

    override suspend fun deleteCard(id: String) {
        cards.document(id)
            .delete()
            .await()
    }

    override suspend fun updateCard(card: CardEntity) {
        saveCard(card)
    }

    private companion object {
        const val CARDS_COLLECTION = "cards"
    }
}
