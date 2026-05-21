package com.abplus.meishiplus.data.entities

data class CardEntity(
    val id: String,
    val name: String,
    val email: String = "",
    val address: String = "",
    val phone: String = "",
    val organization: String = "",
    val title: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val partnerIds: List<String> = emptyList()
)
