package com.abplus.meishiplus.data.entities

data class UserEntity(
    val id: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val cardIds: List<String> = emptyList()
)
