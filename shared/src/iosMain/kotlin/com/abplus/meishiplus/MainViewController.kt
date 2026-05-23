package com.abplus.meishiplus

import androidx.compose.ui.window.ComposeUIViewController
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository

fun MainViewController(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
) = ComposeUIViewController {
    App(
        authUser = authUser,
        onSignOut = onSignOut,
        userRepository = userRepository,
        cardRepository = cardRepository,
    )
}
