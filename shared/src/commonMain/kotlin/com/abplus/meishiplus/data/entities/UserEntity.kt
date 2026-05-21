package com.abplus.meishiplus.data.entities

data class UserEntity(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val cardIds: List<String> = emptyList()
)
