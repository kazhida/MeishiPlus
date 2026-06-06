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

        val createdCards = listOf("Public", "Private").map { caption ->
            cardRepository.addCard(
                CardEntity.default().copy(
                    ownerUid = authUser.uid,
                    caption = caption,
                    name = CardEntity.default().name.copy(value = defaultName),
                    email = CardEntity.default().email.copy(value = defaultEmail),
                )
            )
        }
        val updatedUser = userEntity.copy(cardIds = createdCards.map { it.id })
        userRepository.saveUser(updatedUser)

        return AppUser(user = updatedUser, cards = createdCards)
    }
}
