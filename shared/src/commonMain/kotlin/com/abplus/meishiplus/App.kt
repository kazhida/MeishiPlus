package com.abplus.meishiplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.ui.screens.TabPagerScreen
import com.abplus.meishiplus.viewmodel.UserUiState
import com.abplus.meishiplus.viewmodel.UserViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
@Preview
fun App(
    authUser: AuthUser? = null,
    appUser: AppUser? = null,
    errorMessage: String? = null,
    onSignOut: (() -> Unit)? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
) {
    val fallbackUserState = remember { MutableStateFlow(UserUiState()) }
    val userViewModel = remember(userRepository, cardRepository) {
        if (userRepository != null && cardRepository != null) {
            UserViewModel(
                userRepository = userRepository,
                cardRepository = cardRepository,
            )
        } else {
            null
        }
    }
    val userState by (userViewModel?.uiState ?: fallbackUserState).collectAsState()

    LaunchedEffect(userViewModel, authUser?.uid) {
        userViewModel?.setAuthUser(authUser)
    }

    val effectiveAppUser = appUser ?: userState.appUser
    val effectiveErrorMessage = errorMessage ?: userState.errorMessage

    MaterialTheme {
        TabPagerScreen(
            authUser = authUser,
            appUser = effectiveAppUser,
            errorMessage = effectiveErrorMessage,
            onSignOut = onSignOut,
        )
    }
}
