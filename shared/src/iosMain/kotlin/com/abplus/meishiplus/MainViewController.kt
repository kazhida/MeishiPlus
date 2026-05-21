package com.abplus.meishiplus

import androidx.compose.ui.window.ComposeUIViewController
import com.abplus.meishiplus.auth.AuthUser

fun MainViewController(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
) = ComposeUIViewController {
    App(
        authUser = authUser,
        onSignOut = onSignOut,
    )
}
