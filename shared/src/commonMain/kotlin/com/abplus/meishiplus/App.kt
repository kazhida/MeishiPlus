package com.abplus.meishiplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.auth.DefaultSnsAccountAuthenticator
import com.abplus.meishiplus.auth.SnsAuthDeepLinkState
import com.abplus.meishiplus.auth.SnsAuthRedirectOutcome
import com.abplus.meishiplus.auth.parseSnsAuthRedirect
import com.abplus.meishiplus.auth.authenticateSnsAccount
import com.abplus.meishiplus.auth.snsAuthFailedMessage
import com.abplus.meishiplus.auth.snsAuthInvalidRedirectMessage
import com.abplus.meishiplus.auth.snsAuthMissingCodeMessage
import com.abplus.meishiplus.auth.snsAuthReflectFailedMessage
import com.abplus.meishiplus.auth.snsAuthUnsupportedServiceMessage
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.data.usecase.exchangeCard
import com.abplus.meishiplus.data.usecase.UserInit
import com.abplus.meishiplus.ui.screens.ChargeScreen
import com.abplus.meishiplus.ui.screens.CardEntryScreen
import com.abplus.meishiplus.ui.screens.CardExchangeScreen
import com.abplus.meishiplus.ui.screens.CardLayoutScreen
import com.abplus.meishiplus.ui.screens.CardPreviewScreen
import com.abplus.meishiplus.ui.screens.CardPrintScreen
import com.abplus.meishiplus.ui.screens.LicenseScreen
import com.abplus.meishiplus.ui.screens.PartnerCardScreen
import com.abplus.meishiplus.ui.screens.SnsAuthScreen
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
    onPurchaseCardClick: ((onSuccess: () -> Unit) -> Unit)? = null,
) {
    val fallbackUserState = remember { MutableStateFlow(UserUiState()) }
    val ownedUserViewModel = remember(userRepository, cardRepository) {
        if (userRepository != null && cardRepository != null) {
            UserViewModel(
                userInit = UserInit(userRepository, cardRepository),
                cardRepository = cardRepository,
            )
        } else {
            null
        }
    }
    val effectiveUserViewModel = userViewModel ?: ownedUserViewModel
    val userState by (effectiveUserViewModel?.uiState ?: fallbackUserState).collectAsState()
    val pendingDeepLink by SnsAuthDeepLinkState.pendingUrl.collectAsState()

    LaunchedEffect(effectiveUserViewModel, authUser?.uid) {
        effectiveUserViewModel?.setAuthUser(authUser)
    }

    LaunchedEffect(
        pendingDeepLink,
        effectiveUserViewModel,
        authUser?.uid,
        userState.isLoading,
    ) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        val currentAuthUser = authUser ?: return@LaunchedEffect
        val repository = userRepository ?: return@LaunchedEffect
        val viewModel = effectiveUserViewModel ?: return@LaunchedEffect
        if (userState.isLoading) return@LaunchedEffect

        val redirect = parseSnsAuthRedirect(deepLink) ?: run {
            viewModel.setErrorMessage(snsAuthInvalidRedirectMessage())
            SnsAuthDeepLinkState.clear()
            return@LaunchedEffect
        }

        when (val outcome = redirect.resolve()) {
            SnsAuthRedirectOutcome.MissingService -> {
                viewModel.setErrorMessage(snsAuthInvalidRedirectMessage())
                SnsAuthDeepLinkState.clear()
                return@LaunchedEffect
            }
            SnsAuthRedirectOutcome.MissingCode -> {
                viewModel.setErrorMessage(snsAuthMissingCodeMessage())
                SnsAuthDeepLinkState.clear()
                return@LaunchedEffect
            }
            is SnsAuthRedirectOutcome.Failure -> {
                viewModel.setErrorMessage(snsAuthFailedMessage(outcome.error, outcome.description))
                SnsAuthDeepLinkState.clear()
                return@LaunchedEffect
            }
            is SnsAuthRedirectOutcome.Success -> {
                viewModel.setErrorMessage(null)
                runCatching {
                    val account = authenticateSnsAccount(
                        service = outcome.service,
                        code = outcome.code,
                        state = outcome.state,
                        authenticator = DefaultSnsAccountAuthenticator,
                    )
                    val currentUser = runCatching {
                        repository.getUser(currentAuthUser.uid)
                    }.getOrElse {
                        UserEntity(id = currentAuthUser.uid)
                    }
                    val updatedUser = currentUser.copy(
                        accounts = currentUser.accounts.filterNot {
                            it.service.lowercase() == account.service.lowercase()
                        } + account,
                    )
                    repository.saveUser(updatedUser)
                    viewModel.setAppUser(
                        AppUser(
                            user = updatedUser,
                            cards = userState.appUser?.cards.orEmpty(),
                        ),
                    )
                }.onFailure { throwable ->
                    val errorMessage = if (
                        throwable is IllegalStateException &&
                        throwable.message?.startsWith("未対応のSNSサービスです") == true
                    ) {
                        snsAuthUnsupportedServiceMessage()
                    } else {
                        throwable.message ?: snsAuthReflectFailedMessage()
                    }
                    viewModel.setErrorMessage(errorMessage)
                }
                SnsAuthDeepLinkState.clear()
            }
        }
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
                    cardRepository = cardRepository,
                    isRefreshing = userState.isLoading,
                    onRefresh = {
                        effectiveUserViewModel?.reloadCurrentUser()
                    },
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
                    onPreviewPartnerCard = { card ->
                        navController.navigate(PartnerCardRoute(card.id))
                    },
                    onReorderCards = { fromIndex, toIndex ->
                        effectiveUserViewModel?.reorderCards(fromIndex, toIndex)
                    },
                    onSnsAuthClick = {
                        if (authUser != null) {
                            navController.navigate(SnsAuthRoute) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onChargeClick = {
                        navController.navigate(ChargeRoute) {
                            launchSingleTop = true
                        }
                    },
                    onLicenseClick = {
                        navController.navigate(LicenseRoute) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<LicenseRoute> {
                LicenseScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<SnsAuthRoute> {
                SnsAuthScreen(
                    userEntity = effectiveAppUser?.user ?: UserEntity(),
                    isRefreshing = userState.isLoading,
                    onRefresh = {
                        effectiveUserViewModel?.reloadCurrentUser()
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onUnlinkAccount = { account ->
                        val repository = userRepository
                        val currentAppUser = effectiveAppUser
                        if (repository != null && currentAppUser != null) {
                            runCatching {
                                val updatedUser = currentAppUser.user.copy(
                                    accounts = currentAppUser.user.accounts.filterNot {
                                        it.service.lowercase() == account.service.lowercase()
                                    },
                                )
                                repository.saveUser(updatedUser)
                                effectiveUserViewModel?.setAppUser(
                                    AppUser(
                                        user = updatedUser,
                                        cards = currentAppUser.cards,
                                    ),
                                )
                            }.onFailure { throwable ->
                                effectiveUserViewModel?.setErrorMessage(
                                    throwable.message ?: "SNS認証の解除に失敗しました。",
                                )
                            }
                        }
                    },
                )
            }
            composable<ChargeRoute> {
                ChargeScreen(
                    isPurchasing = userState.isPurchasing,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onPurchaseClick = {
                        onPurchaseCardClick?.invoke {
                            navController.popBackStack<HomeRoute>(inclusive = false)
                        }
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
                    authenticatedAccounts = effectiveAppUser?.user?.accounts.orEmpty(),
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
                    onCardScanned = { partnerCardId ->
                        val repository = cardRepository
                        val currentUser = authUser
                        if (repository != null && currentUser != null) {
                            runCatching {
                                exchangeCard(
                                    cardRepository = repository,
                                    currentUid = currentUser.uid,
                                    currentCardId = card.id,
                                    partnerCardId = partnerCardId,
                                )
                            }.onSuccess {
                                navController.popBackStack()
                                effectiveUserViewModel?.reloadCurrentUser()
                            }.onFailure { throwable ->
                                effectiveUserViewModel?.setErrorMessage(
                                    throwable.message ?: "名刺交換に失敗しました。",
                                )
                            }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<PartnerCardRoute> { backStackEntry ->
                val cardId = backStackEntry.toRoute<PartnerCardRoute>().cardId
                val repository = cardRepository
                val partnerCardState by produceState<PartnerCardScreenState>(
                    initialValue = PartnerCardScreenState.Loading,
                    cardId,
                    repository,
                ) {
                    value = repository?.let {
                        runCatching { it.getCard(cardId) }
                            .fold(
                                onSuccess = { card -> PartnerCardScreenState.Loaded(card) },
                                onFailure = { PartnerCardScreenState.Error },
                            )
                    } ?: PartnerCardScreenState.Error
                }
                when (val state = partnerCardState) {
                    PartnerCardScreenState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("読み込み中")
                        }
                    }
                    PartnerCardScreenState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("名刺を読み込めませんでした")
                        }
                    }
                    is PartnerCardScreenState.Loaded -> {
                        PartnerCardScreen(
                            cardEntity = state.cardEntity,
                            onBackClick = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Serializable
data object HomeRoute

@Serializable
data object SnsAuthRoute

@Serializable
data object ChargeRoute

@Serializable
data object LicenseRoute

@Serializable
data class CardEntryRoute(val cardIndex: Int)

@Serializable
data class CardLayoutRoute(val cardIndex: Int)

@Serializable
data class CardPrintRoute(val cardIndex: Int)

@Serializable
data class CardPreviewRoute(val cardIndex: Int)

@Serializable
data class CardExchangeRoute(val cardIndex: Int)

@Serializable
data class PartnerCardRoute(val cardId: String)

private sealed interface PartnerCardScreenState {
    data object Loading : PartnerCardScreenState
    data object Error : PartnerCardScreenState
    data class Loaded(val cardEntity: CardEntity) : PartnerCardScreenState
}
