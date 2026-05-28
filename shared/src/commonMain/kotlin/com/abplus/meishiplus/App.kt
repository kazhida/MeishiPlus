package com.abplus.meishiplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.ui.screens.CardEntryScreen
import com.abplus.meishiplus.ui.screens.CardExchangeScreen
import com.abplus.meishiplus.ui.screens.CardLayoutScreen
import com.abplus.meishiplus.ui.screens.CardPreviewScreen
import com.abplus.meishiplus.ui.screens.CardPrintScreen
import com.abplus.meishiplus.ui.screens.TabPagerScreen
import com.abplus.meishiplus.viewmodel.UserUiState
import com.abplus.meishiplus.viewmodel.UserViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Composable
@Preview
fun App(
    authUser: AuthUser? = null,
    appUser: AppUser? = null,
    errorMessage: String? = null,
    onSignOut: (() -> Unit)? = null,
    userViewModel: UserViewModel? = null,
    userRepository: UserRepository? = null,
    cardRepository: CardRepository? = null,
) {
    val fallbackUserState = remember { MutableStateFlow(UserUiState()) }
    val ownedUserViewModel = remember(userRepository, cardRepository) {
        if (userRepository != null && cardRepository != null) {
            UserViewModel(
                userRepository = userRepository,
                cardRepository = cardRepository,
            )
        } else {
            null
        }
    }
    val effectiveUserViewModel = userViewModel ?: ownedUserViewModel
    val userState by (effectiveUserViewModel?.uiState ?: fallbackUserState).collectAsState()

    LaunchedEffect(effectiveUserViewModel, authUser?.uid) {
        effectiveUserViewModel?.setAuthUser(authUser)
    }

    val effectiveAppUser = appUser ?: userState.appUser
    val effectiveErrorMessage = errorMessage ?: userState.errorMessage
    val navController = rememberNavController()

    MaterialTheme {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
        ) {
            composable<HomeRoute> {
                TabPagerScreen(
                    authUser = authUser,
                    appUser = effectiveAppUser,
                    errorMessage = effectiveErrorMessage,
                    onSignOut = onSignOut,
                    onEditCard = { cardIndex ->
                        navController.navigate(CardEntryRoute(cardIndex))
                    },
                    onLayoutCard = { cardIndex ->
                        navController.navigate(CardLayoutRoute(cardIndex))
                    },
                    onPrintCard = { cardIndex ->
                        navController.navigate(CardPrintRoute(cardIndex))
                    },
                    onExchangeCard = { cardIndex ->
                        navController.navigate(CardExchangeRoute(cardIndex))
                    },
                    onPreviewCard = { cardIndex ->
                        navController.navigate(CardPreviewRoute(cardIndex))
                    },
                )
            }
            composable<CardEntryRoute> { backStackEntry ->
                val cardIndex = backStackEntry.toRoute<CardEntryRoute>().cardIndex
                val card = effectiveAppUser?.cards?.getOrNull(cardIndex) ?: CardEntity.default().copy(
                    id = cardIndex.toString(),
                    name = CardEntity.default().name.copy(value = "名刺${cardIndex + 1}"),
                )
                val latestCard by rememberUpdatedState(card)
                DisposableEffect(cardIndex, effectiveUserViewModel) {
                    onDispose {
                        effectiveUserViewModel?.updateCardAndReloadUser(latestCard)
                    }
                }
                CardEntryScreen(
                    cardEntity = card,
                    onCardChange = { updatedCard ->
                        effectiveUserViewModel?.updateCard(cardIndex, updatedCard)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<CardLayoutRoute> { backStackEntry ->
                val cardIndex = backStackEntry.toRoute<CardLayoutRoute>().cardIndex
                val card = effectiveAppUser?.cards?.getOrNull(cardIndex) ?: CardEntity.default().copy(
                    id = cardIndex.toString(),
                    name = CardEntity.default().name.copy(value = "名刺${cardIndex + 1}"),
                )
                val latestCard by rememberUpdatedState(card)
                DisposableEffect(cardIndex, effectiveUserViewModel) {
                    onDispose {
                        effectiveUserViewModel?.updateCardAndReloadUser(latestCard)
                    }
                }
                CardLayoutScreen(
                    cardEntity = card,
                    onCardChange = { updatedCard ->
                        effectiveUserViewModel?.updateCard(cardIndex, updatedCard)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<CardPrintRoute> { backStackEntry ->
                val cardIndex = backStackEntry.toRoute<CardPrintRoute>().cardIndex
                val card = effectiveAppUser?.cards?.getOrNull(cardIndex) ?: CardEntity.default().copy(
                    id = cardIndex.toString(),
                    name = CardEntity.default().name.copy(value = "名刺${cardIndex + 1}"),
                )
                val latestCard by rememberUpdatedState(card)
                DisposableEffect(cardIndex, effectiveUserViewModel) {
                    onDispose {
                        effectiveUserViewModel?.updateCardAndReloadUser(latestCard)
                    }
                }
                CardPrintScreen(
                    cardEntity = card,
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<CardPreviewRoute> { backStackEntry ->
                val cardIndex = backStackEntry.toRoute<CardPreviewRoute>().cardIndex
                val card = effectiveAppUser?.cards?.getOrNull(cardIndex) ?: CardEntity.default().copy(
                    id = cardIndex.toString(),
                    name = CardEntity.default().name.copy(value = "名刺${cardIndex + 1}"),
                )
                CardPreviewScreen(
                    cardEntity = card,
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<CardExchangeRoute> { backStackEntry ->
                val cardIndex = backStackEntry.toRoute<CardExchangeRoute>().cardIndex
                val card = effectiveAppUser?.cards?.getOrNull(cardIndex) ?: CardEntity.default().copy(
                    id = cardIndex.toString(),
                    name = CardEntity.default().name.copy(value = "名刺${cardIndex + 1}"),
                )
                CardExchangeScreen(
                    cardEntity = card,
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Serializable
private data object HomeRoute

@Serializable
private data class CardEntryRoute(val cardIndex: Int)

@Serializable
private data class CardLayoutRoute(val cardIndex: Int)

@Serializable
private data class CardPrintRoute(val cardIndex: Int)

@Serializable
private data class CardPreviewRoute(val cardIndex: Int)

@Serializable
private data class CardExchangeRoute(val cardIndex: Int)
