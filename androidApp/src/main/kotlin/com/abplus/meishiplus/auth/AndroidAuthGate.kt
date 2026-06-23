package com.abplus.meishiplus.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.abplus.meishiplus.App
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.usecase.UserInit
import com.abplus.meishiplus.data.repositories.CardRepository
import com.abplus.meishiplus.data.repositories.UserRepository
import com.abplus.meishiplus.purchase.AndroidCardPurchaseManager
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object FacebookLoginCallbackManager {
    val callbackManager: CallbackManager = CallbackManager.Factory.create()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        callbackManager.onActivityResult(requestCode, resultCode, data)
}

@Composable
fun AndroidAuthGate(
    userViewModel: UserViewModel,
    userRepository: UserRepository,
    cardRepository: CardRepository,
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val purchaseManager = remember(context) { AndroidCardPurchaseManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val uiState by userViewModel.uiState.collectAsState()

    DisposableEffect(auth, userViewModel) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userViewModel.setAuthUser(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    DisposableEffect(context, auth) {
        AndroidFacebookAuthBridge.setLoginHandler {
            authenticateFacebookWithFirebase(
                context = context,
                auth = auth,
            )
        }
        onDispose {
            AndroidFacebookAuthBridge.setLoginHandler(null)
        }
    }

    if (!uiState.isAuthResolved) {
        AuthLoadingScreen()
        return
    }

    if (uiState.authUser != null && uiState.appUser == null && uiState.isLoading) {
        AuthLoadingScreen()
        return
    }

    uiState.authUser?.let { user ->
        App(
            authUser = user,
            onSignOut = { shouldDeleteData ->
                userViewModel.signOut(
                    shouldDeleteData = shouldDeleteData,
                    signOut = {
                        auth.signOut()
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    },
                )
            },
            appUser = uiState.appUser,
            errorMessage = uiState.errorMessage,
            userViewModel = userViewModel,
            userRepository = userRepository,
            cardRepository = cardRepository,
            onPurchaseCardClick = { onSuccess ->
                coroutineScope.launch {
                    userViewModel.setPurchasing(true)
                    runCatching {
                        val activity = context as? Activity
                            ?: error("購入にはActivityコンテキストが必要です。")
                        when (val purchaseResult = purchaseManager.purchase(activity)) {
                            AndroidCardPurchaseManager.PurchaseResult.Success -> {
                                val currentUser = uiState.authUser
                                    ?: error("購入対象のユーザーが見つかりません。")
                                val appUser = withContext(Dispatchers.Default) {
                                    UserInit(userRepository, cardRepository).purchaseAdditionalCard(currentUser)
                                }
                                userViewModel.setAppUser(appUser)
                                onSuccess()
                            }
                            is AndroidCardPurchaseManager.PurchaseResult.Failure -> {
                                userViewModel.setErrorMessage(
                                    purchaseResult.message,
                                )
                            }
                        }
                    }.onFailure { throwable ->
                        userViewModel.setErrorMessage(
                            throwable.message ?: "アプリ内課金に失敗しました。",
                        )
                    }.also {
                        userViewModel.setPurchasing(false)
                    }
                }
            },
        )
        return
    }

    SignInScreen(
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onSignIn = {
            userViewModel.signIn(
                signInWithGoogle = {
                    signInWithGoogle(
                        context = context,
                        auth = auth,
                        credentialManager = credentialManager,
                    ).toAuthUser()
                },
                toErrorMessage = Throwable::userMessage,
            )
        },
    )
}

@Composable
private fun AuthLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SignInScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "名刺＋",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Googleアカウントでログイン",
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onSignIn,
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Googleでログイン")
                }
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private suspend fun signInWithGoogle(
    context: Context,
    auth: FirebaseAuth,
    credentialManager: CredentialManager,
): FirebaseUser {
    val activity = context as? Activity ?: error("ログインにはActivityコンテキストが必要です。")
    val googlePlayServicesResult = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context)
    if (googlePlayServicesResult != ConnectionResult.SUCCESS) {
        error(
            "Google Play開発者サービスが利用できません。Play開発者サービスを更新するか、Google Play対応端末で再度お試しください。",
        )
    }
    val webClientId = context.defaultWebClientId()

    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
        serverClientId = webClientId,
    )
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    val result = try {
        credentialManager.getCredential(
            context = activity,
            request = request,
        )
    } catch (securityException: SecurityException) {
        throw IllegalStateException(
            "Googleログインの認証ブローカーにアクセスできませんでした。Google Play開発者サービスを更新し、端末のGoogleアカウント設定を確認してください。",
            securityException,
        )
    }
    val credential = result.credential
    if (
        credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        error("Google IDトークンを取得できませんでした。")
    }

    val googleIdTokenCredential = try {
        GoogleIdTokenCredential.createFrom(credential.data)
    } catch (exception: GoogleIdTokenParsingException) {
        throw IllegalStateException("Google IDトークンの解析に失敗しました。", exception)
    }
    val firebaseCredential = GoogleAuthProvider.getCredential(
        googleIdTokenCredential.idToken,
        null,
    )
    return auth.signInWithCredential(firebaseCredential).await().user
        ?: error("Firebase Authのユーザー情報を取得できませんでした。")
}

