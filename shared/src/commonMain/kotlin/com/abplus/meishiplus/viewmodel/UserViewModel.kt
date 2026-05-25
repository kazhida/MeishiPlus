package com.abplus.meishiplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abplus.meishiplus.auth.AuthUser
import com.abplus.meishiplus.data.entities.UserEntity
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
}
