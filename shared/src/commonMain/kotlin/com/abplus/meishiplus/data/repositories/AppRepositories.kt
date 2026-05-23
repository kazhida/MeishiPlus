package com.abplus.meishiplus.data.repositories

import androidx.compose.runtime.staticCompositionLocalOf

data class AppRepositories(
    val userRepository: UserRepository,
    val cardRepository: CardRepository,
)

val LocalAppRepositories = staticCompositionLocalOf<AppRepositories?> { null }