private suspend fun authenticateFacebookWithFirebase(
    context: Context,
    auth: FirebaseAuth,
): Account.Facebook {
    val activity = context as? Activity ?: error("Facebook認証にはActivityコンテキストが必要です。")
    require(context.facebookClientToken().isNotBlank()) {
        "Facebook Client TokenをandroidApp/src/main/res/values/strings.xmlに設定してください。"
    }
    val currentUser = auth.currentUser ?: error("Facebook認証を連携するFirebaseユーザーが見つかりません。")
    currentUser.facebookProviderUserId()?.let { facebookUserId ->
        return facebookUserId.toFacebookAccount()
    }

    val accessToken = loginWithFacebookSdk(activity)
    val credential = FacebookAuthProvider.getCredential(accessToken.token)

    try {
        currentUser.linkWithCredential(credential).await().user
            ?: error("Facebook認証後のFirebaseユーザー情報を取得できませんでした。")
    } catch (exception: FirebaseAuthUserCollisionException) {
        throw IllegalStateException("このFacebookアカウントは別のユーザーに連携済みです。", exception)
    } catch (exception: FirebaseAuthException) {
        throw IllegalStateException(
            exception.localizedMessage ?: "Facebook認証に失敗しました。",
            exception,
        )
    }

    return accessToken.userId.toFacebookAccount()
}

private suspend fun loginWithFacebookSdk(activity: Activity): AccessToken =
    suspendCancellableCoroutine { continuation ->
        val loginManager = LoginManager.getInstance()
        val callbackManager = FacebookLoginCallbackManager.callbackManager
        loginManager.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    loginManager.unregisterCallback(callbackManager)
                    if (continuation.isActive) {
                        continuation.resume(result.accessToken)
                    }
                }

                override fun onCancel() {
                    loginManager.unregisterCallback(callbackManager)
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("Facebook認証がキャンセルされました。"),
                        )
                    }
                }

                override fun onError(error: FacebookException) {
                    loginManager.unregisterCallback(callbackManager)
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException(
                                error.localizedMessage ?: "Facebook認証に失敗しました。",
                                error,
                            ),
                        )
                    }
                }
            },
        )
        continuation.invokeOnCancellation {
            loginManager.unregisterCallback(callbackManager)
        }
        loginManager.logInWithReadPermissions(activity, listOf("public_profile"))
    }

private fun FirebaseUser.facebookProviderUserId(): String? =
    providerData.firstOrNull { it.providerId == FacebookAuthProvider.PROVIDER_ID }?.uid

private fun String.toFacebookAccount(): Account.Facebook =
    Account.Facebook(
        service = "facebook",
        userName = this,
        userUrl = "https://www.facebook.com/$this",
    )

private fun Context.defaultWebClientId(): String {
    val resourceId = resources.getIdentifier(
        "default_web_client_id",
        "string",
        packageName,
    )
    if (resourceId == 0) {
        error("google-services.jsonを配置し、Googleログインを有効化してください。")
    }
    return getString(resourceId)
}

private fun Context.facebookClientToken(): String {
    val resourceId = resources.getIdentifier(
        "facebook_client_token",
        "string",
        packageName,
    )
    return if (resourceId == 0) {
        ""
    } else {
        getString(resourceId).trim()
    }
}

private fun FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
    )

private fun Throwable.userMessage(): String =
    when (this) {
        is GetCredentialCancellationException -> "Googleログインがキャンセルされました。"
        is NoCredentialException -> "端末に利用可能なGoogleアカウントが見つかりませんでした。Google Play開発者サービスと端末のGoogleアカウントを確認してください。"
        is GetCredentialProviderConfigurationException -> "Credential Managerのプロバイダ設定に問題があります。Google Play開発者サービスを更新してください。"
        is GetCredentialUnsupportedException -> "この端末はCredential ManagerのGoogleログインに対応していません。"
        is GetCredentialException -> {
            Log.e("AndroidAuthGate", "Google credential request failed", this)
            localizedMessage ?: "Googleアカウントの選択が完了しませんでした。"
        }
        is SecurityException -> {
            localizedMessage ?: "Google Play開発者サービスの認証ブローカーに接続できませんでした。"
        }
        else -> localizedMessage ?: "Googleログインに失敗しました。"
}
