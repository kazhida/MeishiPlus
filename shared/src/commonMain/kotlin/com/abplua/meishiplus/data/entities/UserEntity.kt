package com.abplua.meishiplus.data.entities

data class UserEntity(
    val id: String,
    val cardIds: List<String> = emptyList()
)
