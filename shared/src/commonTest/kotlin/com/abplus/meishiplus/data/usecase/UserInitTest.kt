package com.abplus.meishiplus.data.usecase

import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class UserInitTest {

    @Test
    fun invoke_usesStableCardIds_andDoesNotDuplicateDefaultCards() = runTest {
        val userRepository = RecordingUserRepository()
        val cardRepository = RecordingCardRepository()
        val userInit = UserInit(
            userRepository = userRepository,
            cardRepository = cardRepository,
        )
        val authUser = AuthUser(
            uid = "user-1",
            displayName = "Taro",
            email = "taro@example.com",
            photoUrl = null,
        )

        val first = userInit(authUser)
        val second = userInit(authUser)

        assertEquals(listOf("user-1-public", "user-1-private"), first.cards.map { it.id })
        assertEquals(listOf("user-1-public", "user-1-private"), second.cards.map { it.id })
        assertEquals(2, cardRepository.cards.size)
        assertFalse(cardRepository.addCardCalled)
        assertEquals(
            AppUser(
                user = UserEntity(
                    id = "user-1",
                    cardIds = listOf("user-1-public", "user-1-private"),
                ),
                cards = second.cards,
            ),
            second,
        )
        assertTrue(userRepository.savedUsers.isNotEmpty())
    }
}

private class RecordingUserRepository : UserRepository {
    val savedUsers = mutableListOf<UserEntity>()

    override suspend fun addUser(user: UserEntity): UserEntity = user

    override suspend fun getUser(id: String): UserEntity =
        UserEntity(
            id = id,
            cardIds = emptyList(),
        )

    override suspend fun saveUser(user: UserEntity) {
        savedUsers += user
    }

    override suspend fun deleteUser(id: String) = Unit

    override suspend fun updateUser(user: UserEntity) = saveUser(user)
}

private class RecordingCardRepository : CardRepository {
    val cards = mutableMapOf<String, CardEntity>()
    var addCardCalled = false

    override suspend fun addCard(card: CardEntity): CardEntity {
        addCardCalled = true
        error("addCard should not be called by UserInit")
    }

    override suspend fun getCard(id: String): CardEntity =
        cards[id] ?: error("Card not found: $id")

    override suspend fun getCards(cardIds: List<String>): List<CardEntity> =
        cardIds.map { id -> getCard(id) }

    override suspend fun saveCard(card: CardEntity) {
        cards[card.id] = card
    }

    override suspend fun deleteCard(id: String) {
        cards.remove(id)
    }

    override suspend fun updateCard(card: CardEntity) {
        saveCard(card)
    }

    override suspend fun appendPartnerId(cardId: String, partnerCardId: String) = Unit
}
