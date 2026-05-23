package com.abplus.meishiplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.repositories.AppRepositories
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.LocalAppRepositories
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.ui.screens.TabPagerScreen

@Composable
@Preview
fun App(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
) {
    val repositories = remember(userRepository, cardRepository) {
        if (userRepository != null && cardRepository != null) {
            AppRepositories(
                userRepository = userRepository,
                cardRepository = cardRepository,
            )
        } else {
            null
        }
    }

    MaterialTheme {
        CompositionLocalProvider(LocalAppRepositories provides repositories) {
            TabPagerScreen(
                authUser = authUser,
                onSignOut = onSignOut,
            )
        }
    }
}
