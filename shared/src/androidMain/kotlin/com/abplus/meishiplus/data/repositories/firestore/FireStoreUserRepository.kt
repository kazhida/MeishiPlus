package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {
    private val users = firestore.collection(USERS_COLLECTION)
    private val cards = firestore.collection(CARDS_COLLECTION)

    override suspend fun addUser(user: UserEntity): UserEntity {
        val userId = user.id.ifBlank { users.document().id }
        val userDocument = users.document(userId)
        val existingUser = userDocument
            .get()
            .await()
            .toObject(UserEntity::class.java)
        if (existingUser != null && existingUser.cardIds.isNotEmpty()) {
            return existingUser
        }

        val userWithoutCards = (existingUser ?: user).copy(
            id = userId,
            cardIds = emptyList(),
        )
        if (existingUser == null) {
            userDocument
                .set(userWithoutCards)
                .await()
        }

        val cardDocuments = List(DEFAULT_CARD_COUNT) { index ->
            cards.document(defaultCardId(userId = userId, index = index))
        }
        val cardIds = cardDocuments.map { it.id }
        val userWithCards = userWithoutCards.copy(
            cardIds = cardIds,
        )
        val batch = firestore.batch()

        cardDocuments.forEach { document ->
            batch.set(
                document,
                CardEntity.default().copy(
                    id = document.id,
                    ownerUid = userId,
                ),
            )
        }
        batch.commit().await()

        userDocument
            .set(userWithCards)
            .await()

        return userWithCards
    }

    override suspend fun getUser(id: String): UserEntity =
        users.document(id)
            .get()
            .await()
            .toObject(UserEntity::class.java)
            ?: error("User not found: $id")

    override suspend fun saveUser(user: UserEntity) {
        users.document(user.id)
            .set(user)
            .await()
    }

    override suspend fun deleteUser(id: String) {
        users.document(id)
            .delete()
            .await()
    }

    override suspend fun updateUser(user: UserEntity) {
        saveUser(user)
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val CARDS_COLLECTION = "cards"
        const val DEFAULT_CARD_COUNT = 4

        fun defaultCardId(userId: String, index: Int): String =
            "${userId}_default_card_$index"
    }
}
