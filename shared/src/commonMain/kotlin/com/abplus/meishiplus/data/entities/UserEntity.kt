package com.abplus.meishiplus.data.entities

import kotlinx.serialization.Serializable
import com.abplus.meishiplus.data.model.Account

@Serializable
data class UserEntity(
    val id: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val accounts: List<Account> = emptyList(),
    val cardIds: List<String> = emptyList()
)
