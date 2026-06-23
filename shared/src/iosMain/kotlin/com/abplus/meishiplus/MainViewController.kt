package com.abplus.meishiplus

import androidx.compose.ui.window.ComposeUIViewController
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.auth.IOSFacebookAuthBridge
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.abplus.meishiplus.data.model.Account

fun MainViewController(
    authUser: AuthUser? = null,
    onSignOut: ((Boolean) -> Unit)? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
    userViewModel: UserViewModel? = null,
    onPurchaseCardClick: ((onSuccess: () -> Unit) -> Unit)? = null,
    onFacebookAuthClick: (((Account.Facebook) -> Unit, (String) -> Unit) -> Unit)? = null,
) = ComposeUIViewController {
    IOSFacebookAuthBridge.setLoginHandler(onFacebookAuthClick)
    App(
        authUser = authUser,
        onSignOut = onSignOut,
        userRepository = userRepository,
        cardRepository = cardRepository,
        userViewModel = userViewModel,
        onPurchaseCardClick = onPurchaseCardClick,
    )
}
