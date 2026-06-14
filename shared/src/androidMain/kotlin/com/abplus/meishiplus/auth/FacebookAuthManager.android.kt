package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account

actual class FacebookAuthManager actual constructor() {
    actual suspend fun login(): Account.Facebook =
        AndroidFacebookAuthBridge.login()
}

object AndroidFacebookAuthBridge {
    private var loginHandler: (suspend () -> Account.Facebook)? = null

    fun setLoginHandler(handler: (suspend () -> Account.Facebook)?) {
        loginHandler = handler
    }

    suspend fun login(): Account.Facebook =
        loginHandler?.invoke()
            ?: error("AndroidのFacebook認証ハンドラが設定されていません。")
}
