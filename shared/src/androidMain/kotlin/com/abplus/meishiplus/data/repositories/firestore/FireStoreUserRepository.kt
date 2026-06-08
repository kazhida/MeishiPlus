package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreUserRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {
    private val users = firestore.collection(USERS_COLLECTION)

    override suspend fun addUser(user: UserEntity): UserEntity {
        val userId = user.id.ifBlank { users.document().id }
        val userWithId = user.copy(id = userId)
        users.document(userId)
            .set(userWithId)
            .await()
        return userWithId
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
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toUserEntity(): UserEntity? {
    val data = data ?: return null
    return UserEntity(
        id = data["id"] as? String ?: id,
        createdAt = data["createdAt"].asLong(),
        updatedAt = data["updatedAt"].asLong(),
        accounts = (data["accounts"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .mapNotNull { it.toAccount() },
        cardIds = (data["cardIds"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? String },
    )
}

private fun UserEntity.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "accounts" to accounts.map { it.toMap() },
        "cardIds" to cardIds,
    )

private fun Account.toMap(): Map<String, String> =
    mapOf(
        "service" to service,
        "userId" to userId,
        "userUrl" to userUrl,
    )

private fun Map<*, *>.toAccount(): Account? {
    val service = this["service"] as? String ?: return null
    val userId = this["userId"] as? String ?: return null
    val userUrl = this["userUrl"] as? String ?: ""

    return when (service.lowercase()) {
        "facebook" -> Account.Facebook(service, userId, userUrl)
        "x", "twitter" -> Account.X(service, userId, userUrl)
        "google" -> Account.Google(service, userId)
        "github" -> Account.Github(service, userId, userUrl)
        "instagram" -> Account.Instagram(service, userId, userUrl)
        "qiita" -> Account.Qiita(service, userId, userUrl)
        else -> null
    }
}

private fun Any?.asLong(): Long =
    when (this) {
        is Long -> this
        is Int -> toLong()
        is Double -> toLong()
        is Number -> toLong()
        else -> 0L
    }
