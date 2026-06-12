package com.abplus.meishiplus

import androidx.compose.ui.window.ComposeUIViewController
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.viewmodel.UserViewModel

fun MainViewController(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
    userViewModel: UserViewModel? = null,
    onPurchaseCardClick: ((onSuccess: () -> Unit) -> Unit)? = null,
) = ComposeUIViewController {
    App(
        authUser = authUser,
        onSignOut = onSignOut,
        userRepository = userRepository,
        cardRepository = cardRepository,
        userViewModel = userViewModel,
        onPurchaseCardClick = onPurchaseCardClick,
    )
}
