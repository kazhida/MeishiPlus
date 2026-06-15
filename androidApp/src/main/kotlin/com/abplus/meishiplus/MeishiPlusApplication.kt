package com.abplus.meishiplus

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk

class MeishiPlusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val clientToken = facebookClientToken()
        if (clientToken.isBlank()) {
            Log.w(TAG, "Facebook Client Token is not configured. Facebook login will be unavailable.")
            return
        }
        FacebookSdk.setClientToken(clientToken)
        @Suppress("DEPRECATION")
        FacebookSdk.sdkInitialize(applicationContext)
    }

    private fun facebookClientToken(): String {
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

    private companion object {
        private const val TAG = "MeishiPlusApplication"
    }
}
