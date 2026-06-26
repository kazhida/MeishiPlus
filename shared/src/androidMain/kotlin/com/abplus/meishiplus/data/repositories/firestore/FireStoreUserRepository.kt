package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.repositories.UserRepository
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class FireStoreUserRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {
    private val users = firestore.collection(USERS_COLLECTION)

    override suspend fun addUser(user: UserEntity): UserEntity {
        val userId = user.id.ifBlank { users.document().id }
        val userWithId = user.copy(id = userId)
        users.document(userId)
            .set(userWithId.toMap(), SetOptions.merge())
            .await()
        return userWithId
    }

    override suspend fun getUser(id: String): UserEntity =
        runCatching {
            users.document(id)
                .get(Source.SERVER)
                .await()
        }.getOrElse {
            users.document(id)
                .get()
                .await()
        }.toUserEntity()
            ?: error("User not found: $id")

    override suspend fun saveUser(user: UserEntity) {
        users.document(user.id)
            .set(user.toMap(), SetOptions.merge())
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
    buildMap {
        put("id", id)
        if (createdAt != 0L) {
            put("createdAt", createdAt)
        }
        if (updatedAt != 0L) {
            put("updatedAt", updatedAt)
        }
        if (accounts.isNotEmpty()) {
            put("accounts", accounts.map { it.toMap() })
        }
        if (cardIds.isNotEmpty()) {
            put("cardIds", cardIds)
        }
    }

private fun Account.toMap(): Map<String, Any?> =
    buildMap {
        put("service", service)
        put("userId", userName)
        put("userUrl", userUrl)
        if (this@toMap is Account.X && displayName != null) {
            put("displayName", displayName)
        }
    }

private fun Map<*, *>.toAccount(): Account? {
    val service = this["service"] as? String ?: return null
    val userId = this["userId"] as? String ?: return null
    val userUrl = this["userUrl"] as? String ?: ""
    val displayName = this["displayName"] as? String

    return when (service.lowercase()) {
        "facebook" -> Account.Facebook(service, userId, userUrl)
        "x", "twitter" -> Account.X(service, userId, userUrl, displayName)
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
