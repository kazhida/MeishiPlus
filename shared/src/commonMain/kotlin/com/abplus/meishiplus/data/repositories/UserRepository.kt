package com.abplus.meishiplus.data.repositories

import com.abplus.meishiplus.data.entities.UserEntity

interface UserRepository {
    suspend fun addUser(user: UserEntity): UserEntity
    suspend fun getUser(id: String): UserEntity
    suspend fun saveUser(user: UserEntity)
    suspend fun deleteUser(id: String)
    suspend fun updateUser(user: UserEntity)
}
