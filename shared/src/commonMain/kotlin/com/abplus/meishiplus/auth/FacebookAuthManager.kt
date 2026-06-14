package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account

expect class FacebookAuthManager() {
    suspend fun login(): Account.Facebook
}
