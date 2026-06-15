package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class FacebookAuthManager actual constructor() {
    actual suspend fun login(): Account.Facebook =
        IOSFacebookAuthBridge.login()
}

object IOSFacebookAuthBridge {
    private var loginHandler: (((Account.Facebook) -> Unit, (String) -> Unit) -> Unit)? = null

    fun setLoginHandler(handler: (((Account.Facebook) -> Unit, (String) -> Unit) -> Unit)?) {
        loginHandler = handler
    }

    suspend fun login(): Account.Facebook {
        val handler = loginHandler
            ?: error("iOSのFacebook認証ハンドラが設定されていません。")
        return suspendCancellableCoroutine { continuation ->
            handler(
                { account ->
                    if (continuation.isActive) {
                        continuation.resume(account)
                    }
                },
                { message ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(message))
                    }
                },
            )
        }
    }
}
