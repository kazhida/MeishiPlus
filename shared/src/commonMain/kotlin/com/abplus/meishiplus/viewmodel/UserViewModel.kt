package com.abplus.meishiplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.usecase.UserInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val authUser: AuthUser? = null,
    val appUser: AppUser? = null,
    val isAuthResolved: Boolean = false,
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
)

class UserViewModel(
    private val userInit: UserInit,
    private val cardRepository: CardRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun setAuthUser(authUser: AuthUser?) {
        val currentUser = _uiState.value.authUser
        if (_uiState.value.isAuthResolved && currentUser?.uid == authUser?.uid) return

        if (authUser == null) {
            _uiState.value = UserUiState(isAuthResolved = true)
            return
        }

        _uiState.update {
            it.copy(
                authUser = authUser,
                appUser = null,
                isAuthResolved = true,
                isLoading = true,
                errorMessage = null,
            )
        }
        loadAppUser(authUser)
    }

    fun signIn(
        signInWithGoogle: suspend () -> AuthUser,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "Googleログインに失敗しました。"
        },
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                signInWithGoogle()
            }.onSuccess { authUser ->
                _uiState.update {
                    it.copy(
                        authUser = authUser,
                        isAuthResolved = true,
                        isLoading = true,
                        errorMessage = null,
                    )
                }
                loadAppUser(authUser)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthResolved = true,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    fun signOut(
        signOut: suspend () -> Unit,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "ログアウトに失敗しました。"
        },
    ) {
        viewModelScope.launch {
            runCatching {
                signOut()
            }.onSuccess {
                _uiState.value = UserUiState(isAuthResolved = true)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = toErrorMessage(throwable))
                }
            }
        }
    }

    fun updateCard(cardIndex: Int, card: CardEntity) {
        _uiState.update { state ->
            val appUser = state.appUser ?: return@update state
            if (cardIndex !in appUser.cards.indices) return@update state

            val updatedCards = appUser.cards.toMutableList().apply {
                this[cardIndex] = card
            }
            state.copy(appUser = appUser.copy(cards = updatedCards))
        }
    }

    fun updateCardAndReloadUser(card: CardEntity) {
        val authUser = _uiState.value.authUser ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                cardRepository.updateCard(card)
                userInit(authUser)
            }.onSuccess { appUser ->
                _uiState.update {
                    it.copy(
                        appUser = appUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "名刺情報を更新できませんでした。",
                    )
                }
            }
        }
    }

    fun reorderCards(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val currentState = _uiState.value
        val appUser = currentState.appUser ?: return
        if (fromIndex !in appUser.cards.indices || toIndex !in appUser.cards.indices) return

        val updatedCards = appUser.cards.moved(fromIndex, toIndex)
        val updatedCardIds = updatedCards.map { it.id }
        val updatedUser = appUser.user.copy(cardIds = updatedCardIds)
        val updatedAppUser = appUser.copy(
            user = updatedUser,
            cards = updatedCards,
        )

        _uiState.update {
            it.copy(
                appUser = updatedAppUser,
                errorMessage = null,
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                userInit.saveCardOrder(updatedUser, updatedCardIds)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(errorMessage = throwable.message ?: "名刺の並び順を保存できませんでした。")
                }
            }
        }
    }

    fun purchaseAdditionalCard() {
        val authUser = _uiState.value.authUser ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isPurchasing = true,
                    errorMessage = null,
                )
            }

            runCatching {
                userInit.purchaseAdditionalCard(authUser)
            }.onSuccess { appUser ->
                _uiState.update {
                    it.copy(
                        appUser = appUser,
                        isPurchasing = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = throwable.message ?: "カードを追加できませんでした。",
                    )
                }
            }
        }
    }

    fun setPurchasing(isPurchasing: Boolean) {
        _uiState.update {
            it.copy(isPurchasing = isPurchasing)
        }
    }

    fun reloadCurrentUser() {
        val authUser = _uiState.value.authUser ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                userInit(authUser)
            }.onSuccess { appUser ->
                _uiState.update {
                    it.copy(
                        appUser = appUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "ユーザー情報を再取得できませんでした。",
                    )
                }
            }
        }
    }

    fun setAppUser(appUser: AppUser) {
        _uiState.update {
            it.copy(
                appUser = appUser,
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { state ->
            state.copy(errorMessage = message)
        }
    }

    private fun loadAppUser(authUser: AuthUser) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                userInit(authUser)
            }.onSuccess { appUser ->
                _uiState.update {
                    it.copy(
                        appUser = appUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        appUser = null,
                        isLoading = false,
                        errorMessage = throwable.message ?: "ユーザー情報を取得できませんでした。",
                    )
                }
            }
        }
    }
}

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
