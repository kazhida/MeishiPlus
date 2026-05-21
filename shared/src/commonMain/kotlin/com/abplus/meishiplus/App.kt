package com.abplus.meishiplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.ui.screens.TabPagerScreen

@Composable
@Preview
fun App(
    authUser: AuthUser? = null,
    onSignOut: (() -> Unit)? = null,
) {
    MaterialTheme {
        TabPagerScreen(
            authUser = authUser,
            onSignOut = onSignOut,
        )
    }
}
