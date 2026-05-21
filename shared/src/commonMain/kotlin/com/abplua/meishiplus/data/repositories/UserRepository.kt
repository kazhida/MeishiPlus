package com.abplua.meishiplus.data.repositories

import com.abplua.meishiplus.data.entities.UserEntity

interface UserRepository {
    suspend fun getUser(id: String): UserEntity
    suspend fun saveUser(user: UserEntity)
    suspend fun deleteUser(id: String)
    suspend fun updateUser(user: UserEntity)
}
