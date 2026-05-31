package com.abplus.meishiplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.auth.FacebookAuth
import com.abplus.meishiplus.auth.GithubAuth
import com.abplus.meishiplus.auth.InstagramAuth
import com.abplus.meishiplus.auth.QiitaAuth
import com.abplus.meishiplus.auth.XAuth
import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.entities.UserEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.model.AppUser
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
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
    val errorMessage: String? = null,
)

class UserViewModel(
    private val userRepository: UserRepository,
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
                errorMessage = null,
            )
        }
        loadAppUser(authUser.uid)
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
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                loadAppUser(authUser.uid)
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
        val uid = _uiState.value.authUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                cardRepository.updateCard(card)
                initializeUser(uid)
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

    fun authenticateQiitaAndSaveAccount(
        code: String,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "Qiita認証に失敗しました。"
        },
    ) {
        val appUser = _uiState.value.appUser ?: return
        if (code.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Qiitaの認証コードを入力してください。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val qiitaAccount = QiitaAuth.authenticate(code = code)
                val updatedUser = appUser.user.copy(
                    accounts = appUser.user.accounts.upsertAccount(qiitaAccount),
                )
                userRepository.updateUser(updatedUser)
                appUser.copy(user = updatedUser)
            }.onSuccess { updatedAppUser ->
                _uiState.update {
                    it.copy(
                        appUser = updatedAppUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    fun authenticateXAndSaveAccount(
        code: String,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "X認証に失敗しました。"
        },
    ) {
        val appUser = _uiState.value.appUser ?: return
        if (code.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Xの認証コードを取得できませんでした。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val xAccount = XAuth.authenticate(code = code)
                val updatedUser = appUser.user.copy(
                    accounts = appUser.user.accounts.upsertAccount(xAccount),
                )
                userRepository.updateUser(updatedUser)
                appUser.copy(user = updatedUser)
            }.onSuccess { updatedAppUser ->
                _uiState.update {
                    it.copy(
                        appUser = updatedAppUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    fun authenticateGithubAndSaveAccount(
        code: String,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "GitHub認証に失敗しました。"
        },
    ) {
        val appUser = _uiState.value.appUser ?: return
        if (code.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "GitHubの認証コードを取得できませんでした。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val githubAccount = GithubAuth.authenticate(code = code)
                val updatedUser = appUser.user.copy(
                    accounts = appUser.user.accounts.upsertAccount(githubAccount),
                )
                userRepository.updateUser(updatedUser)
                appUser.copy(user = updatedUser)
            }.onSuccess { updatedAppUser ->
                _uiState.update {
                    it.copy(
                        appUser = updatedAppUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    fun authenticateFacebookAndSaveAccount(
        code: String,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "Facebook認証に失敗しました。"
        },
    ) {
        val appUser = _uiState.value.appUser ?: return
        if (code.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Facebookの認証コードを取得できませんでした。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val facebookAccount = FacebookAuth.authenticate(code = code)
                val updatedUser = appUser.user.copy(
                    accounts = appUser.user.accounts.upsertAccount(facebookAccount),
                )
                userRepository.updateUser(updatedUser)
                appUser.copy(user = updatedUser)
            }.onSuccess { updatedAppUser ->
                _uiState.update {
                    it.copy(
                        appUser = updatedAppUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    fun authenticateInstagramAndSaveAccount(
        code: String,
        toErrorMessage: (Throwable) -> String = { throwable ->
            throwable.message ?: "Instagram認証に失敗しました。"
        },
    ) {
        val appUser = _uiState.value.appUser ?: return
        if (code.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Instagramの認証コードを取得できませんでした。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val instagramAccount = InstagramAuth.authenticate(code = code)
                val updatedUser = appUser.user.copy(
                    accounts = appUser.user.accounts.upsertAccount(instagramAccount),
                )
                userRepository.updateUser(updatedUser)
                appUser.copy(user = updatedUser)
            }.onSuccess { updatedAppUser ->
                _uiState.update {
                    it.copy(
                        appUser = updatedAppUser,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = toErrorMessage(throwable),
                    )
                }
            }
        }
    }

    private fun loadAppUser(uid: String) {
        viewModelScope.launch {
            runCatching {
                initializeUser(uid)
            }.onSuccess { appUser ->
                _uiState.update {
                    it.copy(
                        appUser = appUser,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        appUser = null,
                        errorMessage = throwable.message ?: "ユーザー情報を取得できませんでした。",
                    )
                }
            }
        }
    }

    private suspend fun initializeUser(uid: String): AppUser {
        val userEntity = runCatching {
            userRepository.getUser(uid)
        }.getOrElse {
            userRepository.addUser(UserEntity(id = uid))
        }

        val cards = if (userEntity.cardIds.isEmpty()) {
            emptyList()
        } else {
            cardRepository.getCards(userEntity.cardIds)
        }
        return AppUser(user = userEntity, cards = cards)
    }

    private fun List<Account>.upsertAccount(account: Account): List<Account> {
        val accountService = account.service.lowercase()
        return filterNot { it.service.lowercase() == accountService } + account
    }
}
