package com.abplus.meishiplus.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.abplus.meishiplus.viewmodel.UserViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

@Composable
fun AndroidAuthGate(
    userViewModel: UserViewModel,
    deepLinkUri: StateFlow<Uri?>? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val uiState by userViewModel.uiState.collectAsState()
    val currentDeepLinkUri by (deepLinkUri ?: remember { MutableStateFlow<Uri?>(null) }).collectAsState()

    DisposableEffect(auth, userViewModel) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userViewModel.setAuthUser(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentDeepLinkUri, uiState.appUser) {
        if (uiState.appUser == null) return@LaunchedEffect

        currentDeepLinkUri
            ?.takeIf { it.scheme == "mspls" }
            ?.let { uri ->
                val code = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() } ?: return@let
                when (uri.host) {
                    "facebook" -> userViewModel.authenticateFacebookAndSaveAccount(code)
                    "github" -> userViewModel.authenticateGithubAndSaveAccount(code)
                    "instagram" -> userViewModel.authenticateInstagramAndSaveAccount(code)
                    "qiita" -> userViewModel.authenticateQiitaAndSaveAccount(code)
                    "qiiita" -> userViewModel.authenticateQiitaAndSaveAccount(code)
                    "x" -> userViewModel.authenticateXAndSaveAccount(code)
                    else -> return@let
                }
                onDeepLinkConsumed()
            }
    }

    if (!uiState.isAuthResolved) {
        AuthLoadingScreen()
        return
    }

    uiState.authUser?.let { user ->
        App(
            authUser = user,
            onSignOut = {
                userViewModel.signOut(signOut = {
                    auth.signOut()
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                })
            },
            appUser = uiState.appUser,
            errorMessage = uiState.errorMessage,
            userViewModel = userViewModel,
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
    val webClientId = context.defaultWebClientId()

    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
        serverClientId = webClientId,
    )
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    val result = credentialManager.getCredential(
        context = activity,
        request = request,
    )
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
        else -> localizedMessage ?: "Googleログインに失敗しました。"
    }
