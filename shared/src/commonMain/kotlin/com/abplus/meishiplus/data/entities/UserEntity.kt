package com.abplus.meishiplus.data.entities

import com.abplus.meishiplus.data.model.Account

data class UserEntity(
    val id: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val accounts: List<Account> = emptyList(),
    val cardIds: List<String> = emptyList()
)
