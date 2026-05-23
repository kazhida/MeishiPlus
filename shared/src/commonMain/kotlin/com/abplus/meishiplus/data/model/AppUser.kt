package com.abplus.meishiplus.data.model

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity

data class AppUser(
    val user: UserEntity,
    val cards: List<CardEntity>,
)
