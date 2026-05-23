package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {
    private val users = firestore.collection(USERS_COLLECTION)

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
