package com.abplus.meishiplus.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.abplus.meishiplus.data.repositories.firestore.FireStoreCardRepository
import com.abplus.meishiplus.data.repositories.firestore.FireStoreUserRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AndroidAuthGate() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val userRepository = remember { FireStoreUserRepository() }
    val cardRepository = remember { FireStoreCardRepository() }
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(auth.currentUser?.toAuthUser()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser?.toAuthUser()
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    currentUser?.let { user ->
        App(
            authUser = user,
            onSignOut = {
                coroutineScope.launch {
                    auth.signOut()
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    currentUser = null
                }
            },
            userRepository = userRepository,
            cardRepository = cardRepository,
        )
        return
    }

    SignInScreen(
        isLoading = isLoading,
        errorMessage = errorMessage,
        onSignIn = {
            coroutineScope.launch {
                isLoading = true
                errorMessage = null
                runCatching {
                    signInWithGoogle(
                        context = context,
                        auth = auth,
                        credentialManager = credentialManager,
                    )
                }.onSuccess { firebaseUser ->
                    currentUser = firebaseUser.toAuthUser()
                }.onFailure { throwable ->
                    errorMessage = throwable.userMessage()
                }
                isLoading = false
            }
        },
    )
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
