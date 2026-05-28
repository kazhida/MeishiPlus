package com.abplus.meishiplus.data.repositories

import com.abplus.meishiplus.data.entities.CardEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryCardRepositoryTest {
    @Test
    fun addCard_assignsIdAndInitializesLayout() = runTest {
        val repository = InMemoryCardRepository()
        val card = CardEntity(
            id = "ignored",
            name = CardEntity.CardElement(
                value = "未初期化",
                fontSize = 0f,
            ),
        )

        val added = repository.addCard(card)

        assertTrue(added.id.isNotBlank())
        assertNotEquals("ignored", added.id)
        assertEquals(CardEntity.default().name.x, added.name.x)
        assertEquals(CardEntity.default().name.y, added.name.y)
        assertEquals(CardEntity.default().name.fontSize, added.name.fontSize)
        assertEquals(added, repository.getCard(added.id))
    }

    @Test
    fun getCards_returnsCardsInRequestedOrder() = runTest {
        val repository = InMemoryCardRepository()
        val first = repository.addCard(CardEntity(caption = "first"))
        val second = repository.addCard(CardEntity(caption = "second"))
        val third = repository.addCard(CardEntity(caption = "third"))

        val cards = repository.getCards(
            listOf(
                third.id,
                first.id,
                second.id,
            ),
        )

        assertEquals(
            listOf(
                "third",
                "first",
                "second",
            ),
            cards.map { it.caption },
        )
    }

    @Test
    fun getCards_returnsEmptyListForEmptyIds() = runTest {
        val repository = InMemoryCardRepository()

        assertEquals(emptyList(), repository.getCards(emptyList()))
    }

    @Test
    fun saveUpdateAndDeleteCard_mutateStoredCard() = runTest {
        val repository = InMemoryCardRepository()
        repository.saveCard(CardEntity(id = "card-1", caption = "saved"))

        assertEquals("saved", repository.getCard("card-1").caption)

        repository.updateCard(CardEntity(id = "card-1", caption = "updated"))
        assertEquals("updated", repository.getCard("card-1").caption)

        repository.deleteCard("card-1")
        assertFailsWith<IllegalStateException> {
            repository.getCard("card-1")
        }
    }

    @Test
    fun getCard_failsWhenCardDoesNotExist() = runTest {
        val repository = InMemoryCardRepository()

        val error = assertFailsWith<IllegalStateException> {
            repository.getCard("missing")
        }
        assertEquals("Card not found: missing", error.message)
    }
}

private class InMemoryCardRepository : CardRepository {
    private val cards = mutableMapOf<String, CardEntity>()
    private var nextId = 0

    override suspend fun addCard(card: CardEntity): CardEntity {
        val id = "card-${nextId++}"
        val cardWithId = card.withInitializedLayout().copy(id = id)
        cards[id] = cardWithId
        return cardWithId
    }

    override suspend fun getCard(id: String): CardEntity =
        cards[id]?.withInitializedLayout() ?: error("Card not found: $id")

    override suspend fun getCards(cardIds: List<String>): List<CardEntity> =
        cardIds.map { id -> getCard(id) }

    override suspend fun saveCard(card: CardEntity) {
        cards[card.id] = card.withInitializedLayout()
    }

    override suspend fun deleteCard(id: String) {
        cards.remove(id)
    }

    override suspend fun updateCard(card: CardEntity) {
        saveCard(card)
    }
}
