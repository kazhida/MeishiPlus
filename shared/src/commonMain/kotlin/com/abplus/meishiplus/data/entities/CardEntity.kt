package com.abplus.meishiplus.data.entities

data class CardEntity(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val phone: String = "",
    val organization: String = "",
    val title: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val partnerIds: List<String> = emptyList()
)
