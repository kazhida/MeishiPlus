package com.abplus.meishiplus.data.usecase

import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository

class UserInit(
    private val userRepository: UserRepository,
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(authUser: AuthUser): AppUser {
        val userEntity = runCatching {
            userRepository.getUser(authUser.uid)
        }.getOrElse {
            userRepository.addUser(UserEntity(id = authUser.uid))
        }

        if (userEntity.cardIds.isNotEmpty()) {
            val cards = cardRepository.getCards(userEntity.cardIds)
            return AppUser(user = userEntity, cards = cards)
        }

        val defaultName = authUser.displayName ?: CardEntity.default().name.value
        val defaultEmail = authUser.email ?: CardEntity.default().email.value

        val createdCards = listOf(
            createDefaultCard(
                ownerUid = authUser.uid,
                caption = "Public",
                defaultName = defaultName,
                defaultEmail = defaultEmail,
            ),
            createDefaultCard(
                ownerUid = authUser.uid,
                caption = "Private",
                defaultName = defaultName,
                defaultEmail = defaultEmail,
            ),
        ).onEach { cardRepository.saveCard(it) }
        val updatedUser = userEntity.copy(cardIds = createdCards.map { it.id })
        userRepository.saveUser(updatedUser)

        return AppUser(user = updatedUser, cards = createdCards)
    }

    suspend fun purchaseAdditionalCard(authUser: AuthUser): AppUser {
        val userEntity = runCatching {
            userRepository.getUser(authUser.uid)
        }.getOrElse {
            userRepository.addUser(UserEntity(id = authUser.uid))
        }

        val cards = if (userEntity.cardIds.isNotEmpty()) {
            cardRepository.getCards(userEntity.cardIds)
        } else {
            emptyList()
        }
        val sourceCard = cards.firstOrNull() ?: error("追加対象のカードがありません。")

        val addedCard = cardRepository.addCard(
            sourceCard.copy(
                id = "",
                ownerUid = authUser.uid,
            ),
        )
        val updatedUser = userEntity.copy(
            cardIds = userEntity.cardIds + addedCard.id,
        )
        userRepository.saveUser(updatedUser)

        return AppUser(
            user = updatedUser,
            cards = cards + addedCard,
        )
    }

    private fun createDefaultCard(
        ownerUid: String,
        caption: String,
        defaultName: String,
        defaultEmail: String,
    ): CardEntity =
        CardEntity.default().copy(
            id = defaultCardId(ownerUid, caption),
            ownerUid = ownerUid,
            caption = caption,
            name = CardEntity.default().name.copy(value = defaultName),
            email = CardEntity.default().email.copy(value = defaultEmail),
        )

    private fun defaultCardId(ownerUid: String, caption: String): String =
        "${ownerUid.lowercase()}-${caption.lowercase()}"
}
